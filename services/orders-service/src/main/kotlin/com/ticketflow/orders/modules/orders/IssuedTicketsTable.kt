package com.ticketflow.orders.modules.orders
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestampWithTimeZone
object IssuedTicketsTable:Table("issued_tickets"){
 val id=uuid("id");val orderId=uuid("order_id").references(OrdersTable.id);val orderItemId=uuid("order_item_id").references(OrderItemsTable.id)
 val userId=uuid("user_id");val eventId=uuid("event_id");val inventoryId=uuid("inventory_id");val sectionId=uuid("section_id").nullable();val seatId=uuid("seat_id").nullable()
 val admissionToken=uuid("admission_token");val status=varchar("status",20);val issuedAt=timestampWithTimeZone("issued_at");val checkedInAt=timestampWithTimeZone("checked_in_at").nullable();override val primaryKey=PrimaryKey(id)
}
