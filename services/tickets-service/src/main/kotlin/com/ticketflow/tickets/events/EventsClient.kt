package com.ticketflow.tickets.clients.events

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode

/**
 * Cliente HTTP para comunicarnos con Events Service.
 *
 * Centralizamos aquí la comunicación entre microservicios
 * para evitar llamadas HTTP dispersas por Tickets Service.
 */
class EventsClient(
    private val httpClient: HttpClient,
    private val baseUrl: String,
) {

    /**
     * Busca un evento utilizando Events Service.
     *
     * GET {events-service}/events/{eventId}
     *
     * @return EventResponse cuando existe.
     * @return null cuando Events Service responde 404.
     *
     * Cualquier otra respuesta se considera un error de
     * comunicación con Events Service.
     */
    suspend fun findEventById(
        eventId: String,
    ): EventResponse? {

        val response =
            httpClient.get(
                "$baseUrl/events/$eventId",
            )

        return when (response.status) {

            HttpStatusCode.OK ->
                response.body<EventResponse>()

            HttpStatusCode.NotFound ->
                null

            else ->
                error(
                    "Events Service returned unexpected status: ${response.status}",
                )
        }
    }

    /**
     * Busca un asiento físico en Events Service.
     */
    suspend fun findSeatById(
        seatId: String,
    ): SeatResponse? {

        val response =
            httpClient.get(
                "$baseUrl/seats/$seatId",
            )

        return when (response.status) {

            HttpStatusCode.OK ->
                response.body<SeatResponse>()

            HttpStatusCode.NotFound ->
                null

            else ->
                error(
                    "Events Service returned unexpected status while loading seat: ${response.status}",
                )
        }
    }

    /**
     * Busca una sección física en Events Service.
     */
    suspend fun findSectionById(
        sectionId: String,
    ): VenueSectionResponse? {

        val response =
            httpClient.get(
                "$baseUrl/sections/$sectionId",
            )

        return when (response.status) {

            HttpStatusCode.OK ->
                response.body<VenueSectionResponse>()

            HttpStatusCode.NotFound ->
                null

            else ->
                error(
                    "Events Service returned unexpected status while loading section: ${response.status}",
                )
        }
    }
}