import { apiRequest } from './httpClient'
import type { OrderResponse } from '../types/orders'
const BASE = import.meta.env.VITE_ORDERS_API_URL
export const ordersApi = {
  getAll: () => apiRequest<OrderResponse[]>(`${BASE}/orders`),
  get: (id: string) => apiRequest<OrderResponse>(`${BASE}/orders/${id}`),
  create: (inventoryId: string) => apiRequest<OrderResponse>(`${BASE}/orders`, {
    method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ inventoryId }),
  }),
  confirm: (id: string) => apiRequest<OrderResponse>(`${BASE}/orders/${id}/confirm`, { method: 'POST' }),
  cancel: (id: string) => apiRequest<OrderResponse>(`${BASE}/orders/${id}/cancel`, { method: 'POST' }),
}
