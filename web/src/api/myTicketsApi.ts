import {apiRequest} from './httpClient';import type {IssuedTicketResponse} from '../types/ticketsOwned'
export const myTicketsApi={getMine:()=>apiRequest<IssuedTicketResponse[]>('/orders/tickets/me')}
