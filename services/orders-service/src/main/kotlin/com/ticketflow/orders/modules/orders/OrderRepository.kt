package com.ticketflow.orders.modules.orders

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import com.ticketflow.orders.outbox.OutboxEventsTable
import com.ticketflow.orders.outbox.OrderConfirmedEvent
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

class OrderRepository {

    fun createPending(
        userId: UUID,
        inventoryId: UUID,
        amount: BigDecimal,
        currency: String,
    ): OrderResponse = transaction {
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        val id = UUID.randomUUID()

        OrdersTable.insert {
            it[OrdersTable.id] = id
            it[OrdersTable.userId] = userId
            it[OrdersTable.inventoryId] = inventoryId
            it[OrdersTable.amount] = amount
            it[OrdersTable.currency] = currency
            it[status] = OrderStatus.PENDING.name
            it[createdAt] = now
            it[updatedAt] = now
        }

        findByIdInternal(id)!!
    }

    fun markReserved(orderId: UUID, reservationId: UUID): OrderResponse? = transaction {
        val updated = OrdersTable.update({
            (OrdersTable.id eq orderId) and
                (OrdersTable.status eq OrderStatus.PENDING.name)
        }) {
            it[OrdersTable.reservationId] = reservationId
            it[status] = OrderStatus.RESERVED.name
            it[failureReason] = null
            it[updatedAt] = OffsetDateTime.now(ZoneOffset.UTC)
        }
        if (updated == 0) null else findByIdInternal(orderId)
    }

    fun attachPayment(orderId: UUID, paymentId: UUID): OrderResponse? = transaction {
        val updated = OrdersTable.update({
            (OrdersTable.id eq orderId) and
                (OrdersTable.status eq OrderStatus.RESERVED.name)
        }) {
            it[OrdersTable.paymentId] = paymentId
            it[updatedAt] = OffsetDateTime.now(ZoneOffset.UTC)
        }
        if (updated == 0) null else findByIdInternal(orderId)
    }

    /** Atomically confirms the order and records its domain event in the outbox. */
    fun markConfirmed(orderId: UUID, userEmail: String): OrderResponse? = transaction {
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
        val eventId = UUID.randomUUID()
        val event = OrderConfirmedEvent(
            eventId = eventId.toString(), occurredAt = now.toString(), orderId = order.id,
            userId = requireNotNull(order.userId), userEmail = userEmail, inventoryId = order.inventoryId,
            paymentId = requireNotNull(order.paymentId), amount = order.amount, currency = order.currency,
        )
        OutboxEventsTable.insert {
            it[id] = eventId; it[aggregateType] = "Order"; it[aggregateId] = orderId
            it[eventType] = "OrderConfirmed"; it[topic] = "ticketflow.orders.confirmed"
            it[eventKey] = orderId.toString(); it[payload] = Json.encodeToString(event)
            it[createdAt] = now; it[publishedAt] = null; it[attempts] = 0; it[lastError] = null
        }
        order
    }

    fun markCancelled(orderId: UUID): OrderResponse? = transaction {
        val updated = OrdersTable.update({
            (OrdersTable.id eq orderId) and
                (OrdersTable.status eq OrderStatus.RESERVED.name)
        }) {
            it[status] = OrderStatus.CANCELLED.name
            it[failureReason] = null
            it[updatedAt] = OffsetDateTime.now(ZoneOffset.UTC)
        }
        if (updated == 0) null else findByIdInternal(orderId)
    }

    fun markFailed(orderId: UUID, reason: String): OrderResponse = transaction {
        OrdersTable.update({ OrdersTable.id eq orderId }) {
            it[status] = OrderStatus.FAILED.name
            it[failureReason] = reason.take(500)
            it[updatedAt] = OffsetDateTime.now(ZoneOffset.UTC)
        }
        findByIdInternal(orderId)!!
    }

    fun findById(id: UUID): OrderResponse? = transaction { findByIdInternal(id) }

    fun findByUser(userId: UUID): List<OrderResponse> = transaction {
        OrdersTable.selectAll()
            .where { OrdersTable.userId eq userId }
            .orderBy(OrdersTable.createdAt to SortOrder.DESC)
            .map(::toResponse)
    }

    fun findAll(): List<OrderResponse> = transaction {
        OrdersTable.selectAll()
            .orderBy(OrdersTable.createdAt to SortOrder.DESC)
            .map(::toResponse)
    }

    private fun findByIdInternal(id: UUID): OrderResponse? =
        OrdersTable.selectAll()
            .where { OrdersTable.id eq id }
            .singleOrNull()
            ?.let(::toResponse)

    private fun toResponse(row: ResultRow): OrderResponse = OrderResponse(
        id = row[OrdersTable.id].toString(),
        userId = row[OrdersTable.userId]?.toString(),
        inventoryId = row[OrdersTable.inventoryId].toString(),
        reservationId = row[OrdersTable.reservationId]?.toString(),
        paymentId = row[OrdersTable.paymentId]?.toString(),
        amount = row[OrdersTable.amount].toPlainString(),
        currency = row[OrdersTable.currency],
        status = OrderStatus.valueOf(row[OrdersTable.status]),
        failureReason = row[OrdersTable.failureReason],
        createdAt = row[OrdersTable.createdAt].toString(),
        updatedAt = row[OrdersTable.updatedAt].toString(),
    )
}
