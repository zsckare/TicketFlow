package com.ticketflow.orders.modules.orders

import kotlinx.serialization.Serializable

@Serializable data class RefundTicketRequest(val reason: String? = null)
@Serializable data class TransferTicketRequest(val recipientEmail: String)
@Serializable data class AcceptTicketTransferRequest(val transferToken: String)
@Serializable data class TicketTransferResponse(val id:String,val ticketId:String,val fromUserId:String,val recipientEmail:String,val status:String,val transferToken:String,val expiresAt:String,val acceptedByUserId:String?=null,val createdAt:String,val acceptedAt:String?=null)
@Serializable data class TicketOwnershipEntry(val fromUserId:String?=null,val toUserId:String,val reason:String,val createdAt:String)
@Serializable data class ApplyPromotionRequest(val code:String)
@Serializable data class CreatePromotionRequest(val code:String,val description:String?=null,val discountType:String,val discountValue:String,val eventId:String?=null,val startsAt:String?=null,val endsAt:String?=null,val maxUses:Int?=null,val maxUsesPerUser:Int=1,val active:Boolean=true)
@Serializable data class PromotionResponse(val id:String,val code:String,val description:String?=null,val discountType:String,val discountValue:String,val eventId:String?=null,val startsAt:String?=null,val endsAt:String?=null,val maxUses:Int?=null,val maxUsesPerUser:Int,val currentUses:Int,val active:Boolean)
@Serializable data class EventSalesAnalyticsResponse(val eventId:String,val orders:Int,val ticketsSold:Int,val ticketsRefunded:Int,val ticketsCheckedIn:Int,val grossRevenue:String,val refundedRevenue:String,val netRevenue:String,val occupancyPercent:Double,val bySection:List<SectionSalesAnalytics>)
@Serializable data class SectionSalesAnalytics(val sectionId:String?,val sold:Int,val refunded:Int,val checkedIn:Int,val grossRevenue:String)
