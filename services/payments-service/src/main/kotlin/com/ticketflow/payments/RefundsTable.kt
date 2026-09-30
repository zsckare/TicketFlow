package com.ticketflow.payments

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestampWithTimeZone

object RefundsTable : Table("payment_refunds") {
    val id = uuid("id");
    val paymentId = uuid("payment_id").references(PaymentsTable.id);
    val amount = decimal("amount", 12, 2);
    val reason = varchar("reason", 255).nullable();
    val idempotencyKey = varchar("idempotency_key", 100);
    val createdAt = timestampWithTimeZone("created_at");
    override val primaryKey = PrimaryKey(id)
}
