package com.ticketflow.orders.modules.orders

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestampWithTimeZone

object TicketTransfersTable:Table("ticket_transfers"){
    val id=uuid("id"); val ticketId=uuid("ticket_id").references(IssuedTicketsTable.id); val fromUserId=uuid("from_user_id"); val recipientEmail=varchar("recipient_email",320); val status=varchar("status",20); val transferToken=uuid("transfer_token"); val expiresAt=timestampWithTimeZone("expires_at"); val acceptedByUserId=uuid("accepted_by_user_id").nullable(); val createdAt=timestampWithTimeZone("created_at"); val acceptedAt=timestampWithTimeZone("accepted_at").nullable(); val cancelledAt=timestampWithTimeZone("cancelled_at").nullable(); override val primaryKey=PrimaryKey(id)
}
object TicketOwnershipHistoryTable:Table("ticket_ownership_history"){
    val id=uuid("id");val ticketId=uuid("ticket_id").references(IssuedTicketsTable.id);val fromUserId=uuid("from_user_id").nullable();val toUserId=uuid("to_user_id");val reason=varchar("reason",30);val transferId=uuid("transfer_id").nullable();val createdAt=timestampWithTimeZone("created_at");override val primaryKey=PrimaryKey(id)
}
object PromotionsTable:Table("promotions"){
    val id=uuid("id");val code=varchar("code",64);val description=varchar("description",255).nullable();val discountType=varchar("discount_type",20);val discountValue=decimal("discount_value",12,2);val eventId=uuid("event_id").nullable();val startsAt=timestampWithTimeZone("starts_at").nullable();val endsAt=timestampWithTimeZone("ends_at").nullable();val maxUses=integer("max_uses").nullable();val maxUsesPerUser=integer("max_uses_per_user");val currentUses=integer("current_uses");val active=bool("active");val createdAt=timestampWithTimeZone("created_at");val updatedAt=timestampWithTimeZone("updated_at");override val primaryKey=PrimaryKey(id)
}
object PromotionRedemptionsTable:Table("promotion_redemptions"){
    val id=uuid("id");val promotionId=uuid("promotion_id").references(PromotionsTable.id);val orderId=uuid("order_id").references(OrdersTable.id);val userId=uuid("user_id");val discountAmount=decimal("discount_amount",12,2);val createdAt=timestampWithTimeZone("created_at");override val primaryKey=PrimaryKey(id)
}
