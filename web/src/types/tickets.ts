export type TicketInventoryStatus = 'AVAILABLE' | 'RESERVED' | 'SOLD'
export type InventorySectionType = 'GENERAL_ADMISSION' | 'RESERVED_SEATING'

export interface TicketInventoryResponse {
  id: string
  eventId: string
  sectionId?: string | null
  seatId?: string | null
  price: string
  priceOverride?: string | null
  currency: string
  status: TicketInventoryStatus
  reservationId?: string | null
  reservedUntil?: string | null
}

export interface CreateInventoryRequest {
  eventId: string
  seatId: string
  price: string
  currency: string
}

export interface ConfigureSectionInventoryRequest {
  sectionId: string
  basePrice: string
  currency: string
  quantity?: number
}

export interface EventSectionInventoryResponse {
  eventId: string
  sectionId: string
  sectionType: InventorySectionType
  basePrice: string
  currency: string
  capacity: number
}

export interface UpdateSeatPriceRequest {
  priceOverride: string | null
}
