package com.ticketflow.orders.clients.payments

import kotlinx.serialization.Serializable

@Serializable
data class CreatePaymentRequest(
    val orderId: String,
    val userId: String,
    val amount: String,
    val currency: String,
    val idempotencyKey: String,
)

@Serializable
data class PaymentResponse(
    val id: String,
    val orderId: String,
    val userId: String,
    val amount: String,
    val currency: String,
    val status: PaymentStatus,
    val idempotencyKey: String,
    val createdAt: String,
    val updatedAt: String,
)

@Serializable
enum class PaymentStatus {
    PENDING,
    SUCCEEDED,
    FAILED,
    REFUNDED,
}
