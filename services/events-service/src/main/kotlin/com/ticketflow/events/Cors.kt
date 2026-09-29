package com.ticketflow.events


import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.cors.routing.CORS

/**
 * CORS configuration for the local TicketFlow frontend.
 *
 * Production origins will later be configured through environment
 * variables instead of being hardcoded.
 */
fun Application.configureCors() {

    install(CORS) {

        allowHost(
            "localhost:5174",
            schemes = listOf("http"),
        )

        allowHost(
            "127.0.0.1:5174",
            schemes = listOf("http"),
        )

        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Patch)
        allowMethod(HttpMethod.Delete)
        allowMethod(HttpMethod.Options)

        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.Authorization)
    }
}