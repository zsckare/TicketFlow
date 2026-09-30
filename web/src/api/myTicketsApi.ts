import {apiDownload,apiRequest} from './httpClient'
import type {IssuedTicketResponse} from '../types/ticketsOwned'
export const myTicketsApi={
  getMine:()=>apiRequest<IssuedTicketResponse[]>('/orders/tickets/me'),
  downloadPdf:async(ticketId:string)=>{
    const blob=await apiDownload(`/orders/tickets/${ticketId}/pdf`)
    const url=URL.createObjectURL(blob)
    const a=document.createElement('a')
    a.href=url
    a.download=`TicketFlow-${ticketId}.pdf`
    document.body.appendChild(a)
    a.click()
    a.remove()
    URL.revokeObjectURL(url)
  },
}
