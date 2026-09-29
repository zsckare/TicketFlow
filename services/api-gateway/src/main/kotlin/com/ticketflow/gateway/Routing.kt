package com.ticketflow.gateway

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

private data class GatewayTarget(
    val baseUrl: String,
    val path: String,
)

/**
 * Configura las rutas públicas del API Gateway.
 *
 * El frontend sólo conoce al Gateway:
 *
 * http://localhost:8080/api
 *
 * El Gateway se encarga de redirigir cada petición
 * al microservicio correspondiente.
 *
 * IMPORTANTE:
 * CORS NO se configura aquí.
 * Se configura exclusivamente desde Cors.kt.
 */
fun Application.configureRouting() {
    val client = HttpClient(CIO)

    val config = environment.config

    val services = mapOf(
        "users" to config.property("services.users").getString(),
        "events" to config.property("services.events").getString(),
        "tickets" to config.property("services.tickets").getString(),
        "orders" to config.property("services.orders").getString(),
        "payments" to config.property("services.payments").getString(),
        "notifications" to config.property("services.notifications").getString(),
    )

    routing {
        /**
         * Health check del API Gateway.
         */
        get("/health") {
            call.respond(
                mapOf(
                    "status" to "UP",
                ),
            )
        }

        /**
         * Todas las rutas públicas pasan por /api.
         *
         * Ejemplos:
         *
         * /api/events
         * /api/auth/login
         * /api/orders
         * /api/notifications/me
         */
        route("/api/{...}") {
            handle {
                val target = resolveTarget(
                    requestPath = call.request.path(),
                    services = services,
                ) ?: return@handle call.respond(
                    HttpStatusCode.NotFound,
                )

                proxy(
                    call = call,
                    client = client,
                    target = target,
                )
            }
        }
    }
}

/**
 * Determina qué microservicio debe recibir
 * una petición que llegó al API Gateway.
 */
private fun resolveTarget(
    requestPath: String,
    services: Map<String, String>,
): GatewayTarget? {
    val path = requestPath
        .removePrefix("/api")
        .ifBlank { "/" }

    val segments = path
        .trim('/')
        .split('/')
        .filter(String::isNotBlank)

    val first = segments.firstOrNull()
        ?: return null

    return when (first) {
        /**
         * Users Service
         *
         * /api/auth/...
         * /api/users/...
         */
        "auth",
        "users",
        -> GatewayTarget(
            baseUrl = services.getValue("users"),
            path = path,
        )

        /**
         * Events Service
         *
         * /api/venues/...
         * /api/sections/...
         * /api/seats/...
         * /api/events/...
         */
        "venues",
        "sections",
        "seats",
        "events",
        -> GatewayTarget(
            baseUrl = services.getValue("events"),
            path = path,
        )

        /**
         * Tickets Service
         */
        "inventory" -> {
            /**
             * Public API:
             *
             * /api/inventory/events/{eventId}
             *
             * Internal Tickets API:
             *
             * /events/{eventId}/inventory
             */
            if (
                segments.size == 3 &&
                segments[1] == "events"
            ) {
                GatewayTarget(
                    baseUrl = services.getValue("tickets"),
                    path = "/events/${segments[2]}/inventory",
                )
            } else {
                GatewayTarget(
                    baseUrl = services.getValue("tickets"),
                    path = path,
                )
            }
        }

        /**
         * Orders Service
         */
        "orders" -> GatewayTarget(
            baseUrl = services.getValue("orders"),
            path = path,
        )

        /**
         * Payments Service
         */
        "payments" -> {
            /**
             * Public API:
             *
             * /api/payments/orders/{orderId}
             *
             * Internal Payments API:
             *
             * /orders/{orderId}/payments
             */
            if (
                segments.size == 3 &&
                segments[1] == "orders"
            ) {
                GatewayTarget(
                    baseUrl = services.getValue("payments"),
                    path = "/orders/${segments[2]}/payments",
                )
            } else {
                GatewayTarget(
                    baseUrl = services.getValue("payments"),
                    path = path,
                )
            }
        }

        /**
         * Notifications Service
         */
        "notifications" -> {
            /**
             * Public API:
             *
             * /api/notifications/users/{userId}
             *
             * Internal Notifications API:
             *
             * /users/{userId}/notifications
             */
            if (
                segments.size == 3 &&
                segments[1] == "users"
            ) {
                GatewayTarget(
                    baseUrl = services.getValue("notifications"),
                    path = "/users/${segments[2]}/notifications",
                )
            } else {
                GatewayTarget(
                    baseUrl = services.getValue("notifications"),
                    path = path
                        .removePrefix("/notifications")
                        .let { "/notifications$it" },
                )
            }
        }

        else -> null
    }
}

/**
 * Actúa como reverse proxy entre el cliente
 * y el microservicio correspondiente.
 *
 * Conserva:
 *
 * - Método HTTP
 * - Query parameters
 * - Request body
 * - Authorization
 * - Content-Type
 * - Response status
 * - Response body
 *
 * Los headers CORS devueltos por los microservicios
 * NO se reenvían al navegador.
 *
 * El API Gateway es el único responsable de CORS.
 */
private suspend fun proxy(
    call: ApplicationCall,
    client: HttpClient,
    target: GatewayTarget,
) {
    val query = call.request.queryString()

    val targetUrl = buildString {
        append(
            target.baseUrl.trimEnd('/'),
        )

        append(
            target.path,
        )

        if (query.isNotBlank()) {
            append('?')
            append(query)
        }
    }

    /**
     * Obtenemos el body original de la petición.
     *
     * Para GET normalmente estará vacío.
     */
    val body = call.receive<ByteArray>()

    val response = client.request(targetUrl) {
        method = call.request.httpMethod

        /**
         * Reenviamos los headers originales.
         *
         * Host y Content-Length pertenecen a la conexión
         * Browser -> Gateway y no deben reutilizarse para
         * Gateway -> Microservice.
         */
        call.request.headers.forEach { key, values ->
            val shouldSkip =
                key.equals(
                    HttpHeaders.Host,
                    ignoreCase = true,
                ) ||
                key.equals(
                    HttpHeaders.ContentLength,
                    ignoreCase = true,
                )

            if (!shouldSkip) {
                values.forEach { value ->
                    header(
                        key,
                        value,
                    )
                }
            }
        }

        /**
         * Sólo enviamos body cuando existe.
         */
        if (body.isNotEmpty()) {
            setBody(body)
        }
    }

    /**
     * Copiamos los headers devueltos por el microservicio.
     *
     * IMPORTANTE:
     *
     * No reenviamos ningún header Access-Control-*.
     *
     * Algunos microservicios todavía tienen su propia
     * configuración CORS. Si copiáramos esos headers,
     * Cors.kt del Gateway agregaría nuevamente:
     *
     * Access-Control-Allow-Origin
     *
     * provocando headers duplicados.
     *
     * También ignoramos Vary porque el plugin CORS
     * del Gateway se encargará de generarlo.
     */
    response.headers.forEach { key, values ->
        val shouldSkip =
            key.equals(
                HttpHeaders.TransferEncoding,
                ignoreCase = true,
            ) ||
            key.equals(
                HttpHeaders.ContentLength,
                ignoreCase = true,
            ) ||
            key.equals(
                HttpHeaders.Vary,
                ignoreCase = true,
            ) ||
            key.startsWith(
                "Access-Control-",
                ignoreCase = true,
            )

        if (!shouldSkip) {
            values.forEach { value ->
                call.response.headers.append(
                    name = key,
                    value = value,
                    safeOnly = false,
                )
            }
        }
    }

    /**
     * Devolvemos al cliente exactamente el status y
     * body recibidos desde el microservicio.
     */
    call.respondBytes(
        bytes = response.readRawBytes(),
        contentType = response.contentType(),
        status = response.status,
    )
}