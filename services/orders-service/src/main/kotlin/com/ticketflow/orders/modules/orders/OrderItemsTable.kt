package com.ticketflow.orders.modules.orders

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestampWithTimeZone

object OrderItemsTable : Table("order_items") {
    val id = uuid("id")
    val orderId = uuid("order_id").references(OrdersTable.id)
    val inventoryId = uuid("inventory_id")
    val reservationId = uuid("reservation_id").nullable()
    val eventId = uuid("event_id")
    val sectionId = uuid("section_id").nullable()
    val seatId = uuid("seat_id").nullable()
    val unitPrice = decimal("unit_price", 12, 2)
    val currency = varchar("currency", 3)
    val createdAt = timestampWithTimeZone("created_at")
    override val primaryKey = PrimaryKey(id)
}
