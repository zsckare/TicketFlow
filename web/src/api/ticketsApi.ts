import { apiRequest } from './httpClient'
import type { TicketInventoryResponse } from '../types/tickets'

const BASE_URL = import.meta.env.VITE_TICKETS_API_URL

export const ticketsApi = {
  getEventInventory: (eventId: string) =>
    apiRequest<TicketInventoryResponse[]>(`${BASE_URL}/events/${eventId}/inventory`),
  getInventory: (inventoryId: string) =>
    apiRequest<TicketInventoryResponse>(`${BASE_URL}/inventory/${inventoryId}`),
}
