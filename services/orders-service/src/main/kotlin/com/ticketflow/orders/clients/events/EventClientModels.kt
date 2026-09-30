package com.ticketflow.orders.clients.events

import kotlinx.serialization.Serializable

@Serializable
data class EventResponse(
    val id: String,
    val venueId: String,
    val name: String,
    val description: String? = null,
    val startsAt: String,
    val endsAt: String,
    val status: String,
)

@Serializable
data class VenueResponse(
    val id: String,
    val name: String,
    val address: String,
    val city: String,
    val timezone: String = "America/Monterrey",
)

@Serializable
data class VenueSectionResponse(
    val id: String,
    val venueId: String,
    val name: String,
    val type: String,
    val capacity: Int,
)

@Serializable
data class SeatResponse(val id: String, val sectionId: String, val row: String, val number: String)
