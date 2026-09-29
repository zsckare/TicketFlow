package com.ticketflow.notifications
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.util.UUID
fun Application.configureRouting(){val r=NotificationRepository();routing{get("/health"){call.respond(mapOf("status" to "UP"))};post("/notifications"){call.respond(HttpStatusCode.Created,r.create(call.receive()))};get("/users/{userId}/notifications"){call.respond(r.byUser(UUID.fromString(call.parameters["userId"])))}}}