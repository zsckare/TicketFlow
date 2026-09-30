import { apiRequest, json } from './httpClient'
import type { OrderResponse } from '../types/orders'

export interface PricingTier { id:string;eventId:string;sectionId:string;name:string;price:string;currency:string;salesStartAt?:string|null;salesEndAt?:string|null;priority:number;active:boolean }
export interface Promotion { id:string;code:string;description?:string|null;discountType:'PERCENTAGE'|'FIXED';discountValue:string;eventId?:string|null;startsAt?:string|null;endsAt?:string|null;maxUses?:number|null;maxUsesPerUser:number;currentUses:number;active:boolean }
export interface EventAnalytics { eventId:string;orders:number;ticketsSold:number;ticketsRefunded:number;ticketsCheckedIn:number;grossRevenue:string;refundedRevenue:string;netRevenue:string;occupancyPercent:number;bySection:Array<{sectionId?:string|null;sold:number;refunded:number;checkedIn:number;grossRevenue:string}> }
export interface TicketTransfer { id:string;ticketId:string;fromUserId:string;recipientEmail:string;status:string;transferToken:string;expiresAt:string;acceptedByUserId?:string|null;createdAt:string;acceptedAt?:string|null }

export const commerceApi = {
  analytics:(eventId:string)=>apiRequest<EventAnalytics>(`/orders/analytics/events/${eventId}`),
  promotions:()=>apiRequest<Promotion[]>('/orders/promotions'),
  createPromotion:(body:Omit<Promotion,'id'|'currentUses'>)=>apiRequest<Promotion>('/orders/promotions',json(body)),
  applyPromotion:(orderId:string,code:string)=>apiRequest<OrderResponse>(`/orders/${orderId}/promotion`,json({code})),
  pricingTiers:(eventId:string)=>apiRequest<PricingTier[]>(`/inventory/events/${eventId}/pricing-tiers`),
  createPricingTier:(eventId:string,body:Omit<PricingTier,'id'|'eventId'>)=>apiRequest<PricingTier>(`/inventory/events/${eventId}/pricing-tiers`,json(body)),
  deletePricingTier:(id:string)=>apiRequest<{deleted:boolean}>(`/inventory/pricing-tiers/${id}`,{method:'DELETE'}),
  transferTicket:(ticketId:string,recipientEmail:string)=>apiRequest<TicketTransfer>(`/orders/tickets/${ticketId}/transfer`,json({recipientEmail})),
  transfers:()=>apiRequest<TicketTransfer[]>('/orders/tickets/transfers'),
  acceptTransfer:(token:string)=>apiRequest<TicketTransfer>(`/orders/tickets/transfers/${token}/accept`,{method:'POST'}),
  refundTicket:(orderId:string,ticketId:string,reason?:string)=>apiRequest(`/orders/${orderId}/tickets/${ticketId}/refund`,json({reason})),
}
