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
    val mapX: Double? = null, val mapY: Double? = null, val mapWidth: Double? = null, val mapHeight: Double? = null, val mapRotation: Double = 0.0,
)

@Serializable
data class VenueSectionResponse(
    val id: String,
    val venueId: String,
    val name: String,
    val type: VenueSectionType,
    val capacity: Int,
    val mapX: Double? = null, val mapY: Double? = null, val mapWidth: Double? = null, val mapHeight: Double? = null, val mapRotation: Double = 0.0,
)
@Serializable data class UpdateSectionMapRequest(val x:Double?=null,val y:Double?=null,val width:Double?=null,val height:Double?=null,val rotation:Double=0.0)
