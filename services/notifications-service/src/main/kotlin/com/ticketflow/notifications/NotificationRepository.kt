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

    fun findOrderConfirmed(eventId: UUID): NotificationResponse? = transaction { findByEventId(eventId) }

    /** Creates the durable notification record before SMTP is attempted. */
    fun prepareOrderConfirmed(event: OrderConfirmedEvent, subject: String, body: String): NotificationResponse = transaction {
        val eventId = UUID.fromString(event.eventId)
        findByEventId(eventId) ?: insertNotification(
            eventId = eventId,
            userId = UUID.fromString(event.userId),
            type = "ORDER_CONFIRMED",
            destination = event.userEmail,
            subject = subject,
            body = body,
            deliveryStatus = NotificationStatus.PENDING,
        )
    }

    fun updateStatusForEvent(eventId: UUID, status: NotificationStatus): NotificationResponse? = transaction {
        NotificationsTable.update({ NotificationsTable.eventId eq eventId }) {
            it[NotificationsTable.status] = status.name
        }
        findByEventId(eventId)
    }

    fun find(id: UUID): NotificationResponse? = transaction { findInternal(id) }

    fun statusCounts(): Map<NotificationStatus, Int> = transaction {
        val rows = NotificationsTable.selectAll().toList()
        NotificationStatus.entries.associateWith { status -> rows.count { it[NotificationsTable.status] == status.name } }
    }

    fun byUser(id: UUID): List<NotificationResponse> = transaction {
        NotificationsTable.selectAll().where { NotificationsTable.userId eq id }
            .orderBy(NotificationsTable.createdAt to SortOrder.DESC).map(::toResponse)
    }

    private fun insertNotification(
        eventId: UUID?,
        userId: UUID,
        type: String,
        destination: String,
        subject: String,
        body: String,
        deliveryStatus: NotificationStatus = NotificationStatus.SENT,
    ): NotificationResponse {
        val id = UUID.randomUUID()
        NotificationsTable.insert {
            it[NotificationsTable.id] = id
            it[NotificationsTable.eventId] = eventId
            it[NotificationsTable.userId] = userId
            it[NotificationsTable.type] = type
            it[NotificationsTable.destination] = destination
            it[NotificationsTable.subject] = subject
            it[NotificationsTable.body] = body
            it[status] = deliveryStatus.name
            it[createdAt] = OffsetDateTime.now(ZoneOffset.UTC)
        }
        return findInternal(id)!!
    }

    private fun findInternal(id: UUID) = NotificationsTable.selectAll()
        .where { NotificationsTable.id eq id }
        .singleOrNull()
        ?.let(::toResponse)

    private fun findByEventId(eventId: UUID) = NotificationsTable.selectAll()
        .where { NotificationsTable.eventId eq eventId }
        .singleOrNull()
        ?.let(::toResponse)

    private fun toResponse(row: ResultRow) = NotificationResponse(
        row[NotificationsTable.id].toString(),
        row[NotificationsTable.userId].toString(),
        row[NotificationsTable.type],
        row[NotificationsTable.destination],
        row[NotificationsTable.subject],
        row[NotificationsTable.body],
        NotificationStatus.valueOf(row[NotificationsTable.status]),
        row[NotificationsTable.createdAt].toString(),
    )
}
