import type { TicketInventoryResponse } from '../../types/tickets'
import type { VenueSectionType } from '../../types/events'

export interface BookingSelection {
  inventory: TicketInventoryResponse
  sectionName: string
  sectionType: VenueSectionType
  seatLabel?: string
}
