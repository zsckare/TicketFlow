package com.ticketflow.events.modules.venues

import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

class VenueSectionRepository {

    fun create(
        venueId: UUID,
        request: CreateVenueSectionRequest,
    ): VenueSectionResponse = transaction {

        val id = UUID.randomUUID()

        VenueSectionsTable.insert {
            it[VenueSectionsTable.id] = id
            it[VenueSectionsTable.venueId] = venueId
            it[name] = request.name
            it[type] = request.type.name
            it[capacity] = request.capacity
        }

        VenueSectionResponse(
            id = id.toString(),
            venueId = venueId.toString(),
            name = request.name,
            type = request.type,
            capacity = request.capacity,
        )
    }

    fun findByVenue(
        venueId: UUID,
    ): List<VenueSectionResponse> = transaction {

        VenueSectionsTable
            .selectAll()
            .where {
                VenueSectionsTable.venueId eq venueId
            }
            .map(::toResponse)
    }

    private fun toResponse(
        row: ResultRow,
    ) = VenueSectionResponse(
        id = row[VenueSectionsTable.id].toString(),
        venueId = row[VenueSectionsTable.venueId].toString(),
        name = row[VenueSectionsTable.name],
        type = VenueSectionType.valueOf(
            row[VenueSectionsTable.type],
        ),
        capacity = row[VenueSectionsTable.capacity],
    )
}