package com.ticketflow.orders.modules.orders
import kotlinx.serialization.Serializable
@Serializable enum class IssuedTicketStatus{ISSUED,USED,CANCELLED}
@Serializable data class IssuedTicketResponse(val id:String,val orderId:String,val userId:String,val eventId:String,val inventoryId:String,val sectionId:String?=null,val seatId:String?=null,val admissionToken:String,val status:IssuedTicketStatus,val issuedAt:String,val checkedInAt:String?=null)
@Serializable data class CheckInRequest(val admissionToken:String)
