package com.ticketflow.users
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.util.UUID
fun Application.configureRouting(){val repo=UserRepository();val auth=AuthService(repo,environment.config);val c=environment.config;install(Authentication){jwt("auth-jwt"){realm="ticketflow";verifier(JWT.require(Algorithm.HMAC256(c.property("jwt.secret").getString())).withIssuer(c.property("jwt.issuer").getString()).withAudience(c.property("jwt.audience").getString()).build());validate{if(it.payload.subject!=null)JWTPrincipal(it.payload) else null}}};routing{
 get("/health"){call.respond(mapOf("status" to "UP"))}
 post("/auth/register"){call.respond(HttpStatusCode.Created,auth.register(call.receive()))};post("/auth/login"){call.respond(auth.login(call.receive()))}
 authenticate("auth-jwt"){get("/users/me"){val id=UUID.fromString(call.principal<JWTPrincipal>()!!.payload.subject);call.respond(repo.findById(id)?:return@get call.respond(HttpStatusCode.NotFound))}
 get("/users"){requireAdmin(call);call.respond(repo.findAll())};get("/users/{id}"){requireAdmin(call);val id=UUID.fromString(call.parameters["id"]);call.respond(repo.findById(id)?:return@get call.respond(HttpStatusCode.NotFound))};patch("/users/{id}/status"){requireAdmin(call);val id=UUID.fromString(call.parameters["id"]);val req=call.receive<UpdateStatusRequest>();call.respond(repo.updateStatus(id,req.status)?:return@patch call.respond(HttpStatusCode.NotFound))}}
 }}
private suspend fun requireAdmin(call:ApplicationCall){if(call.principal<JWTPrincipal>()?.payload?.getClaim("role")?.asString()!=UserRole.ADMIN.name)throw SecurityException("Admin role required")}
