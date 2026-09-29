package com.ticketflow.notifications
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestampWithTimeZone
object NotificationsTable:Table("notifications"){val id=uuid("id");val userId=uuid("user_id");val eventId=uuid("event_id").nullable();val type=varchar("type",50);val destination=varchar("destination",320);val subject=varchar("subject",200);val body=text("body");val status=varchar("status",30);val createdAt=timestampWithTimeZone("created_at");override val primaryKey=PrimaryKey(id)}