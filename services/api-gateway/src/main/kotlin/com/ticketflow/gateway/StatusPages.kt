package com.ticketflow.gateway

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*

fun Application.configureStatusPages() {
    val application = this

    install(StatusPages) {

        exception<IllegalArgumentException> { call, cause ->
            call.respond(
                HttpStatusCode.BadRequest,
                mapOf(
                    "message" to (
                        cause.message
                            ?: "Invalid request"
                    ),
                ),
            )
        }

        exception<Throwable> { call, cause ->
            application.environment.log.error(
                "Unhandled gateway exception",
                cause,
            )

            call.respond(
                HttpStatusCode.InternalServerError,
                mapOf(
                    "message" to "Internal server error",
                ),
            )
        }
    }
}