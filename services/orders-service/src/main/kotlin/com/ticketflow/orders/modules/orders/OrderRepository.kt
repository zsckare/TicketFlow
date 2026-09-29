package com.ticketflow.orders.modules.orders

import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

class OrderRepository {

    fun createPending(
        inventoryId: UUID,
        amount: BigDecimal,
        currency: String,
    ): OrderResponse =
        transaction {
            val now =
                OffsetDateTime.now(
                    ZoneOffset.UTC
                )

            val id =
                UUID.randomUUID()

            OrdersTable.insert {
                it[OrdersTable.id] = id
                it[OrdersTable.inventoryId] = inventoryId
                it[OrdersTable.amount] = amount
                it[OrdersTable.currency] = currency
                it[status] = OrderStatus.PENDING.name
                it[createdAt] = now
                it[updatedAt] = now
            }

            findByIdInternal(id)!!
        }

    fun markReserved(
        orderId: UUID,
        reservationId: UUID,
    ): OrderResponse =
        transaction {
            OrdersTable.update({
                OrdersTable.id eq orderId
            }) {
                it[OrdersTable.reservationId] =
                    reservationId

                it[status] =
                    OrderStatus.RESERVED.name

                it[updatedAt] =
                    OffsetDateTime.now(
                        ZoneOffset.UTC
                    )
            }

            findByIdInternal(orderId)!!
        }

    fun markFailed(
        orderId: UUID,
        reason: String,
    ): OrderResponse =
        transaction {
            OrdersTable.update({
                OrdersTable.id eq orderId
            }) {
                it[status] =
                    OrderStatus.FAILED.name

                it[failureReason] =
                    reason

                it[updatedAt] =
                    OffsetDateTime.now(
                        ZoneOffset.UTC
                    )
            }

            findByIdInternal(orderId)!!
        }

    fun findById(
        id: UUID,
    ): OrderResponse? =
        transaction {
            findByIdInternal(id)
        }

    fun findAll(): List<OrderResponse> =
        transaction {
            OrdersTable
                .selectAll()
                .map(::toResponse)
        }

    private fun findByIdInternal(
        id: UUID,
    ): OrderResponse? =
        OrdersTable
            .selectAll()
            .where {
                OrdersTable.id eq id
            }
            .singleOrNull()
            ?.let(::toResponse)

    private fun toResponse(
        row: ResultRow,
    ): OrderResponse =
        OrderResponse(
            id =
                row[OrdersTable.id]
                    .toString(),

            inventoryId =
                row[OrdersTable.inventoryId]
                    .toString(),

            reservationId =
                row[OrdersTable.reservationId]
                    ?.toString(),

            amount =
                row[OrdersTable.amount]
                    .toPlainString(),

            currency =
                row[OrdersTable.currency],

            status =
                OrderStatus.valueOf(
                    row[OrdersTable.status]
                ),

            failureReason =
                row[OrdersTable.failureReason],

            createdAt =
                row[OrdersTable.createdAt]
                    .toString(),

            updatedAt =
                row[OrdersTable.updatedAt]
                    .toString(),
        )
}