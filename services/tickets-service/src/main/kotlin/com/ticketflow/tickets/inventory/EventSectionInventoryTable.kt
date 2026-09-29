package com.ticketflow.tickets.modules.inventory

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestampWithTimeZone

/** Commercial configuration for one venue section in one event. */
object EventSectionInventoryTable : Table("event_section_inventory") {
    val eventId = uuid("event_id")
    val sectionId = uuid("section_id")
    val sectionType = varchar("section_type", 30)
    val basePrice = decimal("base_price", 12, 2)
    val currency = varchar("currency", 3)
    val capacity = integer("capacity")
    val createdAt = timestampWithTimeZone("created_at")
    val updatedAt = timestampWithTimeZone("updated_at")
    override val primaryKey = PrimaryKey(eventId, sectionId)
}
