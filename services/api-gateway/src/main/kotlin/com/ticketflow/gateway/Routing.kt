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
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

private data class GatewayTarget(
    val baseUrl: String,
    val path: String,
)

/**
 * Configura las rutas públicas del API Gateway.
 *
 * CORS NO se configura aquí.
 * Cors.kt es el único responsable de CORS en el Gateway.
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
         */
        route("/api/{...}") {
            handle {
                val requestPath = call.request.path()
                val publishEventId = requestPath
                    .removePrefix("/api/events/")
                    .takeIf {
                        call.request.httpMethod == HttpMethod.Post &&
                            requestPath.startsWith("/api/events/") &&
                            it.endsWith("/publish")
                    }
                    ?.removeSuffix("/publish")
                    ?.takeIf { it.isNotBlank() && !it.contains('/') }

                if (publishEventId != null) {
                    return@handle publishEvent(
                        call = call,
                        client = client,
                        eventId = publishEventId,
                        eventsBaseUrl = services.getValue("events"),
                        ticketsBaseUrl = services.getValue("tickets"),
                    )
                }

                val target = resolveTarget(
                    requestPath = requestPath,
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
             * Public:
             *
             * /api/inventory/events/{eventId}
             *
             * Internal:
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
             * Public:
             *
             * /api/payments/orders/{orderId}
             *
             * Internal:
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
             * Public:
             *
             * /api/notifications/users/{userId}
             *
             * Internal:
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
 * Publica un evento sólo cuando Tickets Service confirma que existe
 * inventario vendible. El Gateway orquesta; ningún servicio accede
 * a la base de datos de otro servicio.
 */
private suspend fun publishEvent(
    call: ApplicationCall,
    client: HttpClient,
    eventId: String,
    eventsBaseUrl: String,
    ticketsBaseUrl: String,
) {
    val readinessUrl =
        "${ticketsBaseUrl.trimEnd('/')}/inventory/events/$eventId/readiness"

    val readiness = runCatching {
        client.get(readinessUrl)
    }.getOrElse {
        return call.respond(
            HttpStatusCode.BadGateway,
            mapOf("error" to "Tickets Service is unavailable"),
        )
    }

    if (!readiness.status.isSuccess()) {
        return call.respondBytes(
            bytes = readiness.readRawBytes(),
            contentType = readiness.contentType(),
            status = readiness.status,
        )
    }

    val readinessBody = readiness.bodyAsText()
    val readinessJson = runCatching {
        Json.parseToJsonElement(readinessBody).jsonObject
    }.getOrElse {
        return call.respond(
            HttpStatusCode.BadGateway,
            mapOf("error" to "Invalid readiness response from Tickets Service"),
        )
    }

    val ready = readinessJson["ready"]?.jsonPrimitive?.boolean ?: false

    if (!ready) {
        return call.respondBytes(
            bytes = readinessBody.encodeToByteArray(),
            contentType = ContentType.Application.Json,
            status = HttpStatusCode.Conflict,
        )
    }

    val publishUrl =
        "${eventsBaseUrl.trimEnd('/')}/events/$eventId/publish"

    val response = runCatching {
        client.post(publishUrl) {
            call.request.headers[HttpHeaders.Authorization]?.let { token ->
                header(HttpHeaders.Authorization, token)
            }
        }
    }.getOrElse {
        return call.respond(
            HttpStatusCode.BadGateway,
            mapOf("error" to "Events Service is unavailable"),
        )
    }

    call.respondBytes(
        bytes = response.readRawBytes(),
        contentType = response.contentType(),
        status = response.status,
    )
}

/**
 * Reverse proxy del API Gateway.
 *
 * El Gateway recibe la petición del navegador y la
 * reenvía al microservicio correspondiente.
 *
 * IMPORTANTE:
 *
 * Los headers relacionados con CORS pertenecen exclusivamente
 * a la comunicación Browser -> Gateway.
 *
 * Por eso:
 *
 * 1. NO reenviamos Origin hacia los microservicios.
 * 2. NO reenviamos Access-Control-Request-*.
 * 3. NO copiamos Access-Control-* desde las respuestas
 *    de los microservicios.
 *
 * Cors.kt es el único responsable de CORS hacia el navegador.
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
     * Obtenemos el body original.
     *
     * En GET normalmente estará vacío.
     */
    val body = call.receive<ByteArray>()

    val response = client.request(targetUrl) {
        method = call.request.httpMethod

        /**
         * Reenviamos los headers del request.
         *
         * No reenviamos:
         *
         * Host
         *   Pertenece al Gateway.
         *
         * Content-Length
         *   Ktor calculará el tamaño correcto.
         *
         * Origin
         *   Es información CORS Browser -> Gateway.
         *
         * Access-Control-Request-*
         *   Son headers utilizados exclusivamente
         *   durante el preflight CORS.
         *
         * Authorization SÍ se conserva.
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
                ) ||
                key.equals(
                    HttpHeaders.Origin,
                    ignoreCase = true,
                ) ||
                key.startsWith(
                    "Access-Control-Request-",
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
     * Reenviamos los headers de respuesta del microservicio.
     *
     * No copiamos headers CORS porque el Gateway
     * genera sus propios headers mediante Cors.kt.
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
     * Conservamos exactamente el status y body
     * devueltos por el microservicio.
     */
    call.respondBytes(
        bytes = response.readRawBytes(),
        contentType = response.contentType(),
        status = response.status,
    )
}