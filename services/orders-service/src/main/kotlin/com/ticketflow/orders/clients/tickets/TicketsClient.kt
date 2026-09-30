package com.ticketflow.orders.clients.tickets

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType

/**
 * HTTP client responsible for communication with Tickets Service.
 *
 * Orders Service never accesses tickets_db directly.
 */
class TicketsClient(
    private val httpClient: HttpClient,
    private val baseUrl: String,
) {

    /**
     * Retrieves an inventory item.
     */
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

    /**
     * Attempts to reserve an inventory item.
     *
     * Conflict means somebody else already reserved or sold it.
     */
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

            HttpStatusCode.Conflict,
            HttpStatusCode.NotFound ->
                null

            else ->
                error(
                    "Tickets Service returned ${response.status}"
                )
        }
    }

    /**
     * Releases an existing reservation.
     *
     * This is also the compensating action used by the Order saga.
     */
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

    /**
     * Converts a RESERVED ticket into SOLD.
     */
    suspend fun confirm(
        inventoryId: String,
        reservationId: String,
    ): TicketInventoryResponse? {
        val response =
            httpClient.post(
                "$baseUrl/inventory/$inventoryId/confirm"
            ) {
                contentType(ContentType.Application.Json)

                setBody(
                    ConfirmTicketRequest(
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

    /** Releases every expired reservation in Tickets Service. */
    suspend fun releaseExpired(): Int {
        val response = httpClient.post("$baseUrl/inventory/release-expired")
        if (response.status != HttpStatusCode.OK) {
            error("Tickets Service returned ${response.status}")
        }
        return response.body<Map<String, Int>>()["released"] ?: 0
    }

    suspend fun restock(inventoryId: String): TicketInventoryResponse? {
        val response = httpClient.post("$baseUrl/inventory/$inventoryId/restock")
        return when (response.status) {
            HttpStatusCode.OK -> response.body()
            HttpStatusCode.Conflict, HttpStatusCode.NotFound -> null
            else -> error("Tickets Service returned ${response.status}")
        }
    }
}