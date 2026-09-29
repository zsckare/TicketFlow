package com.ticketflow.tickets.modules.inventory

import kotlinx.serialization.Serializable

/**
 * Estados posibles del inventario.
 */
@Serializable
enum class TicketInventoryStatus {
    AVAILABLE,
    RESERVED,
    SOLD,
}

/**
 * Request utilizado para crear inventario para un asiento
 * dentro de un evento.
 */
@Serializable
data class CreateTicketInventoryRequest(
    val eventId: String,
    val seatId: String,
    val price: String,
    val currency: String,
)

/**
 * Representación pública de un registro de inventario.
 */
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

/**
 * Resultado de reservar temporalmente un ticket.
 */
@Serializable
data class ReserveTicketResponse(
    val inventory: TicketInventoryResponse,
)

/**
 * Request para liberar una reservación.
 *
 * reservationId funciona como token de ownership:
 * solamente quien posee la reservación puede liberarla.
 */
@Serializable
data class ReleaseTicketRequest(
    val reservationId: String,
)

/**
 * Request para confirmar una reservación.
 *
 * Por ahora confirmar significa convertir:
 *
 * RESERVED -> SOLD
 *
 * Más adelante esta transición estará coordinada
 * por Orders/Payments.
 */
@Serializable
data class ConfirmTicketRequest(
    val reservationId: String,
)