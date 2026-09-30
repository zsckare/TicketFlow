package com.ticketflow.events.modules.events

import kotlinx.serialization.Serializable

/**
 * Estados posibles de un evento.
 *
 * DRAFT:
 * El evento todavía está siendo configurado.
 *
 * PUBLISHED:
 * El evento ya está disponible para generar/vender tickets.
 *
 * CANCELLED:
 * El evento fue cancelado.
 *
 * FINISHED:
 * El evento ya ocurrió.
 */
@Serializable
enum class EventStatus {
    DRAFT,
    PUBLISHED,
    CANCELLED,
    FINISHED,
}

/**
 * Request utilizado para crear un nuevo evento.
 *
 * Las fechas se reciben como ISO-8601.
 *
 * Ejemplo:
 * 2026-11-28T20:00:00Z
 */
@Serializable
data class CreateEventRequest(
    val venueId: String,
    val name: String,
    val description: String? = null,
    val startsAt: String,
    val endsAt: String? = null,
)


@Serializable
data class UpdateEventRequest(
    val name: String,
    val description: String? = null,
    val startsAt: String,
    val endsAt: String? = null,
)

/**
 * Representación pública de un evento.
 */
@Serializable
data class EventResponse(
    val id: String,
    val venueId: String,
    val name: String,
    val description: String?,
    val startsAt: String,
    val endsAt: String?,
    val status: EventStatus,
)