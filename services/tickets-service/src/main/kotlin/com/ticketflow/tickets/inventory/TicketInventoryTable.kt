package com.ticketflow.tickets.modules.inventory

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestampWithTimeZone

/**
 * Tabla propiedad exclusiva de Tickets Service.
 *
 * eventId y seatId hacen referencia conceptualmente a recursos
 * de Events Service, pero NO tienen foreign keys porque pertenecen
 * a otra base de datos/microservicio.
 */
object TicketInventoryTable : Table("ticket_inventory") {

    val id =
        uuid("id")

    val eventId =
        uuid("event_id")

    val sectionId =
        uuid("section_id")
            .nullable()

    val seatId =
        uuid("seat_id")
            .nullable()

    val price =
        decimal(
            name = "price",
            precision = 12,
            scale = 2,
        )

    val priceOverride =
        decimal(
            name = "price_override",
            precision = 12,
            scale = 2,
        ).nullable()

    val currency =
        varchar(
            name = "currency",
            length = 3,
        )

    val status =
        varchar(
            name = "status",
            length = 30,
        )

    /**
     * Identificador de la reservación que actualmente
     * posee temporalmente el ticket.
     */
    val reservationId =
        uuid("reservation_id")
            .nullable()

    /**
     * Momento hasta el cual la reservación es válida.
     */
    val reservedUntil =
        timestampWithTimeZone("reserved_until")
            .nullable()

    val createdAt =
        timestampWithTimeZone("created_at")

    val updatedAt =
        timestampWithTimeZone("updated_at")

    override val primaryKey =
        PrimaryKey(id)
}