package com.ticketflow.events.modules.seats

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import java.util.UUID

/**
 * Endpoints relacionados con los asientos físicos.
 */
fun Route.seatRoutes(
    service: SeatService,
) {

    /**
     * GET /seats/{seatId}
     *
     * Permite consultar un asiento físico por su UUID.
     *
     * Este endpoint también puede ser utilizado por otros
     * microservicios, como Tickets Service.
     */
    get("/seats/{seatId}") {

        val seatId =
            call.parameters["seatId"]
                ?.let {
                    runCatching {
                        UUID.fromString(it)
                    }.getOrNull()
                }

        if (seatId == null) {
            call.respond(
                HttpStatusCode.BadRequest,
                mapOf(
                    "error" to "Invalid seat ID",
                ),
            )

            return@get
        }

        val seat =
            service.findById(seatId)

        if (seat == null) {
            call.respond(
                HttpStatusCode.NotFound,
                mapOf(
                    "error" to "Seat not found",
                ),
            )

            return@get
        }

        call.respond(seat)
    }

    put("/seats/{seatId}/map") { val id=call.parameters["seatId"]?.let{runCatching{UUID.fromString(it)}.getOrNull()}?:return@put call.respond(HttpStatusCode.BadRequest);val result=service.updateMap(id,call.receive<UpdateSeatMapRequest>())?:return@put call.respond(HttpStatusCode.NotFound);call.respond(result) }

    route("/sections/{sectionId}/seats") {

        /**
         * GET /sections/{sectionId}/seats
         */
        get {

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

            call.respond(
                service.findBySection(
                    sectionId,
                ),
            )
        }

        /**
         * POST /sections/{sectionId}/seats
         */
        post {

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

                return@post
            }

            val request =
                call.receive<CreateSeatRequest>()

            val seat =
                service.create(
                    sectionId = sectionId,
                    request = request,
                )

            call.respond(
                HttpStatusCode.Created,
                seat,
            )
        }
    }
}