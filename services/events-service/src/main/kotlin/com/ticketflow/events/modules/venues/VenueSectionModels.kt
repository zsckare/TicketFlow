package com.ticketflow.events.modules.venues

import kotlinx.serialization.Serializable

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