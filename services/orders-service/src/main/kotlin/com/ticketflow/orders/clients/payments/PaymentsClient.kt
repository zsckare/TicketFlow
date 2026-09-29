package com.ticketflow.orders.clients.payments

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType

class PaymentsClient(
    private val httpClient: HttpClient,
    private val baseUrl: String,
) {
    suspend fun create(request: CreatePaymentRequest): PaymentResponse {
        val response = httpClient.post("$baseUrl/payments") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }

        if (response.status != HttpStatusCode.Created) {
            error("Payments Service returned ${response.status}")
        }

        return response.body()
    }
}
