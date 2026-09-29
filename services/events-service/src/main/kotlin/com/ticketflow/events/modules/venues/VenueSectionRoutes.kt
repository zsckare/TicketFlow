package com.ticketflow.events.modules.sections

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import java.util.UUID

/**
 * Endpoints relacionados con las secciones de un venue.
 */
fun Route.venueSectionRoutes(
    service: VenueSectionService,
) {

    /**
     * GET /sections/{sectionId}
     *
     * Obtiene una sección concreta.
     */
    get("/sections/{sectionId}") {

        val sectionId =
            call.parameters["sectionId"]
                ?.let {
                    runCatching {
                        UUID.fromString(it)
                    }.getOrNull()
                }

        if (sectionId == null) {
            call.respond(
                HttpStatusCode.BadRequest,
                mapOf(
                    "error" to "Invalid section ID",
                ),
            )

            return@get
        }

        val section =
            service.findById(sectionId)

        if (section == null) {
            call.respond(
                HttpStatusCode.NotFound,
                mapOf(
                    "error" to "Section not found",
                ),
            )

            return@get
        }

        call.respond(section)
    }

    route("/venues/{venueId}/sections") {

        /**
         * GET /venues/{venueId}/sections
         */
        get {

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

            call.respond(
                service.findByVenue(
                    venueId,
                ),
            )
        }

        /**
         * POST /venues/{venueId}/sections
         */
        post {

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

                return@post
            }

            val request =
                call.receive<CreateVenueSectionRequest>()

            val section =
                service.create(
                    venueId = venueId,
                    request = request,
                )

            call.respond(
                HttpStatusCode.Created,
                section,
            )
        }
    }
}