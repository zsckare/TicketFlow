package com.ticketflow.orders.modules.orders

import com.ticketflow.orders.clients.tickets.TicketInventoryResponse
import com.ticketflow.orders.outbox.OrderConfirmedEvent
import com.ticketflow.orders.outbox.OrderConfirmedItem
import com.ticketflow.orders.outbox.OutboxEventsTable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

class OrderRepository {
    fun createPending(userId: UUID, inventory: List<TicketInventoryResponse>): OrderResponse = transaction {
        require(inventory.isNotEmpty())
        val currencies = inventory.map { it.currency.uppercase() }.distinct()
        require(currencies.size == 1) { "All order items must use the same currency" }
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        val id = UUID.randomUUID()
        val amount = inventory.sumOf { BigDecimal(it.price) }
        val first = inventory.first()
        OrdersTable.insert {
            it[OrdersTable.id] = id; it[OrdersTable.userId] = userId
            // Legacy columns remain populated for backwards DB compatibility.
            it[OrdersTable.inventoryId] = UUID.fromString(first.id)
            it[OrdersTable.amount] = amount; it[currency] = currencies.single()
            it[status] = OrderStatus.PENDING.name; it[createdAt] = now; it[updatedAt] = now
        }
        inventory.forEach { remote ->
            OrderItemsTable.insert {
                it[OrderItemsTable.id] = UUID.randomUUID(); it[orderId] = id
                it[inventoryId] = UUID.fromString(remote.id); it[eventId] = UUID.fromString(remote.eventId)
                it[sectionId] = remote.sectionId?.let(UUID::fromString); it[seatId] = remote.seatId?.let(UUID::fromString)
                it[unitPrice] = BigDecimal(remote.price); it[currency] = remote.currency.uppercase(); it[createdAt] = now
            }
        }
        findByIdInternal(id)!!
    }

    fun attachReservation(orderId: UUID, inventoryId: UUID, reservationId: UUID, reservedUntil: OffsetDateTime?) = transaction {
        OrderItemsTable.update({ (OrderItemsTable.orderId eq orderId) and (OrderItemsTable.inventoryId eq inventoryId) }) {
            it[OrderItemsTable.reservationId] = reservationId
            it[OrderItemsTable.reservedUntil] = reservedUntil
        }
    }

    fun markReserved(orderId: UUID): OrderResponse? = transaction {
        val updated = OrdersTable.update({ (OrdersTable.id eq orderId) and (OrdersTable.status eq OrderStatus.PENDING.name) }) {
            it[status] = OrderStatus.RESERVED.name; it[failureReason] = null; it[updatedAt] = OffsetDateTime.now(ZoneOffset.UTC)
            val firstReservation = OrderItemsTable.selectAll().where { OrderItemsTable.orderId eq orderId }.firstOrNull()?.get(OrderItemsTable.reservationId)
            it[reservationId] = firstReservation
        }
        if (updated == 0) null else findByIdInternal(orderId)
    }

    fun attachPayment(orderId: UUID, paymentId: UUID): OrderResponse? = transaction {
        val updated = OrdersTable.update({ (OrdersTable.id eq orderId) and (OrdersTable.status eq OrderStatus.RESERVED.name) }) {
            it[OrdersTable.paymentId] = paymentId; it[updatedAt] = OffsetDateTime.now(ZoneOffset.UTC)
        }
        if (updated == 0) null else findByIdInternal(orderId)
    }

    fun markConfirmed(
        orderId: UUID,
        userEmail: String,
        confirmedItems: List<OrderConfirmedItem>,
    ): OrderResponse? = transaction {
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        val updated = OrdersTable.update({
            (OrdersTable.id eq orderId) and
                (OrdersTable.status eq OrderStatus.RESERVED.name)
        }) {
            it[status] = OrderStatus.CONFIRMED.name
            it[failureReason] = null
            it[updatedAt] = now
        }

        if (updated == 0) return@transaction null

        val order = findByIdInternal(orderId)!!
        val outboxEventId = UUID.randomUUID()
        val event = OrderConfirmedEvent(
            eventId = outboxEventId.toString(),
            occurredAt = now.toString(),
            orderId = order.id,
            userId = requireNotNull(order.userId),
            userEmail = userEmail,
            paymentId = requireNotNull(order.paymentId),
            amount = order.amount,
            currency = order.currency,
            items = confirmedItems,
        )

        OutboxEventsTable.insert {
            it[id] = outboxEventId
            it[aggregateType] = "Order"
            it[aggregateId] = orderId
            it[eventType] = "OrderConfirmed"
            it[topic] = "ticketflow.orders.confirmed"
            it[eventKey] = orderId.toString()
            it[payload] = Json.encodeToString(event)
            it[createdAt] = now
            it[publishedAt] = null
            it[attempts] = 0
            it[lastError] = null
        }

        order
    }

    fun issueTickets(orderId: UUID, userId: UUID): List<IssuedTicketResponse> = transaction {
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        val orderItems = OrderItemsTable.selectAll().where { OrderItemsTable.orderId eq orderId }.toList()
        orderItems.forEach { item ->
            val itemId = item[OrderItemsTable.id]
            val exists = IssuedTicketsTable.selectAll().where { IssuedTicketsTable.orderItemId eq itemId }.any()
            if (!exists) IssuedTicketsTable.insert {
                it[id] = UUID.randomUUID(); it[IssuedTicketsTable.orderId] = orderId; it[orderItemId] = itemId; it[IssuedTicketsTable.userId] = userId
                it[eventId] = item[OrderItemsTable.eventId]; it[inventoryId] = item[OrderItemsTable.inventoryId]; it[sectionId] = item[OrderItemsTable.sectionId]; it[seatId] = item[OrderItemsTable.seatId]
                it[admissionToken] = UUID.randomUUID(); it[status] = IssuedTicketStatus.ISSUED.name; it[issuedAt] = now; it[checkedInAt] = null; it[checkedInByUserId] = null
            }
        }
        findTicketsByOrderInternal(orderId)
    }

    fun findTicketsByUser(userId: UUID): List<IssuedTicketResponse> = transaction {
        IssuedTicketsTable.selectAll().where { IssuedTicketsTable.userId eq userId }.orderBy(IssuedTicketsTable.issuedAt to SortOrder.DESC).map(::toTicket)
    }

    fun findTicketById(ticketId: UUID): IssuedTicketResponse? = transaction {
        IssuedTicketsTable.selectAll().where { IssuedTicketsTable.id eq ticketId }.singleOrNull()?.let(::toTicket)
    }

    fun ticketStats(eventId: UUID): CheckInStatsResponse = transaction {
        val rows = IssuedTicketsTable.selectAll().where { IssuedTicketsTable.eventId eq eventId }.toList()
        val used = rows.count { it[IssuedTicketsTable.status] == IssuedTicketStatus.USED.name }
        val issued = rows.count { it[IssuedTicketsTable.status] == IssuedTicketStatus.ISSUED.name }
        val cancelled = rows.count { it[IssuedTicketsTable.status] == IssuedTicketStatus.CANCELLED.name }
        CheckInStatsResponse(eventId.toString(), rows.size, used, issued, cancelled)
    }

    fun findTicketByAdmissionToken(token: UUID): IssuedTicketResponse? = transaction {
        IssuedTicketsTable.selectAll().where { IssuedTicketsTable.admissionToken eq token }.singleOrNull()?.let(::toTicket)
    }

    fun checkIn(token: UUID, checkedInByUserId: UUID): IssuedTicketResponse? = transaction {
        val updated = IssuedTicketsTable.update({
            (IssuedTicketsTable.admissionToken eq token) and
                (IssuedTicketsTable.status eq IssuedTicketStatus.ISSUED.name)
        }) {
            it[status] = IssuedTicketStatus.USED.name
            it[checkedInAt] = OffsetDateTime.now(ZoneOffset.UTC)
            it[IssuedTicketsTable.checkedInByUserId] = checkedInByUserId
        }
        if (updated == 0) throw IllegalStateException("Ticket has already been used or cancelled")
        IssuedTicketsTable.selectAll()
            .where { IssuedTicketsTable.admissionToken eq token }
            .single()
            .let(::toTicket)
    }

    fun findAllTickets(): List<IssuedTicketResponse> = transaction {
        IssuedTicketsTable.selectAll()
            .orderBy(IssuedTicketsTable.issuedAt to SortOrder.DESC)
            .map(::toTicket)
    }

    fun cancelIssuedTickets(orderId: UUID) = transaction {
        IssuedTicketsTable.update({ (IssuedTicketsTable.orderId eq orderId) and (IssuedTicketsTable.status eq IssuedTicketStatus.ISSUED.name) }) { it[status] = IssuedTicketStatus.CANCELLED.name }
    }

    private fun findTicketsByOrderInternal(orderId: UUID) = IssuedTicketsTable.selectAll().where { IssuedTicketsTable.orderId eq orderId }.map(::toTicket)
    private fun toTicket(row: ResultRow) = IssuedTicketResponse(
        id = row[IssuedTicketsTable.id].toString(),
        orderId = row[IssuedTicketsTable.orderId].toString(),
        userId = row[IssuedTicketsTable.userId].toString(),
        eventId = row[IssuedTicketsTable.eventId].toString(),
        inventoryId = row[IssuedTicketsTable.inventoryId].toString(),
        sectionId = row[IssuedTicketsTable.sectionId]?.toString(),
        seatId = row[IssuedTicketsTable.seatId]?.toString(),
        admissionToken = row[IssuedTicketsTable.admissionToken].toString(),
        status = IssuedTicketStatus.valueOf(row[IssuedTicketsTable.status]),
        issuedAt = row[IssuedTicketsTable.issuedAt].toString(),
        checkedInAt = row[IssuedTicketsTable.checkedInAt]?.toString(),
        checkedInByUserId = row[IssuedTicketsTable.checkedInByUserId]?.toString(),
    )

    fun markCancelled(orderId: UUID): OrderResponse? = transaction {
        val updated=OrdersTable.update({ (OrdersTable.id eq orderId) and ((OrdersTable.status eq OrderStatus.RESERVED.name) or (OrdersTable.status eq OrderStatus.CONFIRMED.name)) }) {
            it[status]=OrderStatus.CANCELLED.name;it[failureReason]=null;it[updatedAt]=OffsetDateTime.now(ZoneOffset.UTC)
        }; if(updated==0)null else findByIdInternal(orderId)
    }
    fun markFailed(orderId: UUID, reason:String):OrderResponse=transaction{OrdersTable.update({OrdersTable.id eq orderId}){it[status]=OrderStatus.FAILED.name;it[failureReason]=reason.take(500);it[updatedAt]=OffsetDateTime.now(ZoneOffset.UTC)};findByIdInternal(orderId)!!}
    fun findById(id:UUID)=transaction{findByIdInternal(id)}
    fun findByUser(userId:UUID)=transaction{OrdersTable.selectAll().where{OrdersTable.userId eq userId}.orderBy(OrdersTable.createdAt to SortOrder.DESC).map(::toResponse)}
    fun findAll()=transaction{OrdersTable.selectAll().orderBy(OrdersTable.createdAt to SortOrder.DESC).map(::toResponse)}
    fun findExpiredReserved(now: OffsetDateTime): List<OrderResponse> = transaction {
        val expiredOrderIds = OrderItemsTable
            .selectAll()
            .where { OrderItemsTable.reservedUntil lessEq now }
            .map { it[OrderItemsTable.orderId] }
            .distinct()
        if (expiredOrderIds.isEmpty()) emptyList()
        else OrdersTable.selectAll()
            .where { (OrdersTable.id inList expiredOrderIds) and (OrdersTable.status eq OrderStatus.RESERVED.name) }
            .map(::toResponse)
    }
    fun allTicketStatuses(): List<IssuedTicketStatus> = transaction { IssuedTicketsTable.selectAll().map { IssuedTicketStatus.valueOf(it[IssuedTicketsTable.status]) } }
    private fun findByIdInternal(id:UUID)=OrdersTable.selectAll().where{OrdersTable.id eq id}.singleOrNull()?.let(::toResponse)
    private fun items(orderId:UUID)=OrderItemsTable.selectAll().where{OrderItemsTable.orderId eq orderId}.map{row->OrderItemResponse(id=row[OrderItemsTable.id].toString(), inventoryId=row[OrderItemsTable.inventoryId].toString(), reservationId=row[OrderItemsTable.reservationId]?.toString(), reservedUntil=row[OrderItemsTable.reservedUntil]?.toString(), eventId=row[OrderItemsTable.eventId].toString(), sectionId=row[OrderItemsTable.sectionId]?.toString(), seatId=row[OrderItemsTable.seatId]?.toString(), unitPrice=row[OrderItemsTable.unitPrice].toPlainString(), currency=row[OrderItemsTable.currency])}
    private fun toResponse(row:ResultRow):OrderResponse{val its=items(row[OrdersTable.id]);return OrderResponse(id=row[OrdersTable.id].toString(), userId=row[OrdersTable.userId]?.toString(), inventoryId=its.singleOrNull()?.inventoryId, reservationId=its.singleOrNull()?.reservationId, paymentId=row[OrdersTable.paymentId]?.toString(), amount=row[OrdersTable.amount].toPlainString(), currency=row[OrdersTable.currency], status=OrderStatus.valueOf(row[OrdersTable.status]), failureReason=row[OrdersTable.failureReason], items=its, reservedUntil=its.mapNotNull { it.reservedUntil }.minOrNull(), createdAt=row[OrdersTable.createdAt].toString(), updatedAt=row[OrdersTable.updatedAt].toString())}
}
