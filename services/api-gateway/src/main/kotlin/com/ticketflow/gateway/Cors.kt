package com.ticketflow.gateway

import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.cors.routing.CORS

/**
 * Configura CORS para permitir que el frontend de TicketFlow
 * se comunique con el API Gateway desde un origen diferente.
 *
 * En desarrollo:
 * Frontend:    http://localhost:5174
 * API Gateway: http://localhost:8080
 */
fun Application.configureCors() {
    install(CORS) {
        // Vite usando localhost.
        allowHost(
            "localhost:5174",
            schemes = listOf("http"),
        )

        // También permitimos 127.0.0.1 por si accedemos
        // al frontend usando esta dirección.
        allowHost(
            "127.0.0.1:5174",
            schemes = listOf("http"),
        )

        // Métodos utilizados por TicketFlow.
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Patch)
        allowMethod(HttpMethod.Delete)
        allowMethod(HttpMethod.Options)

        // Headers utilizados por el frontend.
        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.Accept)

        // Permite Content-Type application/json.
        allowNonSimpleContentTypes = true
    }
}