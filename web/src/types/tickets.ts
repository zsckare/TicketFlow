export type TicketInventoryStatus = 'AVAILABLE' | 'RESERVED' | 'SOLD'
export interface TicketInventoryResponse {
  id: string; eventId: string; seatId: string; price: string; currency: string
  status: TicketInventoryStatus; reservationId?: string | null; reservedUntil?: string | null
}
export interface CreateInventoryRequest {
  eventId: string; seatId: string; price: string; currency: string
}
