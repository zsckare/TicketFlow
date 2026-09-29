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

        get {
            call.respond(
                service.findAll()
            )
        }

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