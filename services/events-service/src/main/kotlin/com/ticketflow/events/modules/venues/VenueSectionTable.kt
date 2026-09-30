package com.ticketflow.events.modules.sections

import com.ticketflow.events.modules.venues.VenuesTable
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestampWithTimeZone

/**
 * Representación Exposed de venue_sections.
 *
 * Flyway continúa siendo el responsable de crear
 * físicamente la tabla.
 */
object VenueSectionsTable : Table("venue_sections") {

    val id =
        uuid("id")

    val venueId =
        uuid("venue_id")
            .references(VenuesTable.id)

    val name =
        varchar(
            name = "name",
            length = 150,
        )

    val type =
        varchar(
            name = "type",
            length = 30,
        )

    val capacity =
        integer("capacity")

    val mapX = decimal("map_x",10,2).nullable()
    val mapY = decimal("map_y",10,2).nullable()
    val mapWidth = decimal("map_width",10,2).nullable()
    val mapHeight = decimal("map_height",10,2).nullable()
    val mapRotation = decimal("map_rotation",10,2)

    val createdAt =
        timestampWithTimeZone("created_at")

    val updatedAt =
        timestampWithTimeZone("updated_at")

    override val primaryKey =
        PrimaryKey(id)
}