package com.ticketflow.orders.modules.orders

import com.ticketflow.orders.clients.tickets.TicketInventoryResponse
import com.ticketflow.orders.outbox.OrderConfirmedEvent
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

    fun attachReservation(orderId: UUID, inventoryId: UUID, reservationId: UUID) = transaction {
        OrderItemsTable.update({ (OrderItemsTable.orderId eq orderId) and (OrderItemsTable.inventoryId eq inventoryId) }) {
            it[OrderItemsTable.reservationId] = reservationId
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

    fun markConfirmed(orderId: UUID, userEmail: String): OrderResponse? = transaction {
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        val updated = OrdersTable.update({ (OrdersTable.id eq orderId) and (OrdersTable.status eq OrderStatus.RESERVED.name) }) {
            it[status] = OrderStatus.CONFIRMED.name; it[failureReason] = null; it[updatedAt] = now
        }
        if (updated == 0) return@transaction null
        val order = findByIdInternal(orderId)!!
        val eventId = UUID.randomUUID()
        val event = OrderConfirmedEvent(eventId.toString(), now.toString(), order.id, requireNotNull(order.userId), userEmail,
            order.items.first().inventoryId, requireNotNull(order.paymentId), order.amount, order.currency)
        OutboxEventsTable.insert {
            it[id]=eventId; it[aggregateType]="Order"; it[aggregateId]=orderId; it[eventType]="OrderConfirmed"
            it[topic]="ticketflow.orders.confirmed"; it[eventKey]=orderId.toString(); it[payload]=Json.encodeToString(event)
            it[createdAt]=now; it[publishedAt]=null; it[attempts]=0; it[lastError]=null
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
                it[admissionToken] = UUID.randomUUID(); it[status] = IssuedTicketStatus.ISSUED.name; it[issuedAt] = now; it[checkedInAt] = null
            }
        }
        findTicketsByOrderInternal(orderId)
    }

    fun findTicketsByUser(userId: UUID): List<IssuedTicketResponse> = transaction {
        IssuedTicketsTable.selectAll().where { IssuedTicketsTable.userId eq userId }.orderBy(IssuedTicketsTable.issuedAt to SortOrder.DESC).map(::toTicket)
    }

    fun checkIn(token: UUID): IssuedTicketResponse? = transaction {
        val row = IssuedTicketsTable.selectAll().where { IssuedTicketsTable.admissionToken eq token }.singleOrNull() ?: return@transaction null
        if (row[IssuedTicketsTable.status] != IssuedTicketStatus.ISSUED.name) throw IllegalStateException("Ticket has already been used or cancelled")
        IssuedTicketsTable.update({ IssuedTicketsTable.admissionToken eq token }) { it[status] = IssuedTicketStatus.USED.name; it[checkedInAt] = OffsetDateTime.now(ZoneOffset.UTC) }
        IssuedTicketsTable.selectAll().where { IssuedTicketsTable.admissionToken eq token }.single().let(::toTicket)
    }

    fun cancelIssuedTickets(orderId: UUID) = transaction {
        IssuedTicketsTable.update({ (IssuedTicketsTable.orderId eq orderId) and (IssuedTicketsTable.status eq IssuedTicketStatus.ISSUED.name) }) { it[status] = IssuedTicketStatus.CANCELLED.name }
    }

    private fun findTicketsByOrderInternal(orderId: UUID) = IssuedTicketsTable.selectAll().where { IssuedTicketsTable.orderId eq orderId }.map(::toTicket)
    private fun toTicket(row: ResultRow) = IssuedTicketResponse(row[IssuedTicketsTable.id].toString(),row[IssuedTicketsTable.orderId].toString(),row[IssuedTicketsTable.userId].toString(),row[IssuedTicketsTable.eventId].toString(),row[IssuedTicketsTable.inventoryId].toString(),row[IssuedTicketsTable.sectionId]?.toString(),row[IssuedTicketsTable.seatId]?.toString(),row[IssuedTicketsTable.admissionToken].toString(),IssuedTicketStatus.valueOf(row[IssuedTicketsTable.status]),row[IssuedTicketsTable.issuedAt].toString(),row[IssuedTicketsTable.checkedInAt]?.toString())

    fun markCancelled(orderId: UUID): OrderResponse? = transaction {
        val updated=OrdersTable.update({ (OrdersTable.id eq orderId) and ((OrdersTable.status eq OrderStatus.RESERVED.name) or (OrdersTable.status eq OrderStatus.CONFIRMED.name)) }) {
            it[status]=OrderStatus.CANCELLED.name;it[failureReason]=null;it[updatedAt]=OffsetDateTime.now(ZoneOffset.UTC)
        }; if(updated==0)null else findByIdInternal(orderId)
    }
    fun markFailed(orderId: UUID, reason:String):OrderResponse=transaction{OrdersTable.update({OrdersTable.id eq orderId}){it[status]=OrderStatus.FAILED.name;it[failureReason]=reason.take(500);it[updatedAt]=OffsetDateTime.now(ZoneOffset.UTC)};findByIdInternal(orderId)!!}
    fun findById(id:UUID)=transaction{findByIdInternal(id)}
    fun findByUser(userId:UUID)=transaction{OrdersTable.selectAll().where{OrdersTable.userId eq userId}.orderBy(OrdersTable.createdAt to SortOrder.DESC).map(::toResponse)}
    fun findAll()=transaction{OrdersTable.selectAll().orderBy(OrdersTable.createdAt to SortOrder.DESC).map(::toResponse)}
    private fun findByIdInternal(id:UUID)=OrdersTable.selectAll().where{OrdersTable.id eq id}.singleOrNull()?.let(::toResponse)
    private fun items(orderId:UUID)=OrderItemsTable.selectAll().where{OrderItemsTable.orderId eq orderId}.map{row->OrderItemResponse(row[OrderItemsTable.id].toString(),row[OrderItemsTable.inventoryId].toString(),row[OrderItemsTable.reservationId]?.toString(),row[OrderItemsTable.eventId].toString(),row[OrderItemsTable.sectionId]?.toString(),row[OrderItemsTable.seatId]?.toString(),row[OrderItemsTable.unitPrice].toPlainString(),row[OrderItemsTable.currency])}
    private fun toResponse(row:ResultRow):OrderResponse{val its=items(row[OrdersTable.id]);return OrderResponse(row[OrdersTable.id].toString(),row[OrdersTable.userId]?.toString(),its.singleOrNull()?.inventoryId,its.singleOrNull()?.reservationId,row[OrdersTable.paymentId]?.toString(),row[OrdersTable.amount].toPlainString(),row[OrdersTable.currency],OrderStatus.valueOf(row[OrdersTable.status]),row[OrdersTable.failureReason],its,row[OrdersTable.createdAt].toString(),row[OrdersTable.updatedAt].toString())}
}
