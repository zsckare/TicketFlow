package com.ticketflow.tickets.modules.inventory

/**
 * Se lanza cuando el inventario solicitado no existe.
 */
class TicketInventoryNotFoundException(
    inventoryId: String,
) : RuntimeException(
    "Ticket inventory '$inventoryId' does not exist",
)

/**
 * Se lanza cuando una operación no es válida para el
 * estado actual del ticket.
 *
 * Ejemplo:
 * intentar reservar un ticket que ya está RESERVED o SOLD.
 */
class TicketInventoryConflictException(
    message: String,
) : RuntimeException(message)

/**
 * Se lanza cuando un reservationId no corresponde con
 * la reservación que actualmente posee el ticket.
 */
class InvalidReservationException(
    message: String,
) : RuntimeException(message)