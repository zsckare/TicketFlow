package com.ticketflow.events.modules.venues

/**
 * Contiene las reglas de negocio relacionadas con venues.
 *
 * Las rutas HTTP no deben acceder directamente al repository.
 */
class VenueService(
    private val repository: VenueRepository,
) {

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

        return repository.create(
            name = request.name.trim(),
            address = request.address.trim(),
            city = request.city.trim(),
        )
    }

    fun findAll(): List<VenueResponse> =
        repository.findAll()
}