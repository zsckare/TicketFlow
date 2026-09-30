package com.ticketflow.orders.clients.payments
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
class PaymentsClient(private val httpClient:HttpClient,private val baseUrl:String){
 suspend fun create(request:CreatePaymentRequest):PaymentResponse{val r=httpClient.post("$baseUrl/payments"){contentType(ContentType.Application.Json);setBody(request)};if(r.status!=HttpStatusCode.Created)error("Payments Service returned ${r.status}");return r.body()}
 suspend fun refund(paymentId:String):PaymentResponse{val r=httpClient.post("$baseUrl/payments/$paymentId/refund"){contentType(ContentType.Application.Json);setBody(mapOf("reason" to "Order cancelled"))};if(r.status!=HttpStatusCode.OK)error("Payments Service returned ${r.status}");return r.body()}
}
