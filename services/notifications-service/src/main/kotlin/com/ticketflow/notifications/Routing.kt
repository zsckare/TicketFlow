package com.ticketflow.notifications

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.util.UUID

fun Application.configureRouting() {
    val repository = NotificationRepository()
    routing {
        get("/health") { call.respond(mapOf("status" to "UP")) }
        authenticate("auth-jwt") {
            get("/notifications/me") {
                val principal = call.principal<JWTPrincipal>()!!
                call.respond(repository.byUser(UUID.fromString(principal.payload.subject)))
            }
            post("/notifications") {
                val principal = call.principal<JWTPrincipal>()!!
                if (principal.payload.getClaim("role").asString() != "ADMIN")
                    return@post call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Admin role required"))
                call.respond(HttpStatusCode.Created, repository.create(call.receive()))
            }
            get("/users/{userId}/notifications") {
                val principal = call.principal<JWTPrincipal>()!!
                if (principal.payload.getClaim("role").asString() != "ADMIN")
                    return@get call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Admin role required"))
                val userId = runCatching { UUID.fromString(call.parameters["userId"]) }.getOrNull()
                    ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid userId"))
                call.respond(repository.byUser(userId))
            }
        }
    }
}
