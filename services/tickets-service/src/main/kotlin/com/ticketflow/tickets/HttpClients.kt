package com.ticketflow.tickets

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopped
import kotlinx.serialization.json.Json

/**
 * HttpClient compartido por Tickets Service para comunicarse
 * con otros microservicios.
 */
lateinit var serviceHttpClient: HttpClient
    private set

/**
 * Configura el cliente HTTP utilizado para comunicación
 * service-to-service.
 */
fun Application.configureHttpClients() {

    serviceHttpClient =
        HttpClient(CIO) {

            install(ContentNegotiation) {
                json(
                    Json {
                        // Permite que Events Service agregue campos nuevos
                        // sin romper inmediatamente Tickets Service.
                        ignoreUnknownKeys = true
                    },
                )
            }
        }

    /**
     * Cerramos correctamente el cliente HTTP cuando
     * Tickets Service se detiene.
     */
    monitor.subscribe(ApplicationStopped) {
        serviceHttpClient.close()
    }
}