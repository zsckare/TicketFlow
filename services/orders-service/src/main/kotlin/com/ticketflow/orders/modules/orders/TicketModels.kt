package com.ticketflow.orders.modules.orders

import kotlinx.serialization.Serializable

@Serializable
enum class IssuedTicketStatus { ISSUED, USED, CANCELLED }

@Serializable
data class IssuedTicketResponse(
    val id: String,
    val orderId: String,
    val userId: String,
    val eventId: String,
    val eventName: String? = null,
    val eventStartsAt: String? = null,
    val venueName: String? = null,
    val inventoryId: String,
    val sectionId: String? = null,
    val sectionName: String? = null,
    val sectionType: String? = null,
    val seatId: String? = null,
    val seatLabel: String? = null,
    val admissionToken: String,
    val status: IssuedTicketStatus,
    val issuedAt: String,
    val checkedInAt: String? = null,
)

@Serializable
data class CheckInRequest(val admissionToken: String)
