import { apiRequest } from './httpClient'
import type { EventResponse, SeatResponse, VenueResponse, VenueSectionResponse } from '../types/events'

const BASE_URL = import.meta.env.VITE_EVENTS_API_URL

export const eventsApi = {
  getEvents: () => apiRequest<EventResponse[]>(`${BASE_URL}/events`),
  getEvent: (eventId: string) => apiRequest<EventResponse>(`${BASE_URL}/events/${eventId}`),
  getVenue: (venueId: string) => apiRequest<VenueResponse>(`${BASE_URL}/venues/${venueId}`),
  getSections: (venueId: string) =>
    apiRequest<VenueSectionResponse[]>(`${BASE_URL}/venues/${venueId}/sections`),
  getSeats: (sectionId: string) =>
    apiRequest<SeatResponse[]>(`${BASE_URL}/sections/${sectionId}/seats`),
}
