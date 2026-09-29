package com.ticketflow.tickets.modules.inventory

import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greater
import org.jetbrains.exposed.sql.SqlExpressionBuilder.less
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

/**
 * Acceso a datos del inventario de Tickets Service.
 *
 * Las transiciones críticas de estado se realizan mediante
 * UPDATE condicionales para que PostgreSQL sea quien garantice
 * la atomicidad.
 */
class TicketInventoryRepository {

    /**
     * Crea un registro de inventario inicialmente AVAILABLE.
     */
    fun create(
        eventId: UUID,
        seatId: UUID,
        price: BigDecimal,
        currency: String,
    ): TicketInventoryResponse = transaction {

        val inventoryId =
            UUID.randomUUID()

        TicketInventoryTable.insert {
            it[id] =
                inventoryId

            it[TicketInventoryTable.eventId] =
                eventId

            it[TicketInventoryTable.seatId] =
                seatId

            it[TicketInventoryTable.price] =
                price

            it[TicketInventoryTable.currency] =
                currency

            it[status] =
                TicketInventoryStatus.AVAILABLE.name
        }

        TicketInventoryResponse(
            id = inventoryId.toString(),
            eventId = eventId.toString(),
            seatId = seatId.toString(),
            price = price.toPlainString(),
            currency = currency,
            status = TicketInventoryStatus.AVAILABLE,
            reservationId = null,
            reservedUntil = null,
        )
    }

    /**
     * Busca un registro de inventario.
     */
    fun findById(
        inventoryId: UUID,
    ): TicketInventoryResponse? = transaction {

        findByIdInternal(
            inventoryId,
        )
    }

    /**
     * Obtiene todo el inventario perteneciente a un evento.
     */
    fun findByEvent(
        eventId: UUID,
    ): List<TicketInventoryResponse> = transaction {

        TicketInventoryTable
            .selectAll()
            .where {
                TicketInventoryTable.eventId eq eventId
            }
            .map(::toResponse)
    }

    /**
     * Intenta cambiar atómicamente:
     *
     * AVAILABLE -> RESERVED
     *
     * El WHERE status = AVAILABLE es nuestra protección
     * contra reservaciones concurrentes.
     *
     * Si dos requests intentan reservar el mismo ticket,
     * solamente uno podrá modificar la fila.
     */
    fun reserve(
        inventoryId: UUID,
        reservationId: UUID,
        reservedUntil: OffsetDateTime,
    ): TicketInventoryResponse? = transaction {

        val now =
            OffsetDateTime.now()

        val updatedRows =
            TicketInventoryTable.update(
                where = {
                    (TicketInventoryTable.id eq inventoryId) and
                            (
                                    TicketInventoryTable.status eq
                                            TicketInventoryStatus.AVAILABLE.name
                                    )
                },
            ) {
                it[status] =
                    TicketInventoryStatus.RESERVED.name

                it[TicketInventoryTable.reservationId] =
                    reservationId

                it[TicketInventoryTable.reservedUntil] =
                    reservedUntil

                it[updatedAt] =
                    now
            }

        if (updatedRows == 0) {
            return@transaction null
        }

        findByIdInternal(
            inventoryId,
        )
    }

    /**
     * Libera explícitamente una reservación.
     *
     * Para modificar la fila deben coincidir:
     *
     * - inventoryId
     * - status RESERVED
     * - reservationId
     *
     * Esto impide que un cliente pueda liberar la
     * reservación perteneciente a otro comprador.
     */
    fun release(
        inventoryId: UUID,
        reservationId: UUID,
    ): TicketInventoryResponse? = transaction {

        val now =
            OffsetDateTime.now()

        val updatedRows =
            TicketInventoryTable.update(
                where = {
                    (TicketInventoryTable.id eq inventoryId) and
                            (
                                    TicketInventoryTable.status eq
                                            TicketInventoryStatus.RESERVED.name
                                    ) and
                            (
                                    TicketInventoryTable.reservationId eq
                                            reservationId
                                    )
                },
            ) {
                it[status] =
                    TicketInventoryStatus.AVAILABLE.name

                it[TicketInventoryTable.reservationId] =
                    null

                it[TicketInventoryTable.reservedUntil] =
                    null

                it[updatedAt] =
                    now
            }

        if (updatedRows == 0) {
            return@transaction null
        }

        findByIdInternal(
            inventoryId,
        )
    }

    /**
     * Libera todas las reservaciones que ya expiraron.
     *
     * RESERVED + reservedUntil < now
     *
     * se convierte en:
     *
     * AVAILABLE
     */
    fun releaseExpired(
        now: OffsetDateTime,
    ): Int = transaction {

        TicketInventoryTable.update(
            where = {
                (
                        TicketInventoryTable.status eq
                                TicketInventoryStatus.RESERVED.name
                        ) and
                        (
                                TicketInventoryTable.reservedUntil less
                                        now
                                )
            },
        ) {
            it[status] =
                TicketInventoryStatus.AVAILABLE.name

            it[reservationId] =
                null

            it[reservedUntil] =
                null

            it[updatedAt] =
                now
        }
    }

    /**
     * Confirma una reservación válida.
     *
     * RESERVED -> SOLD
     *
     * Solamente se realiza cuando:
     *
     * - inventoryId coincide
     * - reservationId coincide
     * - status es RESERVED
     * - la reservación todavía no expiró
     */
    fun confirm(
        inventoryId: UUID,
        reservationId: UUID,
        now: OffsetDateTime,
    ): TicketInventoryResponse? = transaction {

        val updatedRows =
            TicketInventoryTable.update(
                where = {
                    (TicketInventoryTable.id eq inventoryId) and
                            (
                                    TicketInventoryTable.status eq
                                            TicketInventoryStatus.RESERVED.name
                                    ) and
                            (
                                    TicketInventoryTable.reservationId eq
                                            reservationId
                                    ) and
                            (
                                    TicketInventoryTable.reservedUntil greater
                                            now
                                    )
                },
            ) {
                it[status] =
                    TicketInventoryStatus.SOLD.name

                /*
                 * Conservamos reservationId temporalmente para
                 * mantener trazabilidad de qué reservación
                 * terminó produciendo la venta.
                 */
                it[TicketInventoryTable.reservedUntil] =
                    null

                it[updatedAt] =
                    now
            }

        if (updatedRows == 0) {
            return@transaction null
        }

        findByIdInternal(
            inventoryId,
        )
    }

    /**
     * Versión interna de findById.
     *
     * No abre una nueva transaction porque se utiliza desde
     * operaciones que ya se encuentran dentro de una.
     */
    private fun findByIdInternal(
        inventoryId: UUID,
    ): TicketInventoryResponse? =
        TicketInventoryTable
            .selectAll()
            .where {
                TicketInventoryTable.id eq inventoryId
            }
            .limit(1)
            .map(::toResponse)
            .singleOrNull()

    /**
     * Convierte una fila de PostgreSQL en el modelo
     * utilizado por nuestra API.
     */
    private fun toResponse(
        row: ResultRow,
    ): TicketInventoryResponse =
        TicketInventoryResponse(
            id =
                row[TicketInventoryTable.id]
                    .toString(),

            eventId =
                row[TicketInventoryTable.eventId]
                    .toString(),

            seatId =
                row[TicketInventoryTable.seatId]
                    .toString(),

            price =
                row[TicketInventoryTable.price]
                    .toPlainString(),

            currency =
                row[TicketInventoryTable.currency],

            status =
                TicketInventoryStatus.valueOf(
                    row[TicketInventoryTable.status],
                ),

            reservationId =
                row[TicketInventoryTable.reservationId]
                    ?.toString(),

            reservedUntil =
                row[TicketInventoryTable.reservedUntil]
                    ?.toString(),
        )
}