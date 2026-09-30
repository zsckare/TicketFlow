package com.ticketflow.orders.modules.orders

import com.ticketflow.orders.clients.events.EventsClient
import com.ticketflow.orders.clients.payments.CreateCheckoutRequest
import com.ticketflow.orders.clients.payments.PaymentStatus
import com.ticketflow.orders.clients.payments.PaymentsClient
import com.ticketflow.orders.clients.tickets.TicketInventoryStatus
import com.ticketflow.orders.clients.tickets.TicketsClient
import com.ticketflow.orders.outbox.OrderConfirmedItem
import java.time.OffsetDateTime
import java.util.UUID
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import java.util.Base64
import java.security.MessageDigest
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter

class OrderService(
    private val repository: OrderRepository,
    private val ticketsClient: TicketsClient,
    private val paymentsClient: PaymentsClient,
    private val eventsClient: EventsClient,
    private val qrSecret: String,
) {
    suspend fun create(userId: UUID, request: CreateOrderRequest): OrderResponse {
        val existingCart = findActive(userId)
        require(existingCart == null) { "You already have an active reservation. Complete or cancel your cart before starting another one." }

        val raw = (request.inventoryIds + listOfNotNull(request.inventoryId)).distinct()
        require(raw.isNotEmpty()) { "At least one inventoryId is required" }
        require(raw.size <= 10) { "An order can contain at most 10 tickets" }

        val inventory = raw.map { id ->
            val uuid = parseUuid(id, "Invalid inventoryId")
            ticketsClient.findInventoryById(uuid.toString()) ?: throw TicketNotAvailableException()
        }
        require(inventory.map { it.eventId }.distinct().size == 1) { "All tickets in an order must belong to the same event" }
        if (inventory.any { it.status != TicketInventoryStatus.AVAILABLE }) throw TicketNotAvailableException()

        val order = repository.createPending(userId, inventory)
        val orderId = UUID.fromString(order.id)
        val reserved = mutableListOf<Pair<UUID, UUID>>()
        try {
            inventory.forEach { item ->
                val inventoryId = UUID.fromString(item.id)
                val response = ticketsClient.reserve(item.id) ?: throw TicketNotAvailableException()
                val reservationId = response.inventory.reservationId?.let(UUID::fromString)
                    ?: error("Tickets Service returned no reservationId")
                val reservedUntil = response.inventory.reservedUntil?.let(OffsetDateTime::parse)
                repository.attachReservation(orderId, inventoryId, reservationId, reservedUntil)
                reserved += inventoryId to reservationId
            }
        } catch (cause: Exception) {
            reserved.forEach { (inventoryId, reservationId) -> compensateReservation(inventoryId, reservationId) }
            repository.markFailed(orderId, "Unable to reserve all tickets")
            throw cause
        }
        return enrich(repository.markReserved(orderId) ?: repository.markFailed(orderId, "Unable to persist reservation"))
    }

    suspend fun checkout(userId: UUID, orderId: UUID, userEmail: String, request: CheckoutRequest, isAdmin: Boolean = false): CheckoutResponse {
        val order = findById(orderId)
        ensureOwner(order, userId, isAdmin)
        if (order.status != OrderStatus.RESERVED) throw InvalidOrderStateException("Only RESERVED orders can be checked out")
        validateReservations(order)
        val payment = paymentsClient.createCheckout(
            CreateCheckoutRequest(
                order.id,
                userId.toString(),
                userEmail,
                order.amount,
                order.currency,
                "order-checkout-${order.id}",
                request.successUrl,
                request.cancelUrl,
            ),
        )

        // A synchronous provider (SIMULATED) or a very fast webhook may have
        // already attached the payment and confirmed the order before the
        // /checkout call returns. Re-read the order and make this step
        // idempotent instead of trying to attach the same payment twice.
        val orderAfterPayment = findById(orderId)
        when {
            orderAfterPayment.paymentId == null -> {
                repository.attachPayment(orderId, UUID.fromString(payment.id))
                    ?: throw OrderOperationException("Unable to attach payment to order")
            }
            orderAfterPayment.paymentId != payment.id -> {
                throw OrderOperationException("Order already belongs to another payment")
            }
        }

        val latestOrder = repository.findById(orderId) ?: orderAfterPayment
        
        return CheckoutResponse(
            order = enrich(latestOrder),
            paymentId = payment.id,
            paymentStatus = payment.status.name,
            checkoutUrl = payment.checkoutUrl,
        )
    }

    /** Called only by Payments Service after a verified provider webhook. Idempotent. */
    suspend fun paymentSucceeded(orderId: UUID, paymentId: UUID, userEmail: String): OrderResponse {
        val order = findById(orderId)
        if (order.status == OrderStatus.CONFIRMED) return enrich(order)
        if (order.status != OrderStatus.RESERVED) throw InvalidOrderStateException("Order is no longer reservable")
        validateReservations(order)
        repository.attachPayment(orderId, paymentId)
        order.items.forEach { item ->
            val reservationId = item.reservationId ?: throw OrderOperationException("Order item has no reservationId")
            val inventory = ticketsClient.confirm(item.inventoryId, reservationId) ?: throw OrderOperationException("Ticket reservation could not be confirmed")
            if (inventory.status != TicketInventoryStatus.SOLD) throw OrderOperationException("Tickets Service did not mark inventory as SOLD")
        }
        // Capture display data while the order is being confirmed so the
        // asynchronous notification does not need to call other services.
        val enrichedOrder = enrich(order)
        val confirmedItems = enrichedOrder.items.map { item ->
            OrderConfirmedItem(
                inventoryId = item.inventoryId,
                eventId = item.eventId,
                eventName = item.eventName,
                eventStartsAt = item.eventStartsAt,
                venueName = item.venueName,
                sectionName = item.sectionName,
                sectionType = item.sectionType,
                seatLabel = item.seatLabel,
                unitPrice = item.unitPrice,
                currency = item.currency,
            )
        }
        val confirmed = repository.markConfirmed(orderId, userEmail, confirmedItems)
            ?: throw OrderOperationException("Unable to mark order as CONFIRMED")
        repository.issueTickets(orderId, UUID.fromString(requireNotNull(confirmed.userId)))
        return enrich(confirmed)
    }

    private suspend fun validateReservations(order: OrderResponse) {
        order.items.forEach { item ->
            val current = ticketsClient.findInventoryById(item.inventoryId) ?: throw OrderOperationException("Reserved inventory no longer exists")
            if (current.status != TicketInventoryStatus.RESERVED || current.reservationId != item.reservationId) {
                repository.markCancelled(UUID.fromString(order.id))
                throw InvalidOrderStateException("Reservation expired. Please select your tickets again")
            }
        }
    }

    suspend fun cancel(userId: UUID, orderId: UUID, isAdmin: Boolean = false): OrderResponse {
        val order = findById(orderId)
        ensureOwner(order, userId, isAdmin)
        if (order.status == OrderStatus.CANCELLED) return enrich(order)

        if (order.status == OrderStatus.CONFIRMED) {
            val paymentId = order.paymentId ?: throw OrderOperationException("Confirmed order has no paymentId")
            val refund = paymentsClient.refund(paymentId)
            if (refund.status != PaymentStatus.REFUNDED) throw OrderOperationException("Payment could not be refunded")
            order.items.forEach { item ->
                ticketsClient.restock(item.inventoryId)
                    ?: throw OrderOperationException("Refund succeeded but inventory could not be restocked")
            }
            repository.cancelIssuedTickets(orderId)
            return enrich(repository.markCancelled(orderId) ?: throw OrderOperationException("Unable to cancel order"))
        }

        if (order.status != OrderStatus.RESERVED) throw InvalidOrderStateException("Only RESERVED or CONFIRMED orders can be cancelled")
        order.items.forEach { item ->
            val reservationId = item.reservationId ?: return@forEach
            // Expired reservations may already have been released by Tickets Service.
            val current = ticketsClient.findInventoryById(item.inventoryId)
            if (current?.status == TicketInventoryStatus.RESERVED && current.reservationId == reservationId) {
                ticketsClient.release(item.inventoryId, reservationId)
                    ?: throw OrderOperationException("Ticket reservation could not be released")
            }
        }
        return enrich(repository.markCancelled(orderId) ?: throw OrderOperationException("Unable to cancel order"))
    }

    fun findById(id: UUID): OrderResponse = repository.findById(id) ?: throw OrderNotFoundException(id.toString())

    suspend fun findForUser(userId: UUID, isAdmin: Boolean): List<OrderResponse> {
        val orders = if (isAdmin) repository.findAll() else repository.findByUser(userId)
        return orders.map { reconcileAndEnrich(it) }
    }

    suspend fun findForUser(orderId: UUID, userId: UUID, isAdmin: Boolean): OrderResponse {
        val order = findById(orderId)
        ensureOwner(order, userId, isAdmin)
        return reconcileAndEnrich(order)
    }

    suspend fun findActive(userId: UUID): OrderResponse? {
        val candidates = repository.findByUser(userId)
            .filter { it.status == OrderStatus.RESERVED }
        return candidates.firstNotNullOfOrNull { order ->
            val reconciled = reconcileExpired(order)
            if (reconciled.status == OrderStatus.RESERVED) enrich(reconciled) else null
        }
    }

    suspend fun findActiveForEvent(userId: UUID, eventId: UUID): OrderResponse? {
        val candidates = repository.findByUser(userId)
            .filter { it.status == OrderStatus.RESERVED && it.items.any { item -> item.eventId == eventId.toString() } }
        return candidates.firstNotNullOfOrNull { order ->
            val reconciled = reconcileExpired(order)
            if (reconciled.status == OrderStatus.RESERVED) enrich(reconciled) else null
        }
    }

    suspend fun findTickets(userId: UUID): List<IssuedTicketResponse> =
        repository.findTicketsByUser(userId).map { enrich(it) }

    fun checkInStats(eventId: UUID, isOperator: Boolean): CheckInStatsResponse {
        if (!isOperator) throw SecurityException("STAFF or ADMIN role required")
        return repository.ticketStats(eventId)
    }

    suspend fun checkIn(payload: String, expectedEventId: UUID?, isOperator: Boolean): IssuedTicketResponse {
        if (!isOperator) throw SecurityException("STAFF or ADMIN role required")

        val token = verifyQrPayload(payload)
            ?: throw OrderOperationException("Invalid or tampered ticket QR")

        // Repository.checkIn performs an atomic ISSUED -> USED compare-and-set.
        // Two scanners racing with the same QR cannot both admit the attendee.
        val ticketBefore = repository.findTicketByAdmissionToken(token)
            ?: throw OrderOperationException("Ticket not found")
        if (expectedEventId != null && ticketBefore.eventId != expectedEventId.toString()) {
            throw OrderOperationException("Ticket belongs to a different event")
        }
        val ticket = repository.checkIn(token)
            ?: throw OrderOperationException("Ticket not found")

        if (ticket.status != IssuedTicketStatus.USED) {
            throw OrderOperationException("Ticket is not valid for check-in")
        }

        return enrich(ticket)
    }

    private fun signedQr(ticket: IssuedTicketResponse): String {
        val unsignedPayload = listOf(
            "ticketflow",
            "v1",
            ticket.id,
            ticket.admissionToken,
            ticket.eventId,
        ).joinToString(":")

        return "$unsignedPayload:${hmac(unsignedPayload)}"
    }

    private fun qrDataUrl(payload: String): String {
        val matrix = QRCodeWriter().encode(payload, BarcodeFormat.QR_CODE, 240, 240)
        val path = buildString {
            for (y in 0 until matrix.height) {
                for (x in 0 until matrix.width) {
                    if (matrix[x, y]) append("M$x $y h1 v1 h-1z ")
                }
            }
        }
        val svg = "<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 ${matrix.width} ${matrix.height}' shape-rendering='crispEdges'><rect width='100%' height='100%' fill='white'/><path d='$path' fill='black'/></svg>"
        return "data:image/svg+xml;base64," + Base64.getEncoder().encodeToString(svg.toByteArray())
    }

    private fun verifyQrPayload(value: String): UUID? {
        val parts = value.trim().split(":")
        if (parts.size != 6 || parts[0] != "ticketflow" || parts[1] != "v1") return null

        val unsignedPayload = parts.take(5).joinToString(":")
        val expectedSignature = hmac(unsignedPayload)
        val providedSignature = parts[5]

        if (!MessageDigest.isEqual(expectedSignature.toByteArray(), providedSignature.toByteArray())) return null

        return runCatching { UUID.fromString(parts[3]) }.getOrNull()
    }

    private fun hmac(value: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(qrSecret.toByteArray(), "HmacSHA256"))
        return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(value.toByteArray()))
    }

    private suspend fun reconcileAndEnrich(order: OrderResponse): OrderResponse = enrich(reconcileExpired(order))

    private fun reconcileExpired(order: OrderResponse): OrderResponse {
        if (order.status != OrderStatus.RESERVED) return order
        val deadline = order.reservedUntil?.let { runCatching { OffsetDateTime.parse(it) }.getOrNull() } ?: return order
        if (deadline.isAfter(OffsetDateTime.now())) return order
        return repository.markCancelled(UUID.fromString(order.id)) ?: order.copy(status = OrderStatus.CANCELLED)
    }

    private suspend fun enrich(order: OrderResponse): OrderResponse {
        val eventCache = mutableMapOf<String, com.ticketflow.orders.clients.events.EventResponse?>()
        val venueCache = mutableMapOf<String, com.ticketflow.orders.clients.events.VenueResponse?>()
        val sectionCache = mutableMapOf<String, com.ticketflow.orders.clients.events.VenueSectionResponse?>()
        val seatCache = mutableMapOf<String, com.ticketflow.orders.clients.events.SeatResponse?>()
        val items = order.items.map { item ->
            val event = eventCache.getOrPut(item.eventId) { null } ?: eventsClient.findEvent(item.eventId).also { eventCache[item.eventId] = it }
            val venue = event?.venueId?.let { id -> venueCache[id] ?: eventsClient.findVenue(id).also { venueCache[id] = it } }
            val section = item.sectionId?.let { id -> sectionCache[id] ?: eventsClient.findSection(id).also { sectionCache[id] = it } }
            val seat = item.seatId?.let { id -> seatCache[id] ?: eventsClient.findSeat(id).also { seatCache[id] = it } }
            item.copy(
                eventName = event?.name,
                eventStartsAt = event?.startsAt,
                venueName = venue?.name,
                sectionName = section?.name,
                sectionType = section?.type,
                seatLabel = seat?.let { "${it.row}${it.number}" },
            )
        }
        return order.copy(items = items)
    }

    private suspend fun enrich(ticket: IssuedTicketResponse): IssuedTicketResponse {
        val event = eventsClient.findEvent(ticket.eventId)
        val venue = event?.let { eventsClient.findVenue(it.venueId) }
        val section = ticket.sectionId?.let { eventsClient.findSection(it) }
        val seat = ticket.seatId?.let { eventsClient.findSeat(it) }
        val payload = signedQr(ticket)
        return ticket.copy(
            qrPayload = payload,
            qrDataUrl = qrDataUrl(payload),
            eventName = event?.name,
            eventStartsAt = event?.startsAt,
            venueName = venue?.name,
            sectionName = section?.name,
            sectionType = section?.type,
            seatLabel = seat?.let { "${it.row}${it.number}" },
        )
    }

    private fun ensureOwner(order: OrderResponse, userId: UUID, isAdmin: Boolean) {
        if (!isAdmin && order.userId != userId.toString()) throw SecurityException("Order does not belong to authenticated user")
    }

    private suspend fun compensateReservation(inventoryId: UUID, reservationId: UUID) {
        try { ticketsClient.release(inventoryId.toString(), reservationId.toString()) } catch (_: Exception) { }
    }

    private fun parseUuid(value: String, message: String): UUID =
        try { UUID.fromString(value) } catch (_: IllegalArgumentException) { throw IllegalArgumentException(message) }
}
