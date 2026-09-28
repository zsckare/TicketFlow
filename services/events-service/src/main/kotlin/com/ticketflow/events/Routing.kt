package com.ticketflow.events

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    routing {

        /**
         * Health check del Events Service.
         *
         * Este endpoint será utilizado posteriormente por Docker
         * y Kubernetes para comprobar el estado del microservicio.
         */
        get("/health") {
            call.respond(
                HttpStatusCode.OK,
                mapOf(
                    "service" to "events-service",
                    "status" to "UP",
                ),
            )
        }
    }
}