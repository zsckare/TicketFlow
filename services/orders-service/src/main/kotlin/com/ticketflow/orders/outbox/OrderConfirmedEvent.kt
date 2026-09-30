package com.ticketflow.orders.outbox

import kotlinx.serialization.Serializable

@Serializable
data class OrderConfirmedItem(
    val inventoryId: String,
    val eventId: String,
    val eventName: String? = null,
    val eventStartsAt: String? = null,
    val venueName: String? = null,
    val sectionName: String? = null,
    val sectionType: String? = null,
    val seatLabel: String? = null,
    val unitPrice: String,
    val currency: String,
)

@Serializable
data class OrderConfirmedEvent(
    val eventId: String,
    val occurredAt: String,
    val orderId: String,
    val userId: String,
    val userEmail: String,
    val paymentId: String,
    val amount: String,
    val currency: String,
    val items: List<OrderConfirmedItem>,
)
