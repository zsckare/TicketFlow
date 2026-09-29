package com.ticketflow.tickets

import com.ticketflow.tickets.modules.inventory.InvalidReservationException
import com.ticketflow.tickets.modules.inventory.TicketInventoryConflictException
import com.ticketflow.tickets.modules.inventory.TicketInventoryNotFoundException
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.plugins.statuspages.exception
import io.ktor.server.response.respond

/**
 * Configura el manejo centralizado de errores HTTP.
 */
fun Application.configureStatusPages() {

    /*
     * Guardamos una referencia explícita al logger de la aplicación
     * antes de entrar al DSL de StatusPages.
     *
     * Esto evita conflictos entre los receivers implícitos de Ktor.
     */
    val logger =
        environment.log

    install(StatusPages) {

        /**
         * Errores de validación enviados por el cliente.
         */
        exception<IllegalArgumentException> { call, cause ->

            call.respond(
                HttpStatusCode.BadRequest,
                mapOf(
                    "error" to
                            (
                                    cause.message
                                        ?: "Invalid request"
                                    ),
                ),
            )
        }

        /**
         * El recurso de inventario solicitado no existe.
         */
        exception<TicketInventoryNotFoundException> { call, cause ->

            call.respond(
                HttpStatusCode.NotFound,
                mapOf(
                    "error" to
                            (
                                    cause.message
                                        ?: "Ticket inventory not found"
                                    ),
                ),
            )
        }

        /**
         * El recurso existe, pero su estado actual impide
         * realizar la operación solicitada.
         *
         * Ejemplo:
         *
         * AVAILABLE -> reserve -> OK
         *
         * RESERVED -> reserve -> 409
         */
        exception<TicketInventoryConflictException> { call, cause ->

            call.respond(
                HttpStatusCode.Conflict,
                mapOf(
                    "error" to
                            (
                                    cause.message
                                        ?: "Ticket inventory conflict"
                                    ),
                ),
            )
        }

        /**
         * El reservationId proporcionado no posee
         * actualmente el ticket.
         */
        exception<InvalidReservationException> { call, cause ->

            call.respond(
                HttpStatusCode.Conflict,
                mapOf(
                    "error" to
                            (
                                    cause.message
                                        ?: "Invalid reservation"
                                    ),
                ),
            )
        }

        /**
         * Fallback para cualquier error inesperado.
         *
         * El detalle completo queda en los logs del servidor,
         * pero no exponemos stack traces, SQL ni información
         * interna al cliente HTTP.
         */
        exception<Throwable> { call, cause ->

            logger.error(
                "Unhandled exception",
                cause,
            )

            call.respond(
                HttpStatusCode.InternalServerError,
                mapOf(
                    "error" to
                            "Internal server error",
                ),
            )
        }
    }
}