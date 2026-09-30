package com.ticketflow.orders.clients.tickets

import kotlinx.serialization.Serializable

/**
 * Local representation of Tickets Service inventory status.
 *
 * Orders Service deliberately owns its copy of the remote contract.
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
    val sectionId: String? = null,
    val seatId: String? = null,
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

@Serializable
data class ConfirmTicketRequest(
    val reservationId: String,
)