package com.ticketflow.events.modules.seats

import kotlinx.serialization.Serializable

/**
 * Request para crear un asiento físico dentro de una sección.
 *
 * Ejemplo:
 * row = "A"
 * number = "1"
 */
@Serializable
data class CreateSeatRequest(
    val row: String,
    val number: String,
)

/**
 * Representación pública de un asiento.
 */
@Serializable
data class SeatResponse(
    val id: String,
    val sectionId: String,
    val row: String,
    val number: String,
)