package com.ticketflow.events.modules.sections

import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

/**
 * Acceso a datos de las secciones de un venue.
 */
class VenueSectionRepository {

    fun create(
        venueId: UUID,
        request: CreateVenueSectionRequest,
    ): VenueSectionResponse = transaction {

        val sectionId =
            UUID.randomUUID()

        VenueSectionsTable.insert {
            it[id] = sectionId
            it[VenueSectionsTable.venueId] = venueId
            it[VenueSectionsTable.name] = request.name
            it[VenueSectionsTable.type] = request.type.name
            it[VenueSectionsTable.capacity] = request.capacity
        }

        VenueSectionResponse(
            id = sectionId.toString(),
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

    /**
     * Lo necesitaremos posteriormente desde SeatService.
     */
    fun findById(
        id: UUID,
    ): VenueSectionResponse? = transaction {

        VenueSectionsTable
            .selectAll()
            .where {
                VenueSectionsTable.id eq id
            }
            .limit(1)
            .map(::toResponse)
            .singleOrNull()
    }

    private fun toResponse(
        row: ResultRow,
    ): VenueSectionResponse =
        VenueSectionResponse(
            id =
                row[VenueSectionsTable.id]
                    .toString(),

            venueId =
                row[VenueSectionsTable.venueId]
                    .toString(),

            name =
                row[VenueSectionsTable.name],

            type =
                VenueSectionType.valueOf(
                    row[VenueSectionsTable.type],
                ),

            capacity =
                row[VenueSectionsTable.capacity],
        )
}