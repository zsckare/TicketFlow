package com.ticketflow.gateway

import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.cors.routing.CORS

/**
 * Configura CORS para permitir que los frontends de TicketFlow
 * se comuniquen con el API Gateway desde orígenes autorizados.
 *
 * Desarrollo local:
 * Frontend:    http://localhost:5174
 * API Gateway: http://localhost:8080
 *
 * Desarrollo mediante Tailscale:
 * Frontend:    http://antonios-mac-mini.tail4c6258.ts.net:5174
 * API Gateway: http://antonios-mac-mini.tail4c6258.ts.net:8080
 */
fun Application.configureCors() {
    install(CORS) {
        // Vite usando localhost.
        allowHost(
            "localhost:5174",
            schemes = listOf("http"),
        )

        // Desarrollo local usando 127.0.0.1.
        allowHost(
            "127.0.0.1:5174",
            schemes = listOf("http"),
        )

        // Frontend accesible desde dispositivos conectados
        // a nuestra Tailnet mediante Tailscale MagicDNS.
        allowHost(
            "antonios-mac-mini.tail4c6258.ts.net:5174",
            schemes = listOf("http"),
        )

        // Tailscale Serve.
        // El frontend se sirve mediante HTTPS para permitir
        // el acceso a la cámara desde dispositivos móviles.
        allowHost(
            "antonios-mac-mini.tail4c6258.ts.net",
            schemes = listOf("https"),
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