package com.ticketflow.users

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.OffsetDateTime
import java.util.UUID

data class RefreshTokenRecord(
    val id: UUID,
    val userId: UUID,
    val tokenHash: String,
    val expiresAt: OffsetDateTime,
    val revokedAt: OffsetDateTime?,
)

class RefreshTokenRepository {

    fun create(
        userId: UUID,
        tokenHash: String,
        expiresAt: OffsetDateTime,
    ): RefreshTokenRecord = transaction {
        val id = UUID.randomUUID()
        val now = OffsetDateTime.now()

        RefreshTokensTable.insert {
            it[RefreshTokensTable.id] = id
            it[RefreshTokensTable.userId] = userId
            it[RefreshTokensTable.tokenHash] = tokenHash
            it[RefreshTokensTable.expiresAt] = expiresAt
            it[RefreshTokensTable.createdAt] = now
            it[RefreshTokensTable.revokedAt] = null
        }

        RefreshTokenRecord(
            id = id,
            userId = userId,
            tokenHash = tokenHash,
            expiresAt = expiresAt,
            revokedAt = null,
        )
    }

    fun findActiveByHash(
        tokenHash: String,
    ): RefreshTokenRecord? = transaction {
        val now = OffsetDateTime.now()

        RefreshTokensTable
            .selectAll()
            .where {
                (RefreshTokensTable.tokenHash eq tokenHash) and
                    RefreshTokensTable.revokedAt.isNull() and
                    (RefreshTokensTable.expiresAt greater now)
            }
            .singleOrNull()
            ?.toRecord()
    }

    /**
     * Rotation invalidates the token that was just consumed.
     */
    fun revoke(id: UUID) = transaction {
        RefreshTokensTable.update({
            (RefreshTokensTable.id eq id) and
                RefreshTokensTable.revokedAt.isNull()
        }) {
            it[revokedAt] = OffsetDateTime.now()
        }
    }

    fun revokeByHash(tokenHash: String) = transaction {
        RefreshTokensTable.update({
            (RefreshTokensTable.tokenHash eq tokenHash) and
                RefreshTokensTable.revokedAt.isNull()
        }) {
            it[revokedAt] = OffsetDateTime.now()
        }
    }

    fun revokeAllForUser(userId: UUID) = transaction {
        RefreshTokensTable.update({
            (RefreshTokensTable.userId eq userId) and RefreshTokensTable.revokedAt.isNull()
        }) { it[revokedAt] = OffsetDateTime.now() }
    }

    private fun ResultRow.toRecord() =
        RefreshTokenRecord(
            id = this[RefreshTokensTable.id],
            userId = this[RefreshTokensTable.userId],
            tokenHash = this[RefreshTokensTable.tokenHash],
            expiresAt = this[RefreshTokensTable.expiresAt],
            revokedAt = this[RefreshTokensTable.revokedAt],
        )
}
