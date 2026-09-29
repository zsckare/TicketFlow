import { useQuery } from '@tanstack/react-query'
import { eventsApi } from '../../api/eventsApi'

export function useEvents() {
  return useQuery({ queryKey: ['events'], queryFn: eventsApi.getEvents })
}

export function useEvent(eventId?: string) {
  return useQuery({
    queryKey: ['events', eventId],
    queryFn: () => eventsApi.getEvent(eventId!),
    enabled: Boolean(eventId),
  })
}

export function useVenue(venueId?: string) {
  return useQuery({
    queryKey: ['venues', venueId],
    queryFn: () => eventsApi.getVenue(venueId!),
    enabled: Boolean(venueId),
  })
}

export function useSections(venueId?: string) {
  return useQuery({
    queryKey: ['venues', venueId, 'sections'],
    queryFn: () => eventsApi.getSections(venueId!),
    enabled: Boolean(venueId),
  })
}

export function useSeats(sectionId?: string) {
  return useQuery({
    queryKey: ['sections', sectionId, 'seats'],
    queryFn: () => eventsApi.getSeats(sectionId!),
    enabled: Boolean(sectionId),
  })
}
