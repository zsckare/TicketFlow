package com.ticketflow.tickets.clients.events

import kotlinx.serialization.Serializable

/**
 * Estados de un evento que Tickets Service necesita conocer.
 *
 * Este modelo pertenece a Tickets Service.
 * No estamos compartiendo las clases Kotlin de Events Service.
 */
@Serializable
enum class EventStatus {
    DRAFT,
    PUBLISHED,
    CANCELLED,
    FINISHED,
}

/**
 * Representación local del contrato HTTP que recibimos
 * desde Events Service.
 *
 * Tickets Service solamente conoce los datos que necesita
 * para trabajar con un evento.
 */
@Serializable
data class EventResponse(
    val id: String,
    val venueId: String,
    val name: String,
    val description: String? = null,
    val startsAt: String,
    val endsAt: String? = null,
    val status: EventStatus,
)

@Serializable
data class SeatResponse(
    val id: String,
    val sectionId: String,
    val row: String,
    val number: String,
)

@Serializable
data class VenueSectionResponse(
    val id: String,
    val venueId: String,
    val name: String,
    val type: VenueSectionType,
    val capacity: Int,
)

@Serializable
enum class VenueSectionType {
    GENERAL_ADMISSION,
    RESERVED_SEATING,
}