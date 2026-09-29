package com.ticketflow.gateway
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
fun Application.configureRouting(){install(CORS){anyHost();allowHeader(HttpHeaders.Authorization);allowHeader(HttpHeaders.ContentType);allowMethod(HttpMethod.Patch);allowMethod(HttpMethod.Delete)};val client=HttpClient(CIO);val c=environment.config;val routes=listOf("auth" to c.property("services.users").getString(),"users" to c.property("services.users").getString(),"venues" to c.property("services.events").getString(),"sections" to c.property("services.events").getString(),"seats" to c.property("services.events").getString(),"events" to c.property("services.events").getString(),"inventory" to c.property("services.tickets").getString(),"orders" to c.property("services.orders").getString(),"payments" to c.property("services.payments").getString(),"notifications" to c.property("services.notifications").getString());routing{get("/health"){call.respond(mapOf("status" to "UP"))};route("/{...}"){handle{val first=call.request.path().trim('/').substringBefore('/');val base=routes.firstOrNull{it.first==first}?.second?:return@handle call.respond(HttpStatusCode.NotFound);proxy(call,client,base)}}}}
private suspend fun proxy(call:ApplicationCall,client:HttpClient,base:String){val target=base.trimEnd('/')+call.request.uri;val body=call.receive<ByteArray>();val response=client.request(target){method=call.request.httpMethod;call.request.headers.forEach{key,values->if(!key.equals(HttpHeaders.Host,true)&&!key.equals(HttpHeaders.ContentLength,true))values.forEach{header(key,it)}};if(body.isNotEmpty())setBody(body)};response.headers.forEach{key,values->if(!key.equals(HttpHeaders.TransferEncoding,true)&&!key.equals(HttpHeaders.ContentLength,true))values.forEach{call.response.headers.append(key,it,false)}};call.respondBytes(response.readRawBytes(),response.contentType(),response.status)}