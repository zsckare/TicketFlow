package com.ticketflow.orders.modules.orders

import kotlinx.serialization.Serializable

@Serializable
enum class OrderStatus { PENDING, RESERVED, CONFIRMED, FAILED, CANCELLED }

@Serializable
data class CreateOrderRequest(
    val inventoryIds: List<String> = emptyList(),
    val inventoryId: String? = null,
)

@Serializable
data class OrderItemResponse(
    val id: String,
    val inventoryId: String,
    val reservationId: String? = null,
    val reservedUntil: String? = null,
    val eventId: String,
    val eventName: String? = null,
    val eventStartsAt: String? = null,
    val venueName: String? = null,
    val sectionId: String? = null,
    val sectionName: String? = null,
    val sectionType: String? = null,
    val seatId: String? = null,
    val seatLabel: String? = null,
    val unitPrice: String,
    val currency: String,
)

@Serializable
data class OrderResponse(
    val id: String,
    val userId: String? = null,
    val inventoryId: String? = null,
    val reservationId: String? = null,
    val paymentId: String? = null,
    val amount: String,
    val currency: String,
    val status: OrderStatus,
    val failureReason: String? = null,
    val items: List<OrderItemResponse> = emptyList(),
    val reservedUntil: String? = null,
    val createdAt: String,
    val updatedAt: String,
)
