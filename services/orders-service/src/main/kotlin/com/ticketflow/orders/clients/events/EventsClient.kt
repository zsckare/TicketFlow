package com.ticketflow.orders.clients.events

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode

class EventsClient(private val httpClient: HttpClient, private val baseUrl: String) {
    suspend fun findEvent(id: String): EventResponse? = get("$baseUrl/events/$id")
    suspend fun findVenue(id: String): VenueResponse? = get("$baseUrl/venues/$id")
    suspend fun findSection(id: String): VenueSectionResponse? = get("$baseUrl/sections/$id")
    suspend fun findSeat(id: String): SeatResponse? = get("$baseUrl/seats/$id")

    private suspend inline fun <reified T> get(url: String): T? {
        val response = httpClient.get(url)
        return when (response.status) {
            HttpStatusCode.OK -> response.body()
            HttpStatusCode.NotFound -> null
            else -> error("Events Service returned ${response.status}")
        }
    }
}
