package com.ticketflow.orders.modules.orders

import com.ticketflow.orders.clients.tickets.TicketInventoryStatus
import com.ticketflow.orders.clients.tickets.TicketsClient
import java.math.BigDecimal
import java.util.UUID

class OrderService(
    private val repository: OrderRepository,
    private val ticketsClient: TicketsClient,
) {

    /**
     * Creates an Order and reserves the ticket.
     *
     * Orders Service orchestrates the workflow, while Tickets Service
     * remains the authority over inventory availability.
     */
    suspend fun create(
        request: CreateOrderRequest,
    ): OrderResponse {

        val inventoryId =
            parseUuid(
                request.inventoryId,
                "Invalid inventoryId",
            )

        val inventory =
            ticketsClient.findInventoryById(
                inventoryId.toString()
            )
                ?: throw TicketNotAvailableException()

        if (
            inventory.status !=
            TicketInventoryStatus.AVAILABLE
        ) {
            throw TicketNotAvailableException()
        }

        /*
         * Store a price snapshot in the Order.
         *
         * The Order should not depend on a future inventory price
         * remaining unchanged.
         */
        val order =
            repository.createPending(
                inventoryId = inventoryId,
                amount = BigDecimal(inventory.price),
                currency = inventory.currency,
            )

        val orderId =
            UUID.fromString(order.id)

        /*
         * Cross-service operation.
         *
         * This cannot participate in the transaction that inserted
         * the Order because Tickets owns a different database.
         */
        val reservation =
            try {
                ticketsClient.reserve(
                    inventoryId.toString()
                )
            } catch (cause: Exception) {
                repository.markFailed(
                    orderId = orderId,
                    reason = "Tickets Service unavailable",
                )

                throw cause
            }

        if (reservation == null) {
            return repository.markFailed(
                orderId = orderId,
                reason = "Ticket is not available",
            )
        }

        val reservationIdValue =
            reservation.inventory.reservationId

        /*
         * A successful reserve must always return its reservationId.
         *
         * If the remote contract is unexpectedly broken, attempt
         * compensation when possible.
         */
        if (reservationIdValue == null) {
            return repository.markFailed(
                orderId = orderId,
                reason = "Tickets Service returned no reservationId",
            )
        }

        val reservationId =
            UUID.fromString(reservationIdValue)

        /*
         * Persist the successful reservation locally.
         */
        val reservedOrder =
            try {
                repository.markReserved(
                    orderId = orderId,
                    reservationId = reservationId,
                )
            } catch (cause: Exception) {
                compensateReservation(
                    inventoryId = inventoryId,
                    reservationId = reservationId,
                )

                throw cause
            }

        /*
         * If the local transition failed after Tickets successfully
         * reserved the ticket, release the remote reservation.
         *
         * This is our first compensating transaction.
         */
        if (reservedOrder == null) {
            compensateReservation(
                inventoryId = inventoryId,
                reservationId = reservationId,
            )

            return repository.markFailed(
                orderId = orderId,
                reason = "Unable to persist reservation",
            )
        }

        return reservedOrder
    }

    /**
     * Confirms a RESERVED Order.
     *
     * Tickets transitions:
     * RESERVED -> SOLD
     *
     * Orders transitions:
     * RESERVED -> CONFIRMED
     */
    suspend fun confirm(
        orderId: UUID,
    ): OrderResponse {

        val order =
            findById(orderId)

        /*
         * Basic idempotency:
         * confirming an already confirmed Order simply returns it.
         */
        if (order.status == OrderStatus.CONFIRMED) {
            return order
        }

        if (order.status != OrderStatus.RESERVED) {
            throw InvalidOrderStateException(
                "Only RESERVED orders can be confirmed"
            )
        }

        val reservationId =
            order.reservationId
                ?: throw OrderOperationException(
                    "Order has no reservationId"
                )

        val inventory =
            ticketsClient.confirm(
                inventoryId = order.inventoryId,
                reservationId = reservationId,
            )
                ?: throw OrderOperationException(
                    "Ticket reservation could not be confirmed"
                )

        if (
            inventory.status !=
            TicketInventoryStatus.SOLD
        ) {
            throw OrderOperationException(
                "Tickets Service did not mark inventory as SOLD"
            )
        }

        /*
         * There is intentionally still a distributed consistency
         * window here:
         *
         * Tickets may become SOLD and the process could die before
         * Orders becomes CONFIRMED.
         *
         * Kafka/Outbox/reconciliation will address this later.
         */
        return repository.markConfirmed(orderId)
            ?: throw OrderOperationException(
                "Unable to mark order as CONFIRMED"
            )
    }

    /**
     * Cancels a RESERVED Order.
     *
     * The ticket reservation is released before the local Order is
     * marked CANCELLED.
     */
    suspend fun cancel(
        orderId: UUID,
    ): OrderResponse {

        val order =
            findById(orderId)

        /*
         * Basic idempotency.
         */
        if (order.status == OrderStatus.CANCELLED) {
            return order
        }

        if (order.status != OrderStatus.RESERVED) {
            throw InvalidOrderStateException(
                "Only RESERVED orders can be cancelled"
            )
        }

        val reservationId =
            order.reservationId
                ?: throw OrderOperationException(
                    "Order has no reservationId"
                )

        val inventory =
            ticketsClient.release(
                inventoryId = order.inventoryId,
                reservationId = reservationId,
            )
                ?: throw OrderOperationException(
                    "Ticket reservation could not be released"
                )

        if (
            inventory.status !=
            TicketInventoryStatus.AVAILABLE
        ) {
            throw OrderOperationException(
                "Tickets Service did not release inventory"
            )
        }

        return repository.markCancelled(orderId)
            ?: throw OrderOperationException(
                "Unable to mark order as CANCELLED"
            )
    }

    fun findById(
        orderId: UUID,
    ): OrderResponse =
        repository.findById(orderId)
            ?: throw OrderNotFoundException(
                orderId.toString()
            )

    fun findAll(): List<OrderResponse> =
        repository.findAll()

    /**
     * Best-effort compensation.
     *
     * For the MVP we attempt the release immediately.
     *
     * Later this becomes durable through retries/outbox/events.
     */
    private suspend fun compensateReservation(
        inventoryId: UUID,
        reservationId: UUID,
    ) {
        try {
            ticketsClient.release(
                inventoryId = inventoryId.toString(),
                reservationId = reservationId.toString(),
            )
        } catch (_: Exception) {
            /*
             * Intentionally best-effort for MVP.
             *
             * The ticket also has an expiration time, providing a
             * second safety mechanism against permanent reservation.
             *
             * Later we will persist compensation work and retry it.
             */
        }
    }

    private fun parseUuid(
        value: String,
        errorMessage: String,
    ): UUID =
        try {
            UUID.fromString(value)
        } catch (_: IllegalArgumentException) {
            throw IllegalArgumentException(
                errorMessage
            )
        }
}