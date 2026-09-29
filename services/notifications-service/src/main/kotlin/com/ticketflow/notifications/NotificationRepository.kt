package com.ticketflow.notifications

import com.ticketflow.notifications.kafka.OrderConfirmedEvent
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

class NotificationRepository {
    fun create(request: CreateNotificationRequest): NotificationResponse = transaction {
        insertNotification(null, UUID.fromString(request.userId), request.type, request.destination, request.subject, request.body)
    }

    /** Idempotent event handler: one notification per Kafka eventId. */
    fun createOrderConfirmed(event: OrderConfirmedEvent): NotificationResponse = transaction {
        val eventId = UUID.fromString(event.eventId)
        findByEventId(eventId) ?: insertNotification(
            eventId, UUID.fromString(event.userId), "ORDER_CONFIRMED", event.userEmail,
            "Order confirmed", "Your order ${event.orderId} for ${event.amount} ${event.currency} has been confirmed."
        )
    }

    fun find(id: UUID): NotificationResponse? = transaction { findInternal(id) }
    fun byUser(id: UUID): List<NotificationResponse> = transaction {
        NotificationsTable.selectAll().where { NotificationsTable.userId eq id }
            .orderBy(NotificationsTable.createdAt to SortOrder.DESC).map(::toResponse)
    }

    private fun insertNotification(eventId: UUID?, userId: UUID, type: String, destination: String, subject: String, body: String): NotificationResponse {
        val id = UUID.randomUUID()
        NotificationsTable.insert {
            it[NotificationsTable.id] = id; it[NotificationsTable.eventId] = eventId; it[NotificationsTable.userId] = userId
            it[NotificationsTable.type] = type; it[NotificationsTable.destination] = destination; it[NotificationsTable.subject] = subject
            it[NotificationsTable.body] = body; it[status] = NotificationStatus.SENT.name; it[createdAt] = OffsetDateTime.now(ZoneOffset.UTC)
        }
        return findInternal(id)!!
    }
    private fun findInternal(id: UUID) = NotificationsTable.selectAll().where { NotificationsTable.id eq id }.singleOrNull()?.let(::toResponse)
    private fun findByEventId(eventId: UUID) = NotificationsTable.selectAll().where { NotificationsTable.eventId eq eventId }.singleOrNull()?.let(::toResponse)
    private fun toResponse(row: ResultRow) = NotificationResponse(row[NotificationsTable.id].toString(), row[NotificationsTable.userId].toString(), row[NotificationsTable.type], row[NotificationsTable.destination], row[NotificationsTable.subject], row[NotificationsTable.body], NotificationStatus.valueOf(row[NotificationsTable.status]), row[NotificationsTable.createdAt].toString())
}
