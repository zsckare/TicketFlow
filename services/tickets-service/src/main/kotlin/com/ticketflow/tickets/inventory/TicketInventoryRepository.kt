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

    fun deleteEventInventory(eventId: UUID): Int = transaction {
        val inventory = TicketInventoryTable.selectAll().where { TicketInventoryTable.eventId eq eventId }.toList()
        require(inventory.all { it[TicketInventoryTable.status] == TicketInventoryStatus.AVAILABLE.name }) {
            "Event inventory has reserved or sold tickets"
        }
        val deleted = TicketInventoryTable.deleteWhere { TicketInventoryTable.eventId eq eventId }
        EventSectionInventoryTable.deleteWhere { EventSectionInventoryTable.eventId eq eventId }
        deleted
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

    fun restockSold(inventoryId: UUID): TicketInventoryResponse? = transaction {
        val updated = TicketInventoryTable.update({
            (TicketInventoryTable.id eq inventoryId) and
                (TicketInventoryTable.status eq TicketInventoryStatus.SOLD.name)
        }) {
            it[status] = TicketInventoryStatus.AVAILABLE.name
            it[reservationId] = null
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
    fun createPricingTier(eventId:UUID, request:CreatePricingTierRequest):PricingTierResponse=transaction{
        val id=UUID.randomUUID();val now=OffsetDateTime.now();PricingTierTable.insert{it[PricingTierTable.id]=id;it[PricingTierTable.eventId]=eventId;it[sectionId]=UUID.fromString(request.sectionId);it[name]=request.name.trim();it[price]=request.price.toBigDecimal();it[currency]=request.currency.uppercase();it[salesStartAt]=request.salesStartAt?.let(OffsetDateTime::parse);it[salesEndAt]=request.salesEndAt?.let(OffsetDateTime::parse);it[priority]=request.priority;it[active]=request.active;it[createdAt]=now;it[updatedAt]=now};findPricingTierInternal(id)!!
    }
    fun findPricingTiers(eventId: UUID): List<PricingTierResponse> = transaction {PricingTierTable.selectAll().where{PricingTierTable.eventId eq eventId}.orderBy(PricingTierTable.priority to SortOrder.DESC).map(::toPricingTier)}
    fun deletePricingTier(id:UUID):Boolean=transaction{PricingTierTable.deleteWhere{PricingTierTable.id eq id}>0}
    fun effectivePrice(eventId:UUID,sectionId:UUID,base:BigDecimal,now:OffsetDateTime=OffsetDateTime.now()):BigDecimal=transaction{
        PricingTierTable.selectAll().where{(PricingTierTable.eventId eq eventId) and (PricingTierTable.sectionId eq sectionId) and (PricingTierTable.active eq true)}.orderBy(PricingTierTable.priority to SortOrder.DESC).map(::toPricingTier).firstOrNull{(it.salesStartAt==null||!OffsetDateTime.parse(it.salesStartAt).isAfter(now))&&(it.salesEndAt==null||OffsetDateTime.parse(it.salesEndAt).isAfter(now))}?.price?.toBigDecimal()?:base
    }
    fun applyEffectivePrice(inventoryId:UUID):TicketInventoryResponse?=transaction{
        val row=TicketInventoryTable.selectAll().where{TicketInventoryTable.id eq inventoryId}.singleOrNull()?:return@transaction null;val override=row[TicketInventoryTable.priceOverride];if(override!=null)return@transaction toResponse(row);val section=row[TicketInventoryTable.sectionId]?:return@transaction toResponse(row);val base=EventSectionInventoryTable.selectAll().where{(EventSectionInventoryTable.eventId eq row[TicketInventoryTable.eventId]) and (EventSectionInventoryTable.sectionId eq section)}.singleOrNull()?.get(EventSectionInventoryTable.basePrice)?:row[TicketInventoryTable.price];val effective=effectivePrice(row[TicketInventoryTable.eventId],section,base);if(effective!=row[TicketInventoryTable.price])TicketInventoryTable.update({TicketInventoryTable.id eq inventoryId}){it[price]=effective;it[updatedAt]=OffsetDateTime.now()};findByIdInternal(inventoryId)
    }
    private fun findPricingTierInternal(id:UUID)=PricingTierTable.selectAll().where{PricingTierTable.id eq id}.singleOrNull()?.let(::toPricingTier)
    private fun toPricingTier(r:ResultRow)=PricingTierResponse(r[PricingTierTable.id].toString(),r[PricingTierTable.eventId].toString(),r[PricingTierTable.sectionId].toString(),r[PricingTierTable.name],r[PricingTierTable.price].toPlainString(),r[PricingTierTable.currency],r[PricingTierTable.salesStartAt]?.toString(),r[PricingTierTable.salesEndAt]?.toString(),r[PricingTierTable.priority],r[PricingTierTable.active])

    fun hasPricingTiers(eventId:UUID,sectionId:UUID):Boolean=transaction{PricingTierTable.selectAll().where{(PricingTierTable.eventId eq eventId) and (PricingTierTable.sectionId eq sectionId) and (PricingTierTable.active eq true)}.any()}
    fun hasActivePricingTier(eventId:UUID,sectionId:UUID,now:OffsetDateTime=OffsetDateTime.now()):Boolean=transaction{PricingTierTable.selectAll().where{(PricingTierTable.eventId eq eventId) and (PricingTierTable.sectionId eq sectionId) and (PricingTierTable.active eq true)}.map(::toPricingTier).any{(it.salesStartAt==null||!OffsetDateTime.parse(it.salesStartAt).isAfter(now))&&(it.salesEndAt==null||OffsetDateTime.parse(it.salesEndAt).isAfter(now))}}

}
