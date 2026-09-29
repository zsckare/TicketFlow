import { useQuery } from '@tanstack/react-query'
import { ticketsApi } from '../../api/ticketsApi'

export function useEventInventory(eventId?: string) {
  return useQuery({
    queryKey: ['inventory', 'event', eventId],
    queryFn: () => ticketsApi.getEventInventory(eventId!),
    enabled: Boolean(eventId),
  })
}
