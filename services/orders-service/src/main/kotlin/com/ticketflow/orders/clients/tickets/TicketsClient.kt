package com.ticketflow.orders.clients.tickets

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType

class TicketsClient(
    private val httpClient: HttpClient,
    private val baseUrl: String,
) {

    suspend fun findInventoryById(
        inventoryId: String,
    ): TicketInventoryResponse? {
        val response =
            httpClient.get(
                "$baseUrl/inventory/$inventoryId"
            )

        return when (response.status) {
            HttpStatusCode.OK ->
                response.body()

            HttpStatusCode.NotFound ->
                null

            else ->
                error(
                    "Tickets Service returned ${response.status}"
                )
        }
    }

    suspend fun reserve(
        inventoryId: String,
    ): ReserveTicketResponse? {
        val response =
            httpClient.post(
                "$baseUrl/inventory/$inventoryId/reserve"
            )

        return when (response.status) {
            HttpStatusCode.OK ->
                response.body()

            HttpStatusCode.Conflict ->
                null

            HttpStatusCode.NotFound ->
                null

            else ->
                error(
                    "Tickets Service returned ${response.status}"
                )
        }
    }

    suspend fun release(
        inventoryId: String,
        reservationId: String,
    ): TicketInventoryResponse? {
        val response =
            httpClient.post(
                "$baseUrl/inventory/$inventoryId/release"
            ) {
                contentType(ContentType.Application.Json)

                setBody(
                    ReleaseTicketRequest(
                        reservationId = reservationId,
                    )
                )
            }

        return when (response.status) {
            HttpStatusCode.OK ->
                response.body()

            HttpStatusCode.Conflict,
            HttpStatusCode.NotFound ->
                null

            else ->
                error(
                    "Tickets Service returned ${response.status}"
                )
        }
    }
}