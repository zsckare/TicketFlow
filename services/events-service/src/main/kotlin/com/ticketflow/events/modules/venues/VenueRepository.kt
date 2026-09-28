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
    ): VenueResponse = transaction {

        val venueId = UUID.randomUUID()

        VenuesTable.insert {
            it[id] = venueId
            it[VenuesTable.name] = name
            it[VenuesTable.address] = address
            it[VenuesTable.city] = city
        }

        VenueResponse(
            id = venueId.toString(),
            name = name,
            address = address,
            city = city,
        )
    }

    fun findAll(): List<VenueResponse> = transaction {

        VenuesTable
            .selectAll()
            .map(::toVenueResponse)
    }

    private fun toVenueResponse(
        row: ResultRow,
    ): VenueResponse =
        VenueResponse(
            id = row[VenuesTable.id].toString(),
            name = row[VenuesTable.name],
            address = row[VenuesTable.address],
            city = row[VenuesTable.city],
        )
}