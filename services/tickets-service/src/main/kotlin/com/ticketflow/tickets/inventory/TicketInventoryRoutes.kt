package com.ticketflow.tickets.modules.inventory

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.delete
import io.ktor.server.routing.post
import io.ktor.server.routing.put
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

    /** Configure a complete section before the event is published. */
    put("/inventory/events/{eventId}/sections") {
        val eventId = call.parameters["eventId"] ?: return@put call.respond(HttpStatusCode.BadRequest)
        val request = call.receive<ConfigureSectionInventoryRequest>()
        call.respond(HttpStatusCode.OK, service.configureSection(eventId, request))
    }

    /**
     * GET /inventory/events/{eventId}/readiness
     *
     * Used by the API Gateway before publishing an event.
     */
    get("/inventory/events/{eventId}/readiness") {
        val eventId = parseUuid(call.parameters["eventId"])
            ?: return@get call.respond(
                HttpStatusCode.BadRequest,
                mapOf("error" to "Invalid event ID"),
            )

        call.respond(service.getReadiness(eventId))
    }

    /** Removes all AVAILABLE inventory/configuration for a DRAFT event before deleting it. */
    delete("/inventory/events/{eventId}") {
        val eventId = parseUuid(call.parameters["eventId"])
            ?: return@delete call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid event ID"))
        val deleted = service.deleteEventInventory(eventId)
        call.respond(HttpStatusCode.OK, mapOf("deletedInventory" to deleted))
    }

    /** Returns the commercial configuration for every configured section. */
    get("/inventory/events/{eventId}/sections") {
        val eventId = parseUuid(call.parameters["eventId"])
            ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid event ID"))
        call.respond(service.findSectionConfigs(eventId))
    }

    /** Adds, changes or removes an individual seat price override. */
    put("/inventory/events/{eventId}/seats/{seatId}/price") {
        val eventId = call.parameters["eventId"] ?: return@put call.respond(HttpStatusCode.BadRequest)
        val seatId = call.parameters["seatId"] ?: return@put call.respond(HttpStatusCode.BadRequest)
        val request = call.receive<UpdateSeatPriceRequest>()
        call.respond(service.updateSeatPrice(eventId, seatId, request))
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

    /** Internal compensation used after a successful payment refund. */
    post("/inventory/{inventoryId}/restock") {
        val inventoryId = parseUuid(call.parameters["inventoryId"])
            ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid inventory ID"))
        call.respond(HttpStatusCode.OK, service.restockSold(inventoryId))
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