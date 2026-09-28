package com.ticketflow.events.modules.venues

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route

fun Route.venueRoutes(
    service: VenueService,
) {

    route("/venues") {

        /**
         * Lista todos los venues.
         */
        get {
            call.respond(
                service.findAll(),
            )
        }

        /**
         * Crea un nuevo venue.
         */
        post {

            val request =
                call.receive<CreateVenueRequest>()

            val venue =
                service.create(request)

            call.respond(
                HttpStatusCode.Created,
                venue,
            )
        }
    }
}