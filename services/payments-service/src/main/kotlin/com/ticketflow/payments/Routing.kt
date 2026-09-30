package com.ticketflow.payments
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.util.UUID
fun Application.configureRouting(){val repo=PaymentRepository();routing{get("/health"){call.respond(mapOf("status" to "UP"))};post("/payments"){val r=call.receive<CreatePaymentRequest>();require(r.idempotencyKey.isNotBlank());require(r.currency.length==3);call.respond(HttpStatusCode.Created,repo.create(r))};post("/payments/{id}/refund"){val id=UUID.fromString(call.parameters["id"]);val p=repo.refund(id);if(p==null)call.respond(HttpStatusCode.NotFound) else call.respond(p)};get("/payments/{id}"){val p=repo.findById(UUID.fromString(call.parameters["id"]));if(p==null)call.respond(HttpStatusCode.NotFound) else call.respond(p)};get("/orders/{orderId}/payments"){call.respond(repo.findByOrder(UUID.fromString(call.parameters["orderId"])))}}}