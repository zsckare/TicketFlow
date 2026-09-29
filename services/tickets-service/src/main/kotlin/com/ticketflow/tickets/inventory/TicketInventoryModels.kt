package com.ticketflow.tickets.modules.inventory

import kotlinx.serialization.Serializable

@Serializable
enum class TicketInventoryStatus { AVAILABLE, RESERVED, SOLD }

@Serializable
enum class InventorySectionType { GENERAL_ADMISSION, RESERVED_SEATING }

/** Legacy single-seat creation request. Kept for compatibility. */
@Serializable
data class CreateTicketInventoryRequest(
    val eventId: String,
    val seatId: String,
    val price: String,
    val currency: String,
)

/** Configures all sellable inventory for a section of a DRAFT event. */
@Serializable
data class ConfigureSectionInventoryRequest(
    val sectionId: String,
    val basePrice: String,
    val currency: String,
    /** GA may sell fewer tickets than the physical section capacity. */
    val quantity: Int? = null,
)

@Serializable
data class UpdateSeatPriceRequest(
    /** null removes the override and restores the section base price. */
    val priceOverride: String? = null,
)

@Serializable
data class EventSectionInventoryResponse(
    val eventId: String,
    val sectionId: String,
    val sectionType: InventorySectionType,
    val basePrice: String,
    val currency: String,
    val capacity: Int,
)

@Serializable
data class TicketInventoryResponse(
    val id: String,
    val eventId: String,
    val sectionId: String? = null,
    val seatId: String? = null,
    val price: String,
    val priceOverride: String? = null,
    val currency: String,
    val status: TicketInventoryStatus,
    val reservationId: String? = null,
    val reservedUntil: String? = null,
)

@Serializable
data class ReserveTicketResponse(val inventory: TicketInventoryResponse)

@Serializable
data class ReleaseTicketRequest(val reservationId: String)

@Serializable
data class ConfirmTicketRequest(val reservationId: String)
