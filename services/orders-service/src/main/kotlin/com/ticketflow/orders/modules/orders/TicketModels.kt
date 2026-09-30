package com.ticketflow.orders.modules.orders

import kotlinx.serialization.Serializable

@Serializable
enum class IssuedTicketStatus {
    ISSUED,
    USED,
    CANCELLED,
}

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

    /**
     * Signed payload encoded inside the QR.
     *
     * The client must treat this value as opaque and send it back unchanged
     * when performing check-in.
     */
    val qrPayload: String? = null,

    /**
     * QR image represented as a data URL so the web application can render
     * it directly without having to generate or sign QR codes client-side.
     */
    val qrDataUrl: String? = null,
)

/**
 * Check-in now receives the complete signed QR payload instead of trusting
 * a raw admission UUID supplied by the client.
 */
@Serializable
data class CheckInRequest(
    val qrPayload: String,
    val eventId: String? = null,
)

@Serializable
data class CheckInStatsResponse(
    val eventId: String,
    val total: Int,
    val checkedIn: Int,
    val pending: Int,
    val cancelled: Int,
)