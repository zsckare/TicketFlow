package com.ticketflow.events.modules.venues

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestampWithTimeZone


/**
 * Representación de la tabla "venues" para Exposed.
 *
 * Importante:
 * Exposed describe la tabla, pero NO la crea.
 * La creación del schema pertenece exclusivamente a Flyway.
 */
object VenuesTable : Table("venues") {

    val id = uuid("id")

    val name = varchar(
        name = "name",
        length = 200,
    )

    val address = varchar(
        name = "address",
        length = 300,
    )

    val city = varchar(
        name = "city",
        length = 150,
    )

    val timezone = varchar(
        name = "timezone",
        length = 80,
    )

    val createdAt =
        timestampWithTimeZone("created_at")

    val updatedAt =
        timestampWithTimeZone("updated_at")

    override val primaryKey =
        PrimaryKey(id)
}