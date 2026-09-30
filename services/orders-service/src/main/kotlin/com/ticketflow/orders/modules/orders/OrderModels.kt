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
    val inventoryIds: List<String> = emptyList(),
    val inventoryId: String? = null,
)

/**
 * Information sent by the web application when checkout starts.
 *
 * Payments Service uses these URLs when the external payment provider
 * redirects the customer after completing or cancelling checkout.
 */
@Serializable
data class CheckoutRequest(
    val successUrl: String,
    val cancelUrl: String,
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
    val status: String = "ACTIVE",
    val refundedAmount: String = "0.00",
    val discountAmount: String = "0.00",
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
    val discountAmount: String = "0.00",
    val promotionCode: String? = null,
    val status: OrderStatus,
    val failureReason: String? = null,
    val items: List<OrderItemResponse> = emptyList(),
    val reservedUntil: String? = null,
    val createdAt: String,
    val updatedAt: String,
)

/**
 * Result returned to the frontend after checkout has been initialized.
 *
 * The order is included because a simulated payment can complete
 * synchronously while the original checkout request is still executing.
 */
@Serializable
data class CheckoutResponse(
    val order: OrderResponse,
    val paymentId: String,
    val paymentStatus: String,
    val checkoutUrl: String? = null,
)

@Serializable
data class PaymentSucceededRequest(
    val paymentId: String,
    val userEmail: String,
)