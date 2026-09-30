package com.ticketflow.users

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.OffsetDateTime
import java.util.UUID

data class UserRecord(
 val response: UserResponse,
 val passwordHash: String,
)

class UserRepository {

 fun create(
  email: String,
  hash: String,
  first: String,
  last: String,
  role: UserRole = UserRole.CUSTOMER,
 ): UserResponse = transaction {
  val now = OffsetDateTime.now()
  val id = UUID.randomUUID()

  UsersTable.insert {
   it[UsersTable.id] = id
   it[UsersTable.email] = email
   it[UsersTable.passwordHash] = hash
   it[UsersTable.firstName] = first
   it[UsersTable.lastName] = last
   it[UsersTable.role] = role.name
   it[UsersTable.status] = UserStatus.ACTIVE.name
   it[UsersTable.createdAt] = now
   it[UsersTable.updatedAt] = now
  }

  findByIdInternal(id)!!.response
 }

 fun findByEmail(
  email: String,
 ): UserRecord? = transaction {
  UsersTable
   .selectAll()
   .where {
    UsersTable.email eq email
   }
   .singleOrNull()
   ?.toRecord()
 }

 fun findById(
  id: UUID,
 ): UserResponse? = transaction {
  findByIdInternal(id)?.response
 }

 fun findAll(): List<UserResponse> = transaction {
  UsersTable
   .selectAll()
   .orderBy(
    UsersTable.createdAt,
    SortOrder.DESC,
   )
   .map {
    it.toRecord().response
   }
 }

 fun updateStatus(
  id: UUID,
  newStatus: UserStatus,
 ): UserResponse? = transaction {
  val updatedRows = UsersTable.update({
   UsersTable.id eq id
  }) {
   it[UsersTable.status] = newStatus.name
   it[UsersTable.updatedAt] = OffsetDateTime.now()
  }

  if (updatedRows == 0) {
   null
  } else {
   findByIdInternal(id)?.response
  }
 }


 fun updateProfile(id: UUID, first: String, last: String): UserResponse? = transaction {
  val count = UsersTable.update({ UsersTable.id eq id }) {
   it[UsersTable.firstName] = first
   it[UsersTable.lastName] = last
   it[UsersTable.updatedAt] = OffsetDateTime.now()
  }
  if (count == 0) null else findByIdInternal(id)?.response
 }

 fun updateRole(id: UUID, newRole: UserRole): UserResponse? = transaction {
  val count = UsersTable.update({ UsersTable.id eq id }) {
   it[UsersTable.role] = newRole.name
   it[UsersTable.updatedAt] = OffsetDateTime.now()
  }
  if (count == 0) null else findByIdInternal(id)?.response
 }

 fun updatePassword(id: UUID, hash: String): Boolean = transaction {
  UsersTable.update({ UsersTable.id eq id }) {
   it[UsersTable.passwordHash] = hash
   it[UsersTable.updatedAt] = OffsetDateTime.now()
  } > 0
 }

 private fun findByIdInternal(
  id: UUID,
 ): UserRecord? =
  UsersTable
   .selectAll()
   .where {
    UsersTable.id eq id
   }
   .singleOrNull()
   ?.toRecord()

 private fun ResultRow.toRecord(): UserRecord =
  UserRecord(
   response = UserResponse(
    id = this[UsersTable.id].toString(),
    email = this[UsersTable.email],
    firstName = this[UsersTable.firstName],
    lastName = this[UsersTable.lastName],
    role = UserRole.valueOf(
     this[UsersTable.role],
    ),
    status = UserStatus.valueOf(
     this[UsersTable.status],
    ),
    createdAt = this[
     UsersTable.createdAt
    ].toString(),
    updatedAt = this[
     UsersTable.updatedAt
    ].toString(),
   ),
   passwordHash = this[
    UsersTable.passwordHash
   ],
  )
}