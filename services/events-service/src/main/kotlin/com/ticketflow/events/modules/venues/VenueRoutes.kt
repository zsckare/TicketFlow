package com.ticketflow.events.modules.venues

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import java.util.UUID

fun Route.venueRoutes(
    service: VenueService,
) {

    route("/venues") {

        /**
         * GET /venues
         *
         * Lista todos los venues.
         */
        get {
            call.respond(
                service.findAll(),
            )
        }

        /**
         * GET /venues/{venueId}
         *
         * Obtiene un venue concreto.
         */
        get("/{venueId}") {

            val venueId =
                call.parameters["venueId"]
                    ?.let {
                        runCatching {
                            UUID.fromString(it)
                        }.getOrNull()
                    }

            if (venueId == null) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf(
                        "error" to "Invalid venue ID",
                    ),
                )

                return@get
            }

            val venue =
                service.findById(venueId)

            if (venue == null) {
                call.respond(
                    HttpStatusCode.NotFound,
                    mapOf(
                        "error" to "Venue not found",
                    ),
                )

                return@get
            }

            call.respond(venue)
        }

        /**
         * POST /venues
         *
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