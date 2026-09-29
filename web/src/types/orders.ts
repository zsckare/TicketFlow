export type OrderStatus = 'PENDING' | 'RESERVED' | 'CONFIRMED' | 'FAILED' | 'CANCELLED'
export interface OrderResponse {
  id: string; inventoryId: string; reservationId?: string | null; amount: string
  currency: string; status: OrderStatus; failureReason?: string | null
  createdAt: string; updatedAt: string
}
