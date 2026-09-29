package com.ticketflow.events.modules.venues

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestampWithTimeZone

object SeatsTable : Table("seats") {

    val id = uuid("id")

    val sectionId =
        uuid("section_id")
            .references(VenueSectionsTable.id)

    val rowName =
        varchar("row_name", 20)

    val seatNumber =
        varchar("seat_number", 20)

    val createdAt =
        timestampWithTimeZone("created_at")

    val updatedAt =
        timestampWithTimeZone("updated_at")

    override val primaryKey =
        PrimaryKey(id)
}