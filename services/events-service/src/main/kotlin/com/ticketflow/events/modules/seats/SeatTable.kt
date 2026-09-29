package com.ticketflow.events.modules.seats

import com.ticketflow.events.modules.sections.VenueSectionsTable
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestampWithTimeZone

/**
 * Representación Exposed de la tabla seats.
 *
 * Seat representa un asiento físico del venue.
 * NO representa un ticket ni una reservación.
 */
object SeatsTable : Table("seats") {

    val id =
        uuid("id")

    val sectionId =
        uuid("section_id")
            .references(VenueSectionsTable.id)

    val rowName =
        varchar(
            name = "row_name",
            length = 20,
        )

    val seatNumber =
        varchar(
            name = "seat_number",
            length = 20,
        )

    val createdAt =
        timestampWithTimeZone("created_at")

    val updatedAt =
        timestampWithTimeZone("updated_at")

    override val primaryKey =
        PrimaryKey(id)
}