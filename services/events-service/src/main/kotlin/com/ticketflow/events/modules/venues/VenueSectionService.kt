package com.ticketflow.events.modules.sections

import com.ticketflow.events.modules.venues.VenueRepository
import java.util.UUID

/**
 * Reglas de negocio relacionadas con las secciones
 * físicas de un venue.
 */
class VenueSectionService(
    private val venueRepository: VenueRepository,
    private val repository: VenueSectionRepository,
) {

    fun create(
        venueId: UUID,
        request: CreateVenueSectionRequest,
    ): VenueSectionResponse {

        require(
            venueRepository.exists(venueId),
        ) {
            "Venue does not exist"
        }

        require(
            request.name.isNotBlank(),
        ) {
            "Section name cannot be blank"
        }

        require(
            request.capacity > 0,
        ) {
            "Section capacity must be greater than zero"
        }

        return repository.create(
            venueId = venueId,
            request =
                request.copy(
                    name = request.name.trim(),
                ),
        )
    }

    fun findByVenue(
        venueId: UUID,
    ): List<VenueSectionResponse> {

        require(
            venueRepository.exists(venueId),
        ) {
            "Venue does not exist"
        }

        return repository.findByVenue(
            venueId,
        )
    }

    fun findById(
        id: UUID,
    ): VenueSectionResponse? =
        repository.findById(id)
    fun updateMap(id:UUID,request:UpdateSectionMapRequest)=repository.updateMap(id,request)

}