import { ApiError, apiRequest, json } from './httpClient'
import type { OrderResponse } from '../types/orders'

async function getActive(): Promise<OrderResponse | null> {
  try { return await apiRequest<OrderResponse>('/orders/active') }
  catch (error) { if (error instanceof ApiError && error.status === 404) return null; throw error }
}

async function getActiveForEvent(eventId: string): Promise<OrderResponse | null> {
  try {
    return await apiRequest<OrderResponse>(`/orders/active?eventId=${encodeURIComponent(eventId)}`)
  } catch (error) {
    if (error instanceof ApiError && error.status === 404) return null
    throw error
  }
}

export const ordersApi = {
  getAll: () => apiRequest<OrderResponse[]>('/orders'),
  get: (id: string) => apiRequest<OrderResponse>(`/orders/${id}`),
  getActive,
  getActiveForEvent,
  create: (inventoryIds: string[]) => apiRequest<OrderResponse>('/orders', json({ inventoryIds })),
  confirm: (id: string) => apiRequest<OrderResponse>(`/orders/${id}/confirm`, { method: 'POST' }),
  cancel: (id: string) => apiRequest<OrderResponse>(`/orders/${id}/cancel`, { method: 'POST' }),
}
