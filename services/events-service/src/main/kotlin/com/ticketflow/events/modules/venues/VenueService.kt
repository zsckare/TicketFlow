package com.ticketflow.events.modules.venues

import java.time.ZoneId
import java.util.UUID

/**
 * Contiene las reglas de negocio relacionadas con venues.
 *
 * Las rutas HTTP no deben acceder directamente al repository.
 */
class VenueService(
    private val repository: VenueRepository,
) {

    /**
     * Crea un nuevo venue.
     */
    fun create(
        request: CreateVenueRequest,
    ): VenueResponse {

        require(request.name.isNotBlank()) {
            "Venue name cannot be blank"
        }

        require(request.address.isNotBlank()) {
            "Venue address cannot be blank"
        }

        require(request.city.isNotBlank()) {
            "Venue city cannot be blank"
        }

        val timezone = request.timezone.trim().ifBlank { "America/Monterrey" }
        require(runCatching { ZoneId.of(timezone) }.isSuccess) { "Invalid IANA timezone" }

        return repository.create(
            name = request.name.trim(),
            address = request.address.trim(),
            city = request.city.trim(),
            timezone = timezone,
        )
    }

    /**
     * Obtiene todos los venues.
     */
    fun findAll(): List<VenueResponse> =
        repository.findAll()

    /**
     * Obtiene un venue concreto.
     *
     * Retorna null cuando el venue no existe.
     */
    fun findById(
        venueId: UUID,
    ): VenueResponse? =
        repository.findById(venueId)
}