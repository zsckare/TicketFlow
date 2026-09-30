export type OrderStatus =
  | 'PENDING'
  | 'RESERVED'
  | 'CONFIRMED'
  | 'FAILED'
  | 'CANCELLED'

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
  status?: string
  refundedAmount?: string
  discountAmount?: string
}

export interface OrderResponse {
  id: string

  userId?: string | null
  inventoryId?: string | null
  reservationId?: string | null
  paymentId?: string | null

  amount: string
  currency: string
  discountAmount?: string
  promotionCode?: string | null

  status: OrderStatus

  failureReason?: string | null

  items: OrderItemResponse[]

  reservedUntil?: string | null

  createdAt: string
  updatedAt: string
}

/**
 * Response returned when an existing RESERVED order
 * begins its payment checkout.
 *
 * SIMULATED:
 * checkoutUrl will normally be null because payment succeeds
 * immediately.
 *
 * STRIPE:
 * checkoutUrl contains the Stripe Checkout URL.
 */
export interface CheckoutResponse {
    order: OrderResponse
    paymentId: string
    paymentStatus: string
    checkoutUrl?: string | null
}