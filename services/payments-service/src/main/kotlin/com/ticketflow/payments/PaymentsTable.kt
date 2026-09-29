package com.ticketflow.payments
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestampWithTimeZone
object PaymentsTable:Table("payments"){val id=uuid("id");val orderId=uuid("order_id");val userId=uuid("user_id");val amount=decimal("amount",12,2);val currency=varchar("currency",3);val status=varchar("status",30);val idempotencyKey=varchar("idempotency_key",100).uniqueIndex();val createdAt=timestampWithTimeZone("created_at");val updatedAt=timestampWithTimeZone("updated_at");override val primaryKey=PrimaryKey(id)}