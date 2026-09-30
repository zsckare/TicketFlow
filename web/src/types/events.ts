export type EventStatus = 'DRAFT' | 'PUBLISHED' | 'CANCELLED' | 'FINISHED'
export type VenueSectionType = 'GENERAL_ADMISSION' | 'RESERVED_SEATING'

export interface EventResponse {
  id: string
  venueId: string
  name: string
  description?: string | null
  startsAt: string
  endsAt: string
  status: EventStatus
}
export interface VenueResponse { id: string; name: string; address: string; city: string; timezone: string }
export interface VenueSectionResponse {
  id: string; venueId: string; name: string; type: VenueSectionType; capacity: number; mapX?:number|null;mapY?:number|null;mapWidth?:number|null;mapHeight?:number|null;mapRotation?:number
}
export interface SeatResponse { id: string; sectionId: string; row: string; number: string; mapX?:number|null;mapY?:number|null }

export interface CreateVenueRequest { name: string; address: string; city: string; timezone?: string }
export interface CreateSectionRequest { name: string; type: VenueSectionType; capacity: number }
export interface CreateSeatRequest { row: string; number: string }
export interface CreateEventRequest {
  venueId: string; name: string; description?: string; startsAt: string; endsAt: string
}

export interface UpdateEventRequest { name:string; description?:string; startsAt:string; endsAt?:string }
