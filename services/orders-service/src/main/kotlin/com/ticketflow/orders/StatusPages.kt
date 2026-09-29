package com.ticketflow.orders

import com.ticketflow.orders.modules.orders.InvalidOrderStateException
import com.ticketflow.orders.modules.orders.OrderNotFoundException
import com.ticketflow.orders.modules.orders.OrderOperationException
import com.ticketflow.orders.modules.orders.TicketNotAvailableException
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond

fun Application.configureStatusPages() {

    val logger = environment.log

    install(StatusPages) {

        exception<IllegalArgumentException> { call, cause ->
            call.respond(
                HttpStatusCode.BadRequest,
                mapOf(
                    "error" to
                            (cause.message ?: "Bad request")
                ),
            )
        }

        exception<OrderNotFoundException> { call, cause ->
            call.respond(
                HttpStatusCode.NotFound,
                mapOf(
                    "error" to
                            (cause.message ?: "Order not found")
                ),
            )
        }

        exception<TicketNotAvailableException> { call, cause ->
            call.respond(
                HttpStatusCode.Conflict,
                mapOf(
                    "error" to
                            (cause.message ?: "Ticket not available")
                ),
            )
        }

        exception<InvalidOrderStateException> { call, cause ->
            call.respond(
                HttpStatusCode.Conflict,
                mapOf(
                    "error" to
                            (cause.message ?: "Invalid order state")
                ),
            )
        }

        exception<OrderOperationException> { call, cause ->
            call.respond(
                HttpStatusCode.Conflict,
                mapOf(
                    "error" to
                            (cause.message ?: "Order operation failed")
                ),
            )
        }


        exception<SecurityException> { call, cause ->
            call.respond(
                HttpStatusCode.Forbidden,
                mapOf("error" to (cause.message ?: "Forbidden")),
            )
        }

        exception<Throwable> { call, cause ->
            logger.error(
                "Unhandled exception",
                cause,
            )

            call.respond(
                HttpStatusCode.InternalServerError,
                mapOf(
                    "error" to
                            "Internal server error"
                ),
            )
        }
    }
}