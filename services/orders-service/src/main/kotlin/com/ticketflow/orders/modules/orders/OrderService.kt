package com.ticketflow.orders.modules.orders

import com.ticketflow.orders.clients.tickets.TicketInventoryStatus
import com.ticketflow.orders.clients.tickets.TicketsClient
import java.math.BigDecimal
import java.util.UUID

class OrderService(
    private val repository: OrderRepository,
    private val ticketsClient: TicketsClient,
) {

    suspend fun create(
        request: CreateOrderRequest,
    ): OrderResponse {

        val inventoryId =
            try {
                UUID.fromString(
                    request.inventoryId
                )
            } catch (_: IllegalArgumentException) {
                throw IllegalArgumentException(
                    "Invalid inventoryId"
                )
            }

        /*
         * First retrieve the inventory from Tickets Service.
         *
         * Orders Service does NOT read tickets_db directly.
         */
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
         * Snapshot the price in the order.
         *
         * This is important because the inventory price could
         * theoretically change later. The order needs to remember
         * the price at which it was created.
         */
        val order =
            repository.createPending(
                inventoryId = inventoryId,
                amount =
                    BigDecimal(
                        inventory.price
                    ),
                currency =
                    inventory.currency,
            )

        /*
         * We now cross the microservice boundary.
         *
         * This operation cannot participate in the PostgreSQL
         * transaction that created the Order.
         */
        val reservation =
            try {
                ticketsClient.reserve(
                    inventoryId.toString()
                )
            } catch (cause: Exception) {

                repository.markFailed(
                    orderId =
                        UUID.fromString(
                            order.id
                        ),
                    reason =
                        "Tickets Service unavailable",
                )

                throw cause
            }

        /*
         * Another customer could have reserved the ticket between
         * our GET and POST /reserve.
         *
         * Tickets Service remains the authority on availability.
         */
        if (reservation == null) {
            return repository.markFailed(
                orderId =
                    UUID.fromString(
                        order.id
                    ),
                reason =
                    "Ticket is not available",
            )
        }

        val reservationId =
            reservation.inventory.reservationId
                ?: return repository.markFailed(
                    orderId =
                        UUID.fromString(
                            order.id
                        ),
                    reason =
                        "Tickets Service returned no reservationId",
                )

        return repository.markReserved(
            orderId =
                UUID.fromString(
                    order.id
                ),

            reservationId =
                UUID.fromString(
                    reservationId
                ),
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
}