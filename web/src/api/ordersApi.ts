import { apiRequest } from './httpClient'
import type { OrderResponse } from '../types/orders'

const BASE_URL = import.meta.env.VITE_ORDERS_API_URL

export const ordersApi = {
  create: (inventoryId: string) =>
    apiRequest<OrderResponse>(`${BASE_URL}/orders`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ inventoryId }),
    }),
  get: (orderId: string) => apiRequest<OrderResponse>(`${BASE_URL}/orders/${orderId}`),
  confirm: (orderId: string) =>
    apiRequest<OrderResponse>(`${BASE_URL}/orders/${orderId}/confirm`, { method: 'POST' }),
  cancel: (orderId: string) =>
    apiRequest<OrderResponse>(`${BASE_URL}/orders/${orderId}/cancel`, { method: 'POST' }),
}
