package com.ticketflow.tickets.modules.inventory

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greater
import org.jetbrains.exposed.sql.SqlExpressionBuilder.less
import org.jetbrains.exposed.sql.transactions.transaction
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

class TicketInventoryRepository {

    fun create(eventId: UUID, seatId: UUID, price: BigDecimal, currency: String): TicketInventoryResponse = transaction {
        val id = UUID.randomUUID()
        TicketInventoryTable.insert {
            it[TicketInventoryTable.id] = id
            it[TicketInventoryTable.eventId] = eventId
            it[TicketInventoryTable.seatId] = seatId
            it[TicketInventoryTable.price] = price
            it[TicketInventoryTable.currency] = currency
            it[status] = TicketInventoryStatus.AVAILABLE.name
        }
        findByIdInternal(id)!!
    }

    /** Replaces/configures a section while preserving already reserved/sold units. */
    fun configureSection(
        eventId: UUID,
        sectionId: UUID,
        type: InventorySectionType,
        basePrice: BigDecimal,
        currency: String,
        capacity: Int,
        seatIds: List<UUID>,
    ): EventSectionInventoryResponse = transaction {
        val now = OffsetDateTime.now()
        val existing = EventSectionInventoryTable.selectAll().where {
            (EventSectionInventoryTable.eventId eq eventId) and (EventSectionInventoryTable.sectionId eq sectionId)
        }.singleOrNull()

        if (existing == null) {
            EventSectionInventoryTable.insert {
                it[EventSectionInventoryTable.eventId] = eventId
                it[EventSectionInventoryTable.sectionId] = sectionId
                it[sectionType] = type.name
                it[EventSectionInventoryTable.basePrice] = basePrice
                it[EventSectionInventoryTable.currency] = currency
                it[EventSectionInventoryTable.capacity] = capacity
                it[createdAt] = now
                it[updatedAt] = now
            }
        } else {
            EventSectionInventoryTable.update({
                (EventSectionInventoryTable.eventId eq eventId) and (EventSectionInventoryTable.sectionId eq sectionId)
            }) {
                it[sectionType] = type.name
                it[EventSectionInventoryTable.basePrice] = basePrice
                it[EventSectionInventoryTable.currency] = currency
                it[EventSectionInventoryTable.capacity] = capacity
                it[updatedAt] = now
            }
        }

        // Base-price changes affect only units without an individual override.
        TicketInventoryTable.update({
            (TicketInventoryTable.eventId eq eventId) and
                (TicketInventoryTable.sectionId eq sectionId) and
                TicketInventoryTable.priceOverride.isNull()
        }) {
            it[price] = basePrice
            it[TicketInventoryTable.currency] = currency
            it[updatedAt] = now
        }

        if (type == InventorySectionType.RESERVED_SEATING) {
            val existingSeats = TicketInventoryTable.selectAll().where {
                (TicketInventoryTable.eventId eq eventId) and (TicketInventoryTable.sectionId eq sectionId)
            }.mapNotNull { it[TicketInventoryTable.seatId] }.toSet()
            seatIds.filterNot(existingSeats::contains).forEach { seatId ->
                TicketInventoryTable.insert {
                    it[id] = UUID.randomUUID()
                    it[TicketInventoryTable.eventId] = eventId
                    it[TicketInventoryTable.sectionId] = sectionId
                    it[TicketInventoryTable.seatId] = seatId
                    it[price] = basePrice
                    it[TicketInventoryTable.currency] = currency
                    it[status] = TicketInventoryStatus.AVAILABLE.name
                    it[createdAt] = now
                    it[updatedAt] = now
                }
            }
        } else {
            val current = TicketInventoryTable.selectAll().where {
                (TicketInventoryTable.eventId eq eventId) and (TicketInventoryTable.sectionId eq sectionId)
            }.count().toInt()
            repeat((capacity - current).coerceAtLeast(0)) {
                TicketInventoryTable.insert {
                    it[id] = UUID.randomUUID()
                    it[TicketInventoryTable.eventId] = eventId
                    it[TicketInventoryTable.sectionId] = sectionId
                    it[TicketInventoryTable.seatId] = null
                    it[price] = basePrice
                    it[TicketInventoryTable.currency] = currency
                    it[status] = TicketInventoryStatus.AVAILABLE.name
                    it[createdAt] = now
                    it[updatedAt] = now
                }
            }
            // Reducing GA capacity is safe only for AVAILABLE units.
            val excess = (current - capacity).coerceAtLeast(0)

if (excess > 0) {
    val removable = TicketInventoryTable
        .selectAll()
        .where {
            (TicketInventoryTable.eventId eq eventId) and
                (TicketInventoryTable.sectionId eq sectionId) and
                (TicketInventoryTable.status eq TicketInventoryStatus.AVAILABLE.name)
        }
        .limit(excess)
        .map {
            it[TicketInventoryTable.id]
        }

    if (removable.size != excess) {
        error(
            "Cannot reduce quantity below reserved or sold inventory",
        )
    }

    /**
     * Eliminamos únicamente unidades AVAILABLE.
     *
     * Se hace individualmente para mantener compatibilidad
     * con la versión actual de Exposed y evitar depender de
     * una expresión SQL IN para UUIDs.
     */
    removable.forEach { inventoryId ->
        TicketInventoryTable.deleteWhere {
            TicketInventoryTable.id eq inventoryId
        }
    }
}
        }

        EventSectionInventoryResponse(eventId.toString(), sectionId.toString(), type, basePrice.toPlainString(), currency, capacity)
    }

    fun findSectionConfigs(eventId: UUID): List<EventSectionInventoryResponse> = transaction {
        EventSectionInventoryTable.selectAll().where { EventSectionInventoryTable.eventId eq eventId }.map {
            EventSectionInventoryResponse(
                it[EventSectionInventoryTable.eventId].toString(),
                it[EventSectionInventoryTable.sectionId].toString(),
                InventorySectionType.valueOf(it[EventSectionInventoryTable.sectionType]),
                it[EventSectionInventoryTable.basePrice].toPlainString(),
                it[EventSectionInventoryTable.currency],
                it[EventSectionInventoryTable.capacity],
            )
        }
    }

    fun updateSeatOverride(eventId: UUID, seatId: UUID, override: BigDecimal?): TicketInventoryResponse? = transaction {
        val row = TicketInventoryTable.selectAll().where {
            (TicketInventoryTable.eventId eq eventId) and (TicketInventoryTable.seatId eq seatId)
        }.singleOrNull() ?: return@transaction null
        val sectionId = row[TicketInventoryTable.sectionId] ?: return@transaction null
        val config = EventSectionInventoryTable.selectAll().where {
            (EventSectionInventoryTable.eventId eq eventId) and (EventSectionInventoryTable.sectionId eq sectionId)
        }.single()
        val effective = override ?: config[EventSectionInventoryTable.basePrice]
        TicketInventoryTable.update({ TicketInventoryTable.id eq row[TicketInventoryTable.id] }) {
            it[priceOverride] = override
            it[price] = effective
            it[updatedAt] = OffsetDateTime.now()
        }
        findByIdInternal(row[TicketInventoryTable.id])
    }

    fun findById(inventoryId: UUID): TicketInventoryResponse? = transaction { findByIdInternal(inventoryId) }
    fun findByEvent(eventId: UUID): List<TicketInventoryResponse> = transaction {
        TicketInventoryTable.selectAll().where { TicketInventoryTable.eventId eq eventId }.map(::toResponse)
    }

    fun reserve(inventoryId: UUID, reservationId: UUID, reservedUntil: OffsetDateTime): TicketInventoryResponse? = transaction {
        val now = OffsetDateTime.now()
        val updated = TicketInventoryTable.update({
            (TicketInventoryTable.id eq inventoryId) and (TicketInventoryTable.status eq TicketInventoryStatus.AVAILABLE.name)
        }) {
            it[status] = TicketInventoryStatus.RESERVED.name
            it[TicketInventoryTable.reservationId] = reservationId
            it[TicketInventoryTable.reservedUntil] = reservedUntil
            it[updatedAt] = now
        }
        if (updated == 0) null else findByIdInternal(inventoryId)
    }

    fun release(inventoryId: UUID, reservationId: UUID): TicketInventoryResponse? = transaction {
        val updated = TicketInventoryTable.update({
            (TicketInventoryTable.id eq inventoryId) and
                (TicketInventoryTable.status eq TicketInventoryStatus.RESERVED.name) and
                (TicketInventoryTable.reservationId eq reservationId)
        }) {
            it[status] = TicketInventoryStatus.AVAILABLE.name
            it[TicketInventoryTable.reservationId] = null
            it[reservedUntil] = null
            it[updatedAt] = OffsetDateTime.now()
        }
        if (updated == 0) null else findByIdInternal(inventoryId)
    }

    fun releaseExpired(now: OffsetDateTime): Int = transaction {
        TicketInventoryTable.update({
            (TicketInventoryTable.status eq TicketInventoryStatus.RESERVED.name) and
                (TicketInventoryTable.reservedUntil less now)
        }) {
            it[status] = TicketInventoryStatus.AVAILABLE.name
            it[reservationId] = null
            it[reservedUntil] = null
            it[updatedAt] = now
        }
    }

    fun confirm(inventoryId: UUID, reservationId: UUID, now: OffsetDateTime): TicketInventoryResponse? = transaction {
        val updated = TicketInventoryTable.update({
            (TicketInventoryTable.id eq inventoryId) and
                (TicketInventoryTable.status eq TicketInventoryStatus.RESERVED.name) and
                (TicketInventoryTable.reservationId eq reservationId) and
                (TicketInventoryTable.reservedUntil greater now)
        }) {
            it[status] = TicketInventoryStatus.SOLD.name
            it[reservedUntil] = null
            it[updatedAt] = now
        }
        if (updated == 0) null else findByIdInternal(inventoryId)
    }

    private fun findByIdInternal(inventoryId: UUID): TicketInventoryResponse? =
        TicketInventoryTable.selectAll().where { TicketInventoryTable.id eq inventoryId }.limit(1).map(::toResponse).singleOrNull()

    private fun toResponse(row: ResultRow) = TicketInventoryResponse(
        id = row[TicketInventoryTable.id].toString(),
        eventId = row[TicketInventoryTable.eventId].toString(),
        sectionId = row[TicketInventoryTable.sectionId]?.toString(),
        seatId = row[TicketInventoryTable.seatId]?.toString(),
        price = row[TicketInventoryTable.price].toPlainString(),
        priceOverride = row[TicketInventoryTable.priceOverride]?.toPlainString(),
        currency = row[TicketInventoryTable.currency],
        status = TicketInventoryStatus.valueOf(row[TicketInventoryTable.status]),
        reservationId = row[TicketInventoryTable.reservationId]?.toString(),
        reservedUntil = row[TicketInventoryTable.reservedUntil]?.toString(),
    )
}
