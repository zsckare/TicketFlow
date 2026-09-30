export type OrderStatus = 'PENDING' | 'RESERVED' | 'CONFIRMED' | 'FAILED' | 'CANCELLED'

export interface OrderItemResponse {
  id: string
  inventoryId: string
  reservationId?: string | null
  reservedUntil?: string | null
  eventId: string
  eventName?: string | null
  eventStartsAt?: string | null
  venueName?: string | null
  sectionId?: string | null
  sectionName?: string | null
  sectionType?: string | null
  seatId?: string | null
  seatLabel?: string | null
  unitPrice: string
  currency: string
}

export interface OrderResponse {
  id: string
  userId?: string | null
  inventoryId?: string | null
  reservationId?: string | null
  paymentId?: string | null
  amount: string
  currency: string
  status: OrderStatus
  failureReason?: string | null
  items: OrderItemResponse[]
  reservedUntil?: string | null
  createdAt: string
  updatedAt: string
}
