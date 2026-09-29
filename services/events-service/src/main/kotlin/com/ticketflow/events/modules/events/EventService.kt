package com.ticketflow.events.modules.events

import com.ticketflow.events.modules.venues.VenueRepository
import java.time.OffsetDateTime
import java.time.format.DateTimeParseException
import java.util.UUID

/**
 * Contiene las reglas de negocio relacionadas con eventos.
 */
class EventService(
    private val eventRepository: EventRepository,
    private val venueRepository: VenueRepository,
) {

    /**
     * Crea un evento nuevo.
     *
     * Todos los eventos comienzan en DRAFT.
     */
    fun create(
        request: CreateEventRequest,
    ): EventResponse {

        require(request.name.isNotBlank()) {
            "Event name cannot be blank"
        }

        val venueId =
            parseUuid(
                value = request.venueId,
                field = "venueId",
            )

        require(
            venueRepository.exists(venueId),
        ) {
            "Venue does not exist"
        }

        val startsAt =
            parseDateTime(
                value = request.startsAt,
                field = "startsAt",
            )

        val endsAt =
            request.endsAt?.let {
                parseDateTime(
                    value = it,
                    field = "endsAt",
                )
            }

        if (endsAt != null) {
            require(
                endsAt.isAfter(startsAt),
            ) {
                "Event end date must be after start date"
            }
        }

        return eventRepository.create(
            venueId = venueId,
            name = request.name.trim(),
            description =
                request.description
                    ?.trim()
                    ?.takeIf { it.isNotEmpty() },
            startsAt = startsAt,
            endsAt = endsAt,
        )
    }

    /**
     * Obtiene todos los eventos.
     */
    fun findAll(): List<EventResponse> =
        eventRepository.findAll()

    /**
     * Obtiene un evento concreto.
     */
    fun findById(
        id: UUID,
    ): EventResponse? =
        eventRepository.findById(id)

    /**
     * Publica un evento.
     *
     * Solo permitimos publicar eventos que actualmente
     * estén en estado DRAFT.
     */
    fun publish(
        id: UUID,
    ): EventResponse {

        val event =
            eventRepository.findById(id)
                ?: error("Event does not exist")

        require(
            event.status == EventStatus.DRAFT,
        ) {
            "Only draft events can be published"
        }

        return eventRepository.updateStatus(
            id = id,
            status = EventStatus.PUBLISHED,
        ) ?: error("Event does not exist")
    }

    /**
     * Cancela un evento.
     *
     * Por ahora permitimos cancelar eventos DRAFT o PUBLISHED.
     */
    fun cancel(
        id: UUID,
    ): EventResponse {

        val event =
            eventRepository.findById(id)
                ?: error("Event does not exist")

        require(
            event.status == EventStatus.DRAFT ||
                    event.status == EventStatus.PUBLISHED,
        ) {
            "Event cannot be cancelled from status ${event.status}"
        }

        return eventRepository.updateStatus(
            id = id,
            status = EventStatus.CANCELLED,
        ) ?: error("Event does not exist")
    }

    /**
     * Convierte un String en UUID y proporciona un error
     * de dominio más comprensible.
     */
    private fun parseUuid(
        value: String,
        field: String,
    ): UUID =
        try {
            UUID.fromString(value)
        } catch (_: IllegalArgumentException) {
            throw IllegalArgumentException(
                "$field must be a valid UUID",
            )
        }

    /**
     * Convierte una fecha ISO-8601 en OffsetDateTime.
     */
    private fun parseDateTime(
        value: String,
        field: String,
    ): OffsetDateTime =
        try {
            OffsetDateTime.parse(value)
        } catch (_: DateTimeParseException) {
            throw IllegalArgumentException(
                "$field must be a valid ISO-8601 date",
            )
        }
}