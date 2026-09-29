package com.ticketflow.events

import com.ticketflow.events.modules.events.EventRepository
import com.ticketflow.events.modules.events.EventService
import com.ticketflow.events.modules.events.eventRoutes
import com.ticketflow.events.modules.venues.VenueRepository
import com.ticketflow.events.modules.venues.VenueService
import com.ticketflow.events.modules.venues.venueRoutes
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing

fun Application.configureRouting() {

    /*
     * Dependencies del módulo Venue.
     */
    val venueRepository =
        VenueRepository()

    val venueService =
        VenueService(
            repository = venueRepository,
        )

    /*
     * Dependencies del módulo Event.
     *
     * EventService utiliza VenueRepository porque necesita
     * verificar que el recinto exista antes de crear un evento.
     */
    val eventRepository =
        EventRepository()

    val eventService =
        EventService(
            eventRepository = eventRepository,
            venueRepository = venueRepository,
        )

    routing {

        get("/health") {
            call.respond(
                HttpStatusCode.OK,
                mapOf(
                    "service" to "events-service",
                    "status" to "UP",
                ),
            )
        }

        venueRoutes(
            service = venueService,
        )

        eventRoutes(
            service = eventService,
        )
    }
}