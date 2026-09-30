package com.ticketflow.orders.modules.orders

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.header
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import java.util.UUID

fun Route.orderRoutes(service: OrderService) {

    /*
     * ============================================================
     * Internal service-to-service routes
     * ============================================================
     *
     * Payments Service uses this endpoint after a payment has been
     * successfully verified.
     *
     * This endpoint deliberately lives OUTSIDE auth-jwt because
     * Payments Service does not have a customer's JWT.
     */
    route("/internal/orders") {

        post("/{orderId}/payment-succeeded") {

            val providedSecret =
                call.request.header(
                    "X-Internal-Service-Secret",
                )

            /*
             * IMPORTANT:
             * This fallback must match the Payments Service fallback.
             */
            val expectedSecret =
                System.getenv(
                    "INTERNAL_SERVICE_SECRET",
                )
                    ?: "ticketflow-internal-dev-secret"

            if (
                providedSecret == null ||
                providedSecret != expectedSecret
            ) {
                return@post call.respond(
                    HttpStatusCode.Unauthorized,
                    mapOf(
                        "error" to
                            "Invalid internal service credentials",
                    ),
                )
            }

            val orderId =
                parseUuid(
                    call.parameters["orderId"],
                )
                    ?: return@post call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf(
                            "error" to
                                "Invalid orderId",
                        ),
                    )

            val request =
                call.receive<PaymentSucceededRequest>()

            val paymentId =
                parseUuid(
                    request.paymentId,
                )
                    ?: return@post call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf(
                            "error" to
                                "Invalid paymentId",
                        ),
                    )

            val result =
                service.paymentSucceeded(
                    orderId = orderId,
                    paymentId = paymentId,
                    userEmail = request.userEmail,
                )

            call.respond(
                HttpStatusCode.OK,
                result,
            )
        }
    }

    /*
     * ============================================================
     * Customer/Admin authenticated routes
     * ============================================================
     */
    authenticate("auth-jwt") {

        route("/orders") {

            /*
             * Create an order and reserve inventory.
             */
            post {
                val principal =
                    call.principal<JWTPrincipal>()!!

                val userId =
                    UUID.fromString(
                        principal.payload.subject,
                    )

                val request =
                    call.receive<CreateOrderRequest>()

                call.respond(
                    HttpStatusCode.Created,
                    service.create(
                        userId,
                        request,
                    ),
                )
            }

            /*
             * List orders.
             *
             * Customers see their own orders.
             * Administrators can see all orders.
             */
            get {
                val principal =
                    call.principal<JWTPrincipal>()!!

                val userId =
                    UUID.fromString(
                        principal.payload.subject,
                    )

                call.respond(
                    service.findForUser(
                        userId,
                        principal.isAdmin(),
                    ),
                )
            }

            /*
             * Retrieve the user's active reservation/cart.
             *
             * eventId is optional.
             */
            get("/active") {
                val principal =
                    call.principal<JWTPrincipal>()!!

                val userId =
                    UUID.fromString(
                        principal.payload.subject,
                    )

                val eventIdValue =
                    call.request
                        .queryParameters["eventId"]

                val active =
                    if (
                        eventIdValue.isNullOrBlank()
                    ) {
                        service.findActive(
                            userId,
                        )
                    } else {
                        val eventId =
                            parseUuid(
                                eventIdValue,
                            )
                                ?: return@get call.respond(
                                    HttpStatusCode.BadRequest,
                                    mapOf(
                                        "error" to
                                            "Invalid eventId",
                                    ),
                                )

                        service.findActiveForEvent(
                            userId,
                            eventId,
                        )
                    }

                if (active == null) {
                    call.respond(
                        HttpStatusCode.NotFound,
                        mapOf(
                            "error" to
                                "No active reservation",
                        ),
                    )
                } else {
                    call.respond(
                        active,
                    )
                }
            }

            /*
             * Tickets belonging to the authenticated user.
             */
            get("/tickets/me") {
                val principal =
                    call.principal<JWTPrincipal>()!!

                val userId =
                    UUID.fromString(
                        principal.payload.subject,
                    )

                call.respond(
                    service.findTickets(
                        userId,
                    ),
                )
            }

            /*
             * Check-in a ticket.
             *
             * The service validates that the caller is an administrator.
             */
            post("/tickets/check-in") {
                val principal =
                    call.principal<JWTPrincipal>()!!

                val request =
                    call.receive<CheckInRequest>()

                val token =
                    parseUuid(
                        request.admissionToken,
                    )
                        ?: return@post call.respond(
                            HttpStatusCode.BadRequest,
                            mapOf(
                                "error" to
                                    "Invalid admission token",
                            ),
                        )

                call.respond(
                    service.checkIn(
                        token,
                        principal.isAdmin(),
                    ),
                )
            }

            /*
             * Retrieve an individual order.
             */
            get("/{orderId}") {
                val principal =
                    call.principal<JWTPrincipal>()!!

                val userId =
                    UUID.fromString(
                        principal.payload.subject,
                    )

                val orderId =
                    parseUuid(
                        call.parameters["orderId"],
                    )
                        ?: return@get call.respond(
                            HttpStatusCode.BadRequest,
                            mapOf(
                                "error" to
                                    "Invalid orderId",
                            ),
                        )

                call.respond(
                    service.findForUser(
                        orderId,
                        userId,
                        principal.isAdmin(),
                    ),
                )
            }

            /*
             * Begin checkout.
             *
             * This endpoint creates the payment but DOES NOT directly
             * confirm the order.
             *
             * Payment confirmation comes from Payments Service through:
             *
             * POST /internal/orders/{orderId}/payment-succeeded
             */
            post("/{orderId}/checkout") {
                val principal =
                    call.principal<JWTPrincipal>()!!

                val userId =
                    UUID.fromString(
                        principal.payload.subject,
                    )

                val userEmail =
                    principal.payload
                        .getClaim("email")
                        .asString()
                        ?: return@post call.respond(
                            HttpStatusCode.BadRequest,
                            mapOf(
                                "error" to
                                    "JWT has no email claim",
                            ),
                        )

                val orderId =
                    parseUuid(
                        call.parameters["orderId"],
                    )
                        ?: return@post call.respond(
                            HttpStatusCode.BadRequest,
                            mapOf(
                                "error" to
                                    "Invalid orderId",
                            ),
                        )

                call.respond(
                    service.checkout(
                        userId = userId,
                        orderId = orderId,
                        userEmail = userEmail,
                        isAdmin =
                            principal.isAdmin(),
                    ),
                )
            }

            /*
             * Cancel an order.
             */
            post("/{orderId}/cancel") {
                val principal =
                    call.principal<JWTPrincipal>()!!

                val userId =
                    UUID.fromString(
                        principal.payload.subject,
                    )

                val orderId =
                    parseUuid(
                        call.parameters["orderId"],
                    )
                        ?: return@post call.respond(
                            HttpStatusCode.BadRequest,
                            mapOf(
                                "error" to
                                    "Invalid orderId",
                            ),
                        )

                call.respond(
                    service.cancel(
                        userId,
                        orderId,
                        principal.isAdmin(),
                    ),
                )
            }
        }
    }
}

/**
 * Returns true when the authenticated JWT belongs to an ADMIN.
 */
private fun JWTPrincipal.isAdmin(): Boolean =
    payload
        .getClaim("role")
        .asString() == "ADMIN"

/**
 * Safely converts a String to UUID.
 */
private fun parseUuid(
    value: String?,
): UUID? =
    try {
        value?.let(
            UUID::fromString,
        )
    } catch (_: IllegalArgumentException) {
        null
    }