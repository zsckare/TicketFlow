package com.ticketflow.notifications

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import kotlinx.serialization.Serializable

@Serializable
data class ErrorResponse(
    val error: String,
)

fun Application.configureStatusPages() {

    // Guardamos una referencia explícita a la aplicación porque dentro de
    // los handlers de StatusPages existen otros receivers implícitos.
    val application = this

    install(StatusPages) {

        exception<IllegalArgumentException> { call, cause ->
            call.respond(
                HttpStatusCode.BadRequest,
                ErrorResponse(
                    error = cause.message ?: "Invalid request",
                ),
            )
        }

        exception<Throwable> { call, cause ->

            // Usamos la referencia explícita para evitar el conflicto de
            // receivers que produce Ktor con `environment` dentro del handler.
            application.environment.log.error(
                "Unhandled exception",
                cause,
            )

            call.respond(
                HttpStatusCode.InternalServerError,
                ErrorResponse(
                    error = "Internal server error",
                ),
            )
        }
    }
}