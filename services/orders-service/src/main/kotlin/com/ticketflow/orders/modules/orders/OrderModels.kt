package com.ticketflow.orders.modules.orders

import kotlinx.serialization.Serializable

@Serializable
enum class OrderStatus { PENDING, RESERVED, CONFIRMED, FAILED, CANCELLED }

@Serializable
data class CreateOrderRequest(
    val inventoryIds: List<String> = emptyList(),
    /** Backwards-compatible single inventory input. */
    val inventoryId: String? = null,
)

@Serializable
data class OrderItemResponse(
    val id: String,
    val inventoryId: String,
    val reservationId: String? = null,
    val eventId: String,
    val sectionId: String? = null,
    val seatId: String? = null,
    val unitPrice: String,
    val currency: String,
)

@Serializable
data class OrderResponse(
    val id: String,
    val userId: String? = null,
    /** Legacy convenience fields; populated when the order has exactly one item. */
    val inventoryId: String? = null,
    val reservationId: String? = null,
    val paymentId: String? = null,
    val amount: String,
    val currency: String,
    val status: OrderStatus,
    val failureReason: String? = null,
    val items: List<OrderItemResponse> = emptyList(),
    val createdAt: String,
    val updatedAt: String,
)
