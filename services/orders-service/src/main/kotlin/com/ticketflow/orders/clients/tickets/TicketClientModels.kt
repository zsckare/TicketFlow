package com.ticketflow.orders.clients.tickets

import kotlinx.serialization.Serializable

/**
 * Local representation of the contract exposed by Tickets Service.
 *
 * We intentionally do not share the Tickets Service model classes.
 * Orders owns its representation of the remote API contract.
 */
@Serializable
enum class TicketInventoryStatus {
    AVAILABLE,
    RESERVED,
    SOLD,
}

@Serializable
data class TicketInventoryResponse(
    val id: String,
    val eventId: String,
    val seatId: String,
    val price: String,
    val currency: String,
    val status: TicketInventoryStatus,
    val reservationId: String? = null,
    val reservedUntil: String? = null,
)

@Serializable
data class ReserveTicketResponse(
    val inventory: TicketInventoryResponse,
)

@Serializable
data class ReleaseTicketRequest(
    val reservationId: String,
)