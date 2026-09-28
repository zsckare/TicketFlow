package com.ticketflow.events

import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals

class ServerTest {

    /**
     * Verifica que el health check del Events Service
     * esté disponible.
     */
    @Test
    fun `health endpoint returns service status`() = testApplication {

        // Carga la configuración definida en application.yaml.
        configure()

        val response = client.get("/health")

        assertEquals(
            HttpStatusCode.OK,
            response.status,
        )
    }
}