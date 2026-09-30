package com.ticketflow.events.modules.events

import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.time.OffsetDateTime
import java.util.UUID
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
/**
 * Encapsula todo el acceso a PostgreSQL relacionado
 * con eventos.
 *
 * Ninguna ruta HTTP debería acceder directamente
 * a EventsTable.
 */
class EventRepository {

    /**
     * Crea un nuevo evento en estado DRAFT.
     */
    fun create(
        venueId: UUID,
        name: String,
        description: String?,
        startsAt: OffsetDateTime,
        endsAt: OffsetDateTime?,
    ): EventResponse = transaction {

        val eventId =
            UUID.randomUUID()

        val status =
            EventStatus.DRAFT

        EventsTable.insert {
            it[id] = eventId
            it[EventsTable.venueId] = venueId
            it[EventsTable.name] = name
            it[EventsTable.description] = description
            it[EventsTable.startsAt] = startsAt
            it[EventsTable.endsAt] = endsAt
            it[EventsTable.status] = status.name
        }

        EventResponse(
            id = eventId.toString(),
            venueId = venueId.toString(),
            name = name,
            description = description,
            startsAt = startsAt.toString(),
            endsAt = endsAt?.toString(),
            status = status,
        )
    }

    /**
     * Devuelve todos los eventos.
     */
    fun findAll(): List<EventResponse> = transaction {

        EventsTable
            .selectAll()
            .map(::toResponse)
    }

    /**
     * Busca un evento por ID.
     */
    fun findById(
        id: UUID,
    ): EventResponse? = transaction {

        EventsTable
            .selectAll()
            .where {
                EventsTable.id eq id
            }
            .limit(1)
            .map(::toResponse)
            .singleOrNull()
    }

    /**
     * Cambia únicamente el estado del evento.
     *
     * Las reglas que determinan si la transición está permitida
     * pertenecen a EventService.
     */
    fun updateStatus(
        id: UUID,
        status: EventStatus,
    ): EventResponse? = transaction {

        val updatedRows =
            EventsTable.update(
                where = {
                    EventsTable.id eq id
                },
            ) {
                it[EventsTable.status] =
                    status.name

                it[updatedAt] =
                    OffsetDateTime.now()
            }

        if (updatedRows == 0) {
            return@transaction null
        }

        EventsTable
            .selectAll()
            .where {
                EventsTable.id eq id
            }
            .limit(1)
            .map(::toResponse)
            .singleOrNull()
    }

    fun update(id: UUID, name: String, description: String?, startsAt: OffsetDateTime, endsAt: OffsetDateTime?): EventResponse? = transaction {
        val count = EventsTable.update({ EventsTable.id eq id }) {
            it[EventsTable.name] = name
            it[EventsTable.description] = description
            it[EventsTable.startsAt] = startsAt
            it[EventsTable.endsAt] = endsAt
            it[updatedAt] = OffsetDateTime.now()
        }
        if (count == 0) null else EventsTable.selectAll().where { EventsTable.id eq id }.single().let(::toResponse)
    }

    fun delete(id: UUID): Boolean = transaction {
        EventsTable.deleteWhere { EventsTable.id eq id } > 0
    }

    /**
     * Convierte una fila SQL al DTO público.
     */
    private fun toResponse(
        row: ResultRow,
    ): EventResponse =
        EventResponse(
            id =
                row[EventsTable.id]
                    .toString(),

            venueId =
                row[EventsTable.venueId]
                    .toString(),

            name =
                row[EventsTable.name],

            description =
                row[EventsTable.description],

            startsAt =
                row[EventsTable.startsAt]
                    .toString(),

            endsAt =
                row[EventsTable.endsAt]
                    ?.toString(),

            status =
                EventStatus.valueOf(
                    row[EventsTable.status],
                ),
        )
}