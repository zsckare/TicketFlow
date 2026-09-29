import { apiRequest } from './httpClient'
import type {
  CreateEventRequest, CreateSeatRequest, CreateSectionRequest, CreateVenueRequest,
  EventResponse, SeatResponse, VenueResponse, VenueSectionResponse,
} from '../types/events'
const BASE = import.meta.env.VITE_EVENTS_API_URL

export const eventsApi = {
  getEvents: () => apiRequest<EventResponse[]>(`${BASE}/events`),
  getEvent: (id: string) => apiRequest<EventResponse>(`${BASE}/events/${id}`),
  createEvent: (body: CreateEventRequest) => apiRequest<EventResponse>(`${BASE}/events`, jsonPost(body)),
  publishEvent: (id: string) => apiRequest<EventResponse>(`${BASE}/events/${id}/publish`, { method: 'POST' }),
  cancelEvent: (id: string) => apiRequest<EventResponse>(`${BASE}/events/${id}/cancel`, { method: 'POST' }),
  getVenues: () => apiRequest<VenueResponse[]>(`${BASE}/venues`),
  getVenue: (id: string) => apiRequest<VenueResponse>(`${BASE}/venues/${id}`),
  createVenue: (body: CreateVenueRequest) => apiRequest<VenueResponse>(`${BASE}/venues`, jsonPost(body)),
  getSections: (venueId: string) => apiRequest<VenueSectionResponse[]>(`${BASE}/venues/${venueId}/sections`),
  createSection: (venueId: string, body: CreateSectionRequest) =>
    apiRequest<VenueSectionResponse>(`${BASE}/venues/${venueId}/sections`, jsonPost(body)),
  getSection: (id: string) => apiRequest<VenueSectionResponse>(`${BASE}/sections/${id}`),
  getSeats: (sectionId: string) => apiRequest<SeatResponse[]>(`${BASE}/sections/${sectionId}/seats`),
  createSeat: (sectionId: string, body: CreateSeatRequest) =>
    apiRequest<SeatResponse>(`${BASE}/sections/${sectionId}/seats`, jsonPost(body)),
  getSeat: (id: string) => apiRequest<SeatResponse>(`${BASE}/seats/${id}`),
}
function jsonPost(body: unknown): RequestInit {
  return { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body) }
}
