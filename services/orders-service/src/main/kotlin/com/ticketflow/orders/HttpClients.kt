package com.ticketflow.orders

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopped
import kotlinx.serialization.json.Json

/**
 * Shared HTTP client used for service-to-service communication.
 */
lateinit var serviceHttpClient: HttpClient
    private set

fun Application.configureHttpClients() {
    serviceHttpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                }
            )
        }
    }

    monitor.subscribe(ApplicationStopped) {
        serviceHttpClient.close()
    }
}