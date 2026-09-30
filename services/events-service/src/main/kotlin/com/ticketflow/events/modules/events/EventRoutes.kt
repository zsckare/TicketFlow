package com.ticketflow.events.modules.events

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.delete
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import java.util.UUID

/**
 * Endpoints públicos relacionados con eventos.
 */
fun Route.eventRoutes(
    service: EventService,
) {

    route("/events") {

        /**
         * GET /events
         *
         * Lista todos los eventos.
         */
        get {
            call.respond(
                service.findAll(),
            )
        }

        /**
         * POST /events
         *
         * Crea un evento nuevo en estado DRAFT.
         */
        post {

            val request =
                call.receive<CreateEventRequest>()

            val event =
                service.create(request)

            call.respond(
                HttpStatusCode.Created,
                event,
            )
        }

        /**
         * GET /events/{eventId}
         */
        get("/{eventId}") {

            val eventId =
                call.parameters["eventId"]
                    ?.let {
                        runCatching {
                            UUID.fromString(it)
                        }.getOrNull()
                    }

            if (eventId == null) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf(
                        "error" to "Invalid event ID",
                    ),
                )

                return@get
            }

            val event =
                service.findById(eventId)

            if (event == null) {
                call.respond(
                    HttpStatusCode.NotFound,
                    mapOf(
                        "error" to "Event not found",
                    ),
                )

                return@get
            }

            call.respond(event)
        }

        delete("/{eventId}") {
            val eventId = call.parameters["eventId"]?.let { runCatching { UUID.fromString(it) }.getOrNull() }
                ?: return@delete call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid event ID"))
            service.delete(eventId)
            call.respond(HttpStatusCode.NoContent)
        }

        /**
         * POST /events/{eventId}/publish
         *
         * Cambia:
         *
         * DRAFT -> PUBLISHED
         */
        post("/{eventId}/publish") {

            val eventId =
                call.parameters["eventId"]
                    ?.let {
                        runCatching {
                            UUID.fromString(it)
                        }.getOrNull()
                    }

            if (eventId == null) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf(
                        "error" to "Invalid event ID",
                    ),
                )

                return@post
            }

            val event =
                service.publish(eventId)

            call.respond(event)
        }

        /**
         * POST /events/{eventId}/cancel
         */
        post("/{eventId}/cancel") {

            val eventId =
                call.parameters["eventId"]
                    ?.let {
                        runCatching {
                            UUID.fromString(it)
                        }.getOrNull()
                    }

            if (eventId == null) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf(
                        "error" to "Invalid event ID",
                    ),
                )

                return@post
            }

            val event =
                service.cancel(eventId)

            call.respond(event)
        }
    }
}