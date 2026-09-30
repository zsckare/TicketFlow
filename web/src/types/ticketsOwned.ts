export type IssuedTicketStatus = 'ISSUED' | 'USED' | 'CANCELLED'
export interface IssuedTicketResponse {
  id: string
  orderId: string
  userId: string
  eventId: string
  eventName?: string | null
  eventStartsAt?: string | null
  venueName?: string | null
  inventoryId: string
  sectionId?: string | null
  sectionName?: string | null
  sectionType?: string | null
  seatId?: string | null
  seatLabel?: string | null
  admissionToken: string
  status: IssuedTicketStatus
  issuedAt: string
  checkedInAt?: string | null
}
