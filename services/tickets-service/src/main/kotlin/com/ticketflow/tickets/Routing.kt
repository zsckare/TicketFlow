package com.ticketflow.tickets

import com.ticketflow.tickets.clients.events.EventsClient
import com.ticketflow.tickets.modules.inventory.TicketInventoryRepository
import com.ticketflow.tickets.modules.inventory.TicketInventoryService
import com.ticketflow.tickets.modules.inventory.ticketInventoryRoutes
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing

fun Application.configureRouting() {

    val eventsBaseUrl =
        environment.config
            .property("services.events.baseUrl")
            .getString()

    /*
     * Cliente para comunicación:
     *
     * Tickets -> Events
     */
    val eventsClient =
        EventsClient(
            httpClient = serviceHttpClient,
            baseUrl = eventsBaseUrl,
        )

    /*
     * Módulo de inventario.
     */
    val inventoryRepository =
        TicketInventoryRepository()

    val inventoryService =
        TicketInventoryService(
            repository = inventoryRepository,
            eventsClient = eventsClient,
        )

    routing {

        get("/health") {
            call.respond(
                HttpStatusCode.OK,
                mapOf(
                    "service" to "tickets-service",
                    "status" to "UP",
                ),
            )
        }

        ticketInventoryRoutes(
            service = inventoryService,
        )
    }
}