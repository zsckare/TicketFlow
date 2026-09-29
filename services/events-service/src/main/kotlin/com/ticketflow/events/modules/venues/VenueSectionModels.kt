package com.ticketflow.events.modules.sections

import kotlinx.serialization.Serializable

/**
 * Tipo de sección dentro de un venue.
 *
 * GENERAL_ADMISSION:
 * No existen asientos individuales. Se controla mediante capacidad.
 *
 * RESERVED_SEATING:
 * La sección contiene asientos individuales identificables.
 */
@Serializable
enum class VenueSectionType {
    GENERAL_ADMISSION,
    RESERVED_SEATING,
}

@Serializable
data class CreateVenueSectionRequest(
    val name: String,
    val type: VenueSectionType,
    val capacity: Int,
)

@Serializable
data class VenueSectionResponse(
    val id: String,
    val venueId: String,
    val name: String,
    val type: VenueSectionType,
    val capacity: Int,
)