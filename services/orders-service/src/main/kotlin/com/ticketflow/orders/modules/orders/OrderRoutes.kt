package com.ticketflow.orders.modules.orders

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.*
import java.util.UUID

fun Route.orderRoutes(service: OrderService) {
    authenticate("auth-jwt") {
        route("/orders") {
            post {
                val principal = call.principal<JWTPrincipal>()!!
                val userId = UUID.fromString(principal.payload.subject)
                val request = call.receive<CreateOrderRequest>()
                call.respond(HttpStatusCode.Created, service.create(userId, request))
            }

            get {
                val principal = call.principal<JWTPrincipal>()!!
                val userId = UUID.fromString(principal.payload.subject)
                call.respond(service.findForUser(userId, principal.isAdmin()))
            }

            get("/tickets/me") {
                val principal = call.principal<JWTPrincipal>()!!
                call.respond(service.findTickets(UUID.fromString(principal.payload.subject)))
            }

            post("/tickets/check-in") {
                val principal = call.principal<JWTPrincipal>()!!
                val request = call.receive<CheckInRequest>()
                val token = parseUuid(request.admissionToken) ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid admission token"))
                call.respond(service.checkIn(token, principal.isAdmin()))
            }

            get("/{orderId}") {
                val principal = call.principal<JWTPrincipal>()!!
                val userId = UUID.fromString(principal.payload.subject)
                val orderId = parseUuid(call.parameters["orderId"])
                    ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid orderId"))
                call.respond(service.findForUser(orderId, userId, principal.isAdmin()))
            }

            post("/{orderId}/confirm") {
                val principal = call.principal<JWTPrincipal>()!!
                val userId = UUID.fromString(principal.payload.subject)
                val userEmail = principal.payload.getClaim("email").asString()
                    ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "JWT has no email claim"))
                val orderId = parseUuid(call.parameters["orderId"])
                    ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid orderId"))
                call.respond(service.confirm(userId, orderId, userEmail, principal.isAdmin()))
            }

            post("/{orderId}/cancel") {
                val principal = call.principal<JWTPrincipal>()!!
                val userId = UUID.fromString(principal.payload.subject)
                val orderId = parseUuid(call.parameters["orderId"])
                    ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid orderId"))
                call.respond(service.cancel(userId, orderId, principal.isAdmin()))
            }
        }
    }
}

private fun JWTPrincipal.isAdmin(): Boolean =
    payload.getClaim("role").asString() == "ADMIN"

private fun parseUuid(value: String?): UUID? =
    try {
        value?.let(UUID::fromString)
    } catch (_: IllegalArgumentException) {
        null
    }
