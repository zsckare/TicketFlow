import { apiRequest } from './httpClient'
import type { CreateInventoryRequest, TicketInventoryResponse } from '../types/tickets'
const BASE = import.meta.env.VITE_TICKETS_API_URL
export const ticketsApi = {
  getEventInventory: (eventId: string) =>
    apiRequest<TicketInventoryResponse[]>(`${BASE}/events/${eventId}/inventory`),
  getInventory: (id: string) => apiRequest<TicketInventoryResponse>(`${BASE}/inventory/${id}`),
  createInventory: (body: CreateInventoryRequest) =>
    apiRequest<TicketInventoryResponse>(`${BASE}/inventory`, {
      method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body),
    }),
}
