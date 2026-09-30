package com.ticketflow.payments

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

class PaymentRepository {
    fun createPending(r: CreateCheckoutRequest, providerName: String): PaymentResponse = transaction {
        findByKeyInternal(r.idempotencyKey)?.let { return@transaction it };
        val id = UUID.randomUUID();
        val now = OffsetDateTime.now(ZoneOffset.UTC); PaymentsTable.insert {
        it[PaymentsTable.id] = id; it[orderId] = UUID.fromString(r.orderId); it[userId] =
        UUID.fromString(r.userId); it[amount] = BigDecimal(r.amount); it[currency] =
        r.currency.uppercase(); it[status] = PaymentStatus.PENDING.name; it[idempotencyKey] =
        r.idempotencyKey; it[provider] = providerName; it[refundedAmount] = BigDecimal.ZERO; it[customerEmail] =
        r.userEmail; it[createdAt] = now; it[updatedAt] = now
    }; findByIdInternal(id)!!
    }

    fun attachSession(id: UUID, sessionId: String, url: String) = transaction {
        PaymentsTable.update({ PaymentsTable.id eq id }) {
            it[providerSessionId] = sessionId; it[checkoutUrl] = url; it[updatedAt] = OffsetDateTime.now(ZoneOffset.UTC)
        }; findByIdInternal(id)!!
    }

    fun markSucceededBySession(sessionId: String): PaymentResponse? = transaction {
        val row = PaymentsTable.selectAll().where { PaymentsTable.providerSessionId eq sessionId }.singleOrNull()
            ?: return@transaction null;
        val id =
            row[PaymentsTable.id]; if (row[PaymentsTable.status] != PaymentStatus.SUCCEEDED.name) PaymentsTable.update({ PaymentsTable.id eq id }) {
        it[status] = PaymentStatus.SUCCEEDED.name; it[updatedAt] = OffsetDateTime.now(ZoneOffset.UTC)
    }; findByIdInternal(id)
    }

    fun markSucceeded(id: UUID): PaymentResponse? = transaction {
        PaymentsTable.update({ PaymentsTable.id eq id }) {
            it[status] = PaymentStatus.SUCCEEDED.name; it[updatedAt] = OffsetDateTime.now(ZoneOffset.UTC)
        }; findByIdInternal(id)
    }

    fun customerEmail(id: UUID) = transaction {
        PaymentsTable.selectAll().where { PaymentsTable.id eq id }.singleOrNull()?.get(PaymentsTable.customerEmail)
    }

    fun findById(id: UUID) = transaction { findByIdInternal(id) }
    fun refund(
        id: UUID,
        requestedAmount: BigDecimal? = null,
        reason: String? = null,
        key: String? = null
    ): PaymentResponse? = transaction {
        val e = findByIdInternal(id) ?: return@transaction null
        require(e.status == PaymentStatus.SUCCEEDED || e.status == PaymentStatus.PARTIALLY_REFUNDED) { "Only successful payments can be refunded" }
        val already = BigDecimal(e.refundedAmount);
        val remaining = BigDecimal(e.amount).subtract(already);
        val amount = requestedAmount ?: remaining
        require(amount > BigDecimal.ZERO && amount <= remaining) { "Invalid refund amount" }
        val idem = key ?: "refund:${id}:${already.add(amount)}"; RefundsTable.selectAll()
        .where { RefundsTable.idempotencyKey eq idem }.singleOrNull()?.let { return@transaction findByIdInternal(id) }
        val total = already.add(amount);
        val next =
            if (total.compareTo(BigDecimal(e.amount)) == 0) PaymentStatus.REFUNDED else PaymentStatus.PARTIALLY_REFUNDED
        RefundsTable.insert {
            it[RefundsTable.id] = UUID.randomUUID(); it[paymentId] = id; it[RefundsTable.amount] =
            amount; it[RefundsTable.reason] = reason; it[idempotencyKey] = idem; it[createdAt] =
            OffsetDateTime.now(ZoneOffset.UTC)
        }
        PaymentsTable.update({ PaymentsTable.id eq id }) {
            it[refundedAmount] = total; it[status] = next.name; it[updatedAt] = OffsetDateTime.now(ZoneOffset.UTC)
        }; findByIdInternal(id)
    }

    fun findByOrder(id: UUID) =
        transaction { PaymentsTable.selectAll().where { PaymentsTable.orderId eq id }.map { it.response() } }

    fun findAll() = transaction { PaymentsTable.selectAll().map { it.response() } }
    private fun findByKeyInternal(k: String) =
        PaymentsTable.selectAll().where { PaymentsTable.idempotencyKey eq k }.singleOrNull()?.response()

    private fun findByIdInternal(id: UUID) =
        PaymentsTable.selectAll().where { PaymentsTable.id eq id }.singleOrNull()?.response()

    private fun ResultRow.response() = PaymentResponse(
        this[PaymentsTable.id].toString(),
        this[PaymentsTable.orderId].toString(),
        this[PaymentsTable.userId].toString(),
        this[PaymentsTable.amount].toPlainString(),
        this[PaymentsTable.currency],
        PaymentStatus.valueOf(this[PaymentsTable.status]),
        this[PaymentsTable.refundedAmount].toPlainString(),
        this[PaymentsTable.idempotencyKey],
        this[PaymentsTable.provider],
        this[PaymentsTable.checkoutUrl],
        this[PaymentsTable.providerSessionId],
        this[PaymentsTable.createdAt].toString(),
        this[PaymentsTable.updatedAt].toString()
    )
}
