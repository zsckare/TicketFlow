package com.ticketflow.events.modules.events

import com.ticketflow.events.modules.venues.VenuesTable
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestampWithTimeZone

/**
 * Representación Exposed de la tabla events.
 *
 * Flyway continúa siendo el responsable de crear/modificar
 * físicamente la tabla en PostgreSQL.
 */
object EventsTable : Table("events") {

    val id =
        uuid("id")

    val venueId =
        uuid("venue_id")
            .references(VenuesTable.id)

    val name =
        varchar(
            name = "name",
            length = 200,
        )

    val description =
        text("description")
            .nullable()

    val startsAt =
        timestampWithTimeZone("starts_at")

    val endsAt =
        timestampWithTimeZone("ends_at")
            .nullable()

    val status =
        varchar(
            name = "status",
            length = 30,
        )

    val createdAt =
        timestampWithTimeZone("created_at")

    val updatedAt =
        timestampWithTimeZone("updated_at")

    override val primaryKey =
        PrimaryKey(id)
}