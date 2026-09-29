import { apiRequest, json } from './httpClient'
import type {
  ConfigureSectionInventoryRequest,
  CreateInventoryRequest,
  EventSectionInventoryResponse,
  TicketInventoryResponse,
  UpdateSeatPriceRequest,
} from '../types/tickets'

export const ticketsApi = {
  getEventInventory: (id: string) =>
    apiRequest<TicketInventoryResponse[]>(`/inventory/events/${id}`),
  getInventory: (id: string) =>
    apiRequest<TicketInventoryResponse>(`/inventory/${id}`),
  createInventory: (body: CreateInventoryRequest) =>
    apiRequest<TicketInventoryResponse>('/inventory', json(body)),
  getSectionConfigs: (eventId: string) =>
    apiRequest<EventSectionInventoryResponse[]>(`/inventory/events/${eventId}/sections`),
  configureSection: (eventId: string, body: ConfigureSectionInventoryRequest) =>
    apiRequest<EventSectionInventoryResponse>(`/inventory/events/${eventId}/sections`, { ...json(body), method: 'PUT' }),
  updateSeatPrice: (eventId: string, seatId: string, body: UpdateSeatPriceRequest) =>
    apiRequest<TicketInventoryResponse>(`/inventory/events/${eventId}/seats/${seatId}/price`, { ...json(body), method: 'PUT' }),
}
