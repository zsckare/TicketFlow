package com.ticketflow.orders.modules.orders

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import java.util.UUID

fun Route.orderRoutes(
    service: OrderService,
) {

    route("/orders") {

        /**
         * Creates an Order and reserves its inventory.
         */
        post {
            val request =
                call.receive<CreateOrderRequest>()

            val order =
                service.create(request)

            call.respond(
                HttpStatusCode.Created,
                order,
            )
        }

        /**
         * Returns Orders, newest first.
         */
        get {
            call.respond(
                service.findAll()
            )
        }

        /**
         * Returns one Order.
         */
        get("/{orderId}") {
            val orderId =
                parseUuid(
                    call.parameters["orderId"]
                )
                    ?: return@get call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf(
                            "error" to
                                    "Invalid orderId"
                        ),
                    )

            call.respond(
                service.findById(orderId)
            )
        }

        /**
         * Confirms the Order and converts its ticket to SOLD.
         */
        post("/{orderId}/confirm") {
            val orderId =
                parseUuid(
                    call.parameters["orderId"]
                )
                    ?: return@post call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf(
                            "error" to
                                    "Invalid orderId"
                        ),
                    )

            call.respond(
                service.confirm(orderId)
            )
        }

        /**
         * Cancels the Order and releases its ticket.
         */
        post("/{orderId}/cancel") {
            val orderId =
                parseUuid(
                    call.parameters["orderId"]
                )
                    ?: return@post call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf(
                            "error" to
                                    "Invalid orderId"
                        ),
                    )

            call.respond(
                service.cancel(orderId)
            )
        }
    }
}

private fun parseUuid(
    value: String?,
): UUID? =
    try {
        value?.let(UUID::fromString)
    } catch (_: IllegalArgumentException) {
        null
    }