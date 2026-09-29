package com.ticketflow.events.modules.venues

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestampWithTimeZone

object VenueSectionsTable : Table("venue_sections") {

    val id = uuid("id")

    val venueId =
        uuid("venue_id")
            .references(VenuesTable.id)

    val name =
        varchar("name", 150)

    val type =
        varchar("type", 30)

    val capacity =
        integer("capacity")

    val createdAt =
        timestampWithTimeZone("created_at")

    val updatedAt =
        timestampWithTimeZone("updated_at")

    override val primaryKey =
        PrimaryKey(id)
}