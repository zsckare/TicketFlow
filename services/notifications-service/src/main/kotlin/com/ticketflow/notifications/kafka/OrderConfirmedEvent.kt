package com.ticketflow.notifications.kafka

import kotlinx.serialization.Serializable

@Serializable
data class OrderConfirmedEvent(val eventId:String,val occurredAt:String,val orderId:String,val userId:String,val userEmail:String,val inventoryId:String,val paymentId:String,val amount:String,val currency:String)
