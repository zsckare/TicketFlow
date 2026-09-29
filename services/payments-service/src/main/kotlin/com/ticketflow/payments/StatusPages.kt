package com.ticketflow.payments

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
                ErrorResponse(
                    message = cause.message ?: "Invalid request",
                ),
            )
        }

        exception<IllegalStateException> { call, cause ->
            call.respond(
                HttpStatusCode.Conflict,
                ErrorResponse(
                    message = cause.message ?: "Conflict",
                ),
            )
        }

        exception<Throwable> { call, cause ->
            application.environment.log.error(
                "Unhandled exception",
                cause,
            )

            call.respond(
                HttpStatusCode.InternalServerError,
                ErrorResponse(
                    message = "Internal server error",
                ),
            )
        }
    }
}