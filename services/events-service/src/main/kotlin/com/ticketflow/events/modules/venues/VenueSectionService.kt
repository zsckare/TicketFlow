package com.ticketflow.events.modules.venues

import java.util.UUID

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
            venueId,
            request.copy(
                name = request.name.trim(),
            ),
        )
    }

    fun findByVenue(
        venueId: UUID,
    ): List<VenueSectionResponse> =
        repository.findByVenue(venueId)
}