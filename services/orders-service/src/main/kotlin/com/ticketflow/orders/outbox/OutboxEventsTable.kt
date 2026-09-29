package com.ticketflow.orders.outbox

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestampWithTimeZone

object OutboxEventsTable : Table("outbox_events") {
    val id = uuid("id")
    val aggregateType = varchar("aggregate_type", 100)
    val aggregateId = uuid("aggregate_id")
    val eventType = varchar("event_type", 150)
    val topic = varchar("topic", 150)
    val eventKey = varchar("event_key", 200)
    val payload = text("payload")
    val createdAt = timestampWithTimeZone("created_at")
    val publishedAt = timestampWithTimeZone("published_at").nullable()
    val attempts = integer("attempts")
    val lastError = varchar("last_error", 1000).nullable()
    override val primaryKey = PrimaryKey(id)
}
