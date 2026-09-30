package com.ticketflow.payments
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestampWithTimeZone
object PaymentsTable:Table("payments"){
 val id=uuid("id");val orderId=uuid("order_id");val userId=uuid("user_id");val amount=decimal("amount",12,2);val currency=varchar("currency",3);val status=varchar("status",30);val idempotencyKey=varchar("idempotency_key",100).uniqueIndex();val provider=varchar("provider",30);val providerSessionId=varchar("provider_session_id",255).nullable();val checkoutUrl=text("checkout_url").nullable();val customerEmail=varchar("customer_email",320).nullable();val createdAt=timestampWithTimeZone("created_at");val updatedAt=timestampWithTimeZone("updated_at");override val primaryKey=PrimaryKey(id)
}
