package com.ticketflow.orders.modules.orders

import kotlinx.serialization.Serializable

@Serializable
enum class OrderStatus {
    PENDING,
    RESERVED,
    CONFIRMED,
    FAILED,
    CANCELLED,
}

@Serializable
data class CreateOrderRequest(
    val inventoryId: String,
)

@Serializable
data class OrderResponse(
    val id: String,
    val userId: String? = null,
    val inventoryId: String,
    val reservationId: String? = null,
    val paymentId: String? = null,
    val amount: String,
    val currency: String,
    val status: OrderStatus,
    val failureReason: String? = null,
    val createdAt: String,
    val updatedAt: String,
)
