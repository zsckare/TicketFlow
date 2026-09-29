package com.ticketflow.tickets.modules.inventory

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import java.util.UUID

/**
 * Endpoints HTTP relacionados con el inventario
 * y sus reservaciones.
 */
fun Route.ticketInventoryRoutes(
    service: TicketInventoryService,
) {

    /**
     * POST /inventory
     *
     * Crea inventario para un asiento/evento.
     */
    post("/inventory") {

        val request =
            call.receive<CreateTicketInventoryRequest>()

        val inventory =
            service.create(
                request,
            )

        call.respond(
            HttpStatusCode.Created,
            inventory,
        )
    }

    /**
     * IMPORTANTE:
     *
     * Esta ruta debe declararse antes de
     * /inventory/{inventoryId}
     *
     * para evitar cualquier ambigüedad con "release-expired".
     */
    post("/inventory/release-expired") {

        val released =
            service.releaseExpired()

        call.respond(
            HttpStatusCode.OK,
            mapOf(
                "released" to released,
            ),
        )
    }

    /**
     * GET /inventory/{inventoryId}
     */
    get("/inventory/{inventoryId}") {

        val inventoryId =
            parseUuid(
                call.parameters["inventoryId"],
            )

        if (inventoryId == null) {
            call.respond(
                HttpStatusCode.BadRequest,
                mapOf(
                    "error" to "Invalid inventory ID",
                ),
            )

            return@get
        }

        val inventory =
            service.findById(
                inventoryId,
            )

        if (inventory == null) {
            call.respond(
                HttpStatusCode.NotFound,
                mapOf(
                    "error" to
                            "Ticket inventory not found",
                ),
            )

            return@get
        }

        call.respond(
            inventory,
        )
    }

    /**
     * GET /events/{eventId}/inventory
     */
    get("/events/{eventId}/inventory") {

        val eventId =
            parseUuid(
                call.parameters["eventId"],
            )

        if (eventId == null) {
            call.respond(
                HttpStatusCode.BadRequest,
                mapOf(
                    "error" to "Invalid event ID",
                ),
            )

            return@get
        }

        call.respond(
            service.findByEvent(
                eventId,
            ),
        )
    }

    /**
     * POST /inventory/{inventoryId}/reserve
     *
     * AVAILABLE -> RESERVED
     */
    post("/inventory/{inventoryId}/reserve") {

        val inventoryId =
            parseUuid(
                call.parameters["inventoryId"],
            )

        if (inventoryId == null) {
            call.respond(
                HttpStatusCode.BadRequest,
                mapOf(
                    "error" to
                            "Invalid inventory ID",
                ),
            )

            return@post
        }

        val reservation =
            service.reserve(
                inventoryId,
            )

        call.respond(
            HttpStatusCode.OK,
            reservation,
        )
    }

    /**
     * POST /inventory/{inventoryId}/release
     *
     * RESERVED -> AVAILABLE
     */
    post("/inventory/{inventoryId}/release") {

        val inventoryId =
            parseUuid(
                call.parameters["inventoryId"],
            )

        if (inventoryId == null) {
            call.respond(
                HttpStatusCode.BadRequest,
                mapOf(
                    "error" to
                            "Invalid inventory ID",
                ),
            )

            return@post
        }

        val request =
            call.receive<ReleaseTicketRequest>()

        val reservationId =
            parseUuid(
                request.reservationId,
            )

        if (reservationId == null) {
            call.respond(
                HttpStatusCode.BadRequest,
                mapOf(
                    "error" to
                            "Invalid reservation ID",
                ),
            )

            return@post
        }

        val inventory =
            service.release(
                inventoryId = inventoryId,
                reservationId = reservationId,
            )

        call.respond(
            HttpStatusCode.OK,
            inventory,
        )
    }

    /**
     * POST /inventory/{inventoryId}/confirm
     *
     * RESERVED -> SOLD
     */
    post("/inventory/{inventoryId}/confirm") {

        val inventoryId =
            parseUuid(
                call.parameters["inventoryId"],
            )

        if (inventoryId == null) {
            call.respond(
                HttpStatusCode.BadRequest,
                mapOf(
                    "error" to
                            "Invalid inventory ID",
                ),
            )

            return@post
        }

        val request =
            call.receive<ConfirmTicketRequest>()

        val reservationId =
            parseUuid(
                request.reservationId,
            )

        if (reservationId == null) {
            call.respond(
                HttpStatusCode.BadRequest,
                mapOf(
                    "error" to
                            "Invalid reservation ID",
                ),
            )

            return@post
        }

        val inventory =
            service.confirm(
                inventoryId = inventoryId,
                reservationId = reservationId,
            )

        call.respond(
            HttpStatusCode.OK,
            inventory,
        )
    }
}

/**
 * Convierte un String nullable en UUID de forma segura.
 */
private fun parseUuid(
    value: String?,
): UUID? {

    if (value == null) {
        return null
    }

    return runCatching {
        UUID.fromString(
            value,
        )
    }.getOrNull()
}