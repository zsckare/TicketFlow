package com.ticketflow.events.modules.venues

import kotlinx.serialization.Serializable

/**
 * Representación pública de un recinto.
 */
@Serializable
data class VenueResponse(
    val id: String,
    val name: String,
    val address: String,
    val city: String,
    val timezone: String = "America/Monterrey",
)

/**
 * Datos necesarios para crear un recinto.
 *
 * El cliente no proporciona el ID.
 * Es responsabilidad del Events Service generarlo.
 */
@Serializable
data class CreateVenueRequest(
    val name: String,
    val address: String,
    val city: String,
    val timezone: String = "America/Monterrey",
)