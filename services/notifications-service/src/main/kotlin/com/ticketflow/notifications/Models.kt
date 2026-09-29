package com.ticketflow.notifications
import kotlinx.serialization.Serializable
@Serializable enum class NotificationStatus{PENDING,SENT,FAILED}
@Serializable data class CreateNotificationRequest(val userId:String,val type:String,val destination:String,val subject:String,val body:String)
@Serializable data class NotificationResponse(val id:String,val userId:String,val type:String,val destination:String,val subject:String,val body:String,val status:NotificationStatus,val createdAt:String)
