package com.ticketflow.events.modules.venues

import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

/**
 * Encapsula el acceso a datos relacionado con venues.
 */
class VenueRepository {

    fun create(
        name: String,
        address: String,
        city: String,
        timezone: String,
    ): VenueResponse = transaction {

        val venueId = UUID.randomUUID()

        VenuesTable.insert {
            it[id] = venueId
            it[VenuesTable.name] = name
            it[VenuesTable.address] = address
            it[VenuesTable.city] = city
            it[VenuesTable.timezone] = timezone
        }

        VenueResponse(
            id = venueId.toString(),
            name = name,
            address = address,
            city = city,
            timezone = timezone,
        )
    }

    /**
     * Obtiene todos los venues.
     */
    fun findAll(): List<VenueResponse> = transaction {

        VenuesTable
            .selectAll()
            .map(::toVenueResponse)
    }

    /**
     * Obtiene un venue por su ID.
     *
     * Retorna null cuando el venue no existe.
     */
    fun findById(
        id: UUID,
    ): VenueResponse? = transaction {

        VenuesTable
            .selectAll()
            .where {
                VenuesTable.id eq id
            }
            .singleOrNull()
            ?.let(::toVenueResponse)
    }

    /**
     * Comprueba si existe un venue con el ID indicado.
     *
     * Se utiliza desde otros componentes del Events Service
     * para validar referencias a venues.
     */
    fun exists(
        id: UUID,
    ): Boolean = transaction {

        VenuesTable
            .selectAll()
            .where {
                VenuesTable.id eq id
            }
            .limit(1)
            .any()
    }

    private fun toVenueResponse(
        row: ResultRow,
    ): VenueResponse =
        VenueResponse(
            id = row[VenuesTable.id].toString(),
            name = row[VenuesTable.name],
            address = row[VenuesTable.address],
            city = row[VenuesTable.city],
            timezone = row[VenuesTable.timezone],
        )
}