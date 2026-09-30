package com.ticketflow.payments
import kotlinx.serialization.Serializable
@Serializable enum class PaymentStatus{PENDING,SUCCEEDED,FAILED,REFUNDED}
@Serializable data class CreateCheckoutRequest(val orderId:String,val userId:String,val userEmail:String,val amount:String,val currency:String,val idempotencyKey:String,val successUrl:String,val cancelUrl:String)
@Serializable data class RefundPaymentRequest(val reason:String?=null)
@Serializable data class PaymentResponse(val id:String,val orderId:String,val userId:String,val amount:String,val currency:String,val status:PaymentStatus,val idempotencyKey:String,val provider:String,val checkoutUrl:String?=null,val providerSessionId:String?=null,val createdAt:String,val updatedAt:String)
@Serializable data class ErrorResponse(val message:String)
