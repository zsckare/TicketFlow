package com.ticketflow.events.modules.venues

import kotlinx.serialization.Serializable

@Serializable
data class CreateSeatRequest(
    val row: String,
    val number: String,
)

@Serializable
data class SeatResponse(
    val id: String,
    val sectionId: String,
    val row: String,
    val number: String,
)