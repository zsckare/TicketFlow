package com.ticketflow.users
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestampWithTimeZone
object UsersTable:Table("users"){val id=uuid("id");val email=varchar("email",320);val passwordHash=varchar("password_hash",100);val firstName=varchar("first_name",100);val lastName=varchar("last_name",100);val role=varchar("role",30);val status=varchar("status",30);val createdAt=timestampWithTimeZone("created_at");val updatedAt=timestampWithTimeZone("updated_at");override val primaryKey=PrimaryKey(id)}