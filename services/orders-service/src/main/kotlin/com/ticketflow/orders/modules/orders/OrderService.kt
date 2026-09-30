package com.ticketflow.orders.modules.orders

import com.ticketflow.orders.clients.events.EventsClient 
import com.ticketflow.orders.clients.payments.CreateCheckoutRequest
import com.ticketflow.orders.clients.payments.PaymentStatus
import com.ticketflow.orders.clients.payments.PaymentsClient
import com.ticketflow.orders.clients.tickets.TicketInventoryStatus
import com.ticketflow.orders.clients.tickets.TicketsClient
import java.time.OffsetDateTime
import java.util.UUID

class OrderService(
    private val repository: OrderRepository,
    private val ticketsClient: TicketsClient,
    private val paymentsClient: PaymentsClient,
    private val eventsClient: EventsClient,
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


    suspend fun checkout(
    userId: UUID,
    orderId: UUID,
    userEmail: String,
    isAdmin: Boolean = false,
): CheckoutResponse {
    val order = findById(orderId)

    ensureOwner(
        order = order,
        userId = userId,
        isAdmin = isAdmin,
    )

    /*
     * Solo una reservación activa puede iniciar checkout.
     *
     * Una orden CONFIRMED no necesita volver a pasar por
     * Payments Service.
     */
    if (order.status != OrderStatus.RESERVED) {
        throw InvalidOrderStateException(
            "Only RESERVED orders can proceed to checkout",
        )
    }

    /*
     * Antes de crear el pago verificamos que todas las
     * reservaciones sigan vigentes en Tickets Service.
     *
     * Esto evita cobrar una orden cuyo inventario ya fue
     * liberado por expiración.
     */
    order.items.forEach { item ->
        val current =
            ticketsClient.findInventoryById(
                item.inventoryId,
            )
                ?: throw OrderOperationException(
                    "Reserved inventory no longer exists",
                )

        if (
            current.status != TicketInventoryStatus.RESERVED ||
            current.reservationId != item.reservationId
        ) {
            repository.markCancelled(
                orderId,
            )

            throw InvalidOrderStateException(
                "Reservation expired. Please select your tickets again",
            )
        }
    }

    /*
     * Creamos o recuperamos el pago.
     *
     * Payments Service utiliza idempotencyKey, por lo que
     * repetir esta operación para la misma orden no debe
     * crear un segundo pago.
     */
    val payment =
        paymentsClient.createCheckout(
            CreateCheckoutRequest(
                orderId = order.id,
                userId = userId.toString(),
                userEmail = userEmail,
                amount = order.amount,
                currency = order.currency,
                idempotencyKey =
                    "order-checkout-${order.id}",
                successUrl =
                    "http://localhost:5174/checkout/success?orderId=${order.id}",
                cancelUrl =
                    "http://localhost:5174/cart",
            ),
        )

    /*
     * IMPORTANTE:
     *
     * En proveedores reales como Stripe, normalmente
     * createCheckout() regresa mientras el pago sigue
     * PENDING. En ese caso todavía debemos asociar el
     * paymentId a la orden.
     *
     * Sin embargo, SIMULATED completa el pago
     * inmediatamente:
     *
     * Orders
     *   -> Payments /checkout
     *   -> Payment SUCCEEDED
     *   -> Orders /payment-succeeded
     *   -> paymentSucceeded()
     *   -> attachPayment()
     *   -> CONFIRMED
     *   -> regresa a checkout()
     *
     * Por eso debemos volver a consultar la orden antes
     * de intentar asociar el pago.
     */
    val orderAfterPayment =
        findById(orderId)

    when {
        /*
         * Caso normal de un proveedor asíncrono:
         * todavía no llegó el webhook/callback.
         */
        orderAfterPayment.paymentId == null -> {
            repository.attachPayment(
                orderId,
                UUID.fromString(
                    payment.id,
                ),
            ) ?: throw OrderOperationException(
                "Unable to attach payment to order",
            )
        }

        /*
         * El callback ya asoció exactamente este pago.
         *
         * Es el comportamiento esperado con SIMULATED
         * y también puede ocurrir con un webhook real
         * extremadamente rápido.
         *
         * No hacemos nada: la operación ya está hecha.
         */
        orderAfterPayment.paymentId ==
            payment.id -> {
            // Payment already attached.
        }

        /*
         * La orden ya tiene OTRO paymentId.
         *
         * Esto sí representa una inconsistencia y no
         * debemos continuar silenciosamente.
         */
        else -> {
            throw OrderOperationException(
                "Order already belongs to another payment",
            )
        }
    }

    return CheckoutResponse(
        orderId = order.id,
        paymentId = payment.id,
        paymentStatus =
            payment.status.name,
        checkoutUrl =
            payment.checkoutUrl,
    )
}

/**
 * Confirma la orden después de que Payments Service haya
 * validado el pago.
 *
 * Este método NO debe ser llamado directamente por el navegador.
 * Es utilizado por el callback interno proveniente de Payments.
 */
suspend fun paymentSucceeded(
    orderId: UUID,
    paymentId: UUID,
    userEmail: String,
): OrderResponse {
    val order = findById(orderId)

    // Hace el callback idempotente.
    if (order.status == OrderStatus.CONFIRMED) {
        return enrich(order)
    }

    if (order.status != OrderStatus.RESERVED) {
        throw InvalidOrderStateException(
            "Only RESERVED orders can be confirmed",
        )
    }

    // El payment asociado al callback debe ser el mismo
    // que fue creado durante checkout.
    if (
        order.paymentId != null &&
        order.paymentId != paymentId.toString()
    ) {
        throw OrderOperationException(
            "Payment does not belong to this order",
        )
    }

    if (order.paymentId == null) {
        repository.attachPayment(
            orderId,
            paymentId,
        ) ?: throw OrderOperationException(
            "Unable to attach payment to order",
        )
    }

    // Convertimos cada reservación temporal en inventario vendido.
    order.items.forEach { item ->
        val reservationId =
            item.reservationId
                ?: throw OrderOperationException(
                    "Order item has no reservationId",
                )

        val inventory =
            ticketsClient.confirm(
                item.inventoryId,
                reservationId,
            )
                ?: throw OrderOperationException(
                    "Ticket reservation could not be confirmed",
                )

        if (
            inventory.status !=
            TicketInventoryStatus.SOLD
        ) {
            throw OrderOperationException(
                "Tickets Service did not mark inventory as SOLD",
            )
        }
    }

    val confirmed =
        repository.markConfirmed(
            orderId,
            userEmail,
        )
            ?: throw OrderOperationException(
                "Unable to mark order as CONFIRMED",
            )

    repository.issueTickets(
        orderId,
        UUID.fromString(
            confirmed.userId
                ?: throw OrderOperationException(
                    "Confirmed order has no userId",
                ),
        ),
    )

    return enrich(confirmed)
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

    fun checkIn(token: UUID, isAdmin: Boolean): IssuedTicketResponse {
        if (!isAdmin) throw SecurityException("ADMIN role required")
        val ticket = repository.checkIn(token) ?: throw OrderOperationException("Ticket not found")
        if (ticket.status != IssuedTicketStatus.USED) throw OrderOperationException("Ticket is not valid for check-in")
        return ticket
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
        return ticket.copy(
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
