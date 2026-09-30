package com.ticketflow.events.modules.seats

import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

/**
 * Acceso a datos relacionado con los asientos físicos
 * de un venue.
 */
class SeatRepository {

    /**
     * Crea un asiento dentro de una sección.
     */
    fun create(
        sectionId: UUID,
        row: String,
        number: String,
    ): SeatResponse = transaction {

        val seatId =
            UUID.randomUUID()

        SeatsTable.insert {
            it[id] = seatId
            it[SeatsTable.sectionId] = sectionId
            it[rowName] = row
            it[seatNumber] = number
        }

        SeatResponse(
            id = seatId.toString(),
            sectionId = sectionId.toString(),
            row = row,
            number = number,
        )
    }

    /**
     * Obtiene todos los asientos pertenecientes
     * a una sección.
     */
    fun findBySection(
        sectionId: UUID,
    ): List<SeatResponse> = transaction {

        SeatsTable
            .selectAll()
            .where {
                SeatsTable.sectionId eq sectionId
            }
            .map(::toResponse)
    }

    /**
     * Busca un asiento por su UUID.
     *
     * Lo necesitaremos posteriormente para que Events Service
     * pueda validar seats utilizados por Tickets Service.
     */
    fun findById(
        id: UUID,
    ): SeatResponse? = transaction {

        SeatsTable
            .selectAll()
            .where {
                SeatsTable.id eq id
            }
            .limit(1)
            .map(::toResponse)
            .singleOrNull()
    }

    private fun toResponse(
        row: ResultRow,
    ): SeatResponse =
        SeatResponse(
            id =
                row[SeatsTable.id]
                    .toString(),

            sectionId =
                row[SeatsTable.sectionId]
                    .toString(),

            row =
                row[SeatsTable.rowName],

            number = row[SeatsTable.seatNumber],
            mapX=row[SeatsTable.mapX]?.toDouble(), mapY=row[SeatsTable.mapY]?.toDouble(),
        )
    fun updateMap(id:UUID,r:UpdateSeatMapRequest):SeatResponse?=transaction{val updated=SeatsTable.update({SeatsTable.id eq id}){it[mapX]=r.x?.toBigDecimal();it[mapY]=r.y?.toBigDecimal()};if(updated==0)null else SeatsTable.selectAll().where{SeatsTable.id eq id}.single().let(::toResponse)}

}