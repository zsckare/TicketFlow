package com.ticketflow.events

import com.ticketflow.events.modules.venues.VenueRepository
import com.ticketflow.events.modules.venues.VenueService
import com.ticketflow.events.modules.venues.venueRoutes
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting() {

    val venueRepository =
        VenueRepository()

    val venueService =
        VenueService(venueRepository)

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

        venueRoutes(venueService)
    }
}