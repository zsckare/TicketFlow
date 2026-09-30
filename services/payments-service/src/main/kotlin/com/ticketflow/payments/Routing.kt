package com.ticketflow.payments

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopped
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.UUID

fun Application.configureRouting() {

    val repo =
        PaymentRepository()

    val client =
        HttpClient(CIO) {
            install(ContentNegotiation) {
                json(
                    Json {
                        ignoreUnknownKeys = true
                    },
                )
            }
        }

    /*
     * ============================================================
     * Configuration
     * ============================================================
     */

    val providerName =
        environment.config
            .propertyOrNull("payments.provider")
            ?.getString()
            ?.uppercase()
            ?: "SIMULATED"

    val stripeSecret =
        environment.config
            .propertyOrNull("stripe.secretKey")
            ?.getString()
            .orEmpty()

    val webhookSecret =
        environment.config
            .propertyOrNull("stripe.webhookSecret")
            ?.getString()
            .orEmpty()

    val ordersUrl =
        environment.config
            .property(
                "services.orders.baseUrl",
            )
            .getString()

    val internalSecret =
        environment.config
            .property(
                "services.orders.internalSecret",
            )
            .getString()

    /*
     * ============================================================
     * Payment Provider
     * ============================================================
     *
     * SIMULATED:
     * No external provider is required.
     *
     * STRIPE:
     * StripePaymentProvider handles Checkout Sessions and refunds.
     */

    val provider: PaymentProvider? =
        if (providerName == "STRIPE") {
            StripePaymentProvider(
                client,
                stripeSecret,
            )
        } else {
            null
        }

    /*
     * ============================================================
     * Orders callback
     * ============================================================
     *
     * Payments Service notifies Orders Service only after the
     * payment has been successfully completed.
     *
     * Orders Service then owns the transition:
     *
     * RESERVED
     *    ↓
     * SOLD
     *    ↓
     * CONFIRMED
     *    ↓
     * ticket ISSUED
     */

    suspend fun notifyOrder(
        payment: PaymentResponse,
    ) {
        val paymentId =
            UUID.fromString(
                payment.id,
            )

        val email =
            repo.customerEmail(
                paymentId,
            )
                ?: error(
                    "Customer email not found for payment ${payment.id}",
                )

        val response =
            client.post(
                "$ordersUrl/internal/orders/${payment.orderId}/payment-succeeded",
            ) {
                /*
                 * Service-to-service authentication.
                 *
                 * Must match the header expected by Orders Service.
                 */
                header(
                    "X-Internal-Service-Secret",
                    internalSecret,
                )

                contentType(
                    ContentType.Application.Json,
                )

                setBody(
                    mapOf(
                        "paymentId" to payment.id,
                        "userEmail" to email,
                    ),
                )
            }

        /*
         * Never silently ignore an Orders Service failure.
         *
         * Otherwise we could end up with:
         *
         * Payment = SUCCEEDED
         * Order   = RESERVED
         */
        if (!response.status.isSuccess()) {
            error(
                "Orders Service rejected payment confirmation " +
                    "for order ${payment.orderId}: ${response.status}",
            )
        }
    }

    /*
     * ============================================================
     * Routes
     * ============================================================
     */

    routing {

        /*
         * --------------------------------------------------------
         * Health
         * --------------------------------------------------------
         */

        get("/health") {
            call.respond(
                mapOf(
                    "status" to "UP",
                    "provider" to providerName,
                ),
            )
        }

        /*
         * --------------------------------------------------------
         * Checkout
         * --------------------------------------------------------
         *
         * Creates a PENDING payment.
         *
         * SIMULATED:
         * Immediately marks the payment SUCCEEDED and calls Orders.
         *
         * STRIPE:
         * Creates a Stripe Checkout Session and returns checkoutUrl.
         */

        post("/checkout") {

            val request =
                call.receive<CreateCheckoutRequest>()

            require(
                request.idempotencyKey.isNotBlank(),
            ) {
                "idempotencyKey is required"
            }

            require(
                request.currency.length == 3,
            ) {
                "currency must contain exactly 3 characters"
            }

            var payment =
                repo.createPending(
                    request,
                    providerName,
                )

            /*
             * Idempotency.
             *
             * If this checkout has already succeeded, return the
             * existing payment instead of creating another one.
             */
            if (
                payment.status ==
                    PaymentStatus.SUCCEEDED
            ) {
                return@post call.respond(
                    payment,
                )
            }

            /*
             * ----------------------------------------------------
             * Simulated payment
             * ----------------------------------------------------
             */

            if (
                providerName ==
                    "SIMULATED"
            ) {
                payment =
                    repo.markSucceeded(
                        UUID.fromString(
                            payment.id,
                        ),
                    )
                        ?: error(
                            "Unable to mark simulated payment as succeeded",
                        )

                /*
                 * IMPORTANT:
                 *
                 * Even simulated payments go through the same
                 * Payments → Orders callback used by Stripe.
                 */
                notifyOrder(
                    payment,
                )

                call.respond(
                    HttpStatusCode.Created,
                    payment,
                )

                return@post
            }

            /*
             * ----------------------------------------------------
             * Stripe Checkout
             * ----------------------------------------------------
             */

            if (
                payment.checkoutUrl == null
            ) {
                val stripeProvider =
                    provider
                        ?: error(
                            "Stripe provider is not configured",
                        )

                val session =
                    stripeProvider.createCheckout(
                        payment,
                        request,
                    )

                /*
                 * PaymentRepository.attachSession() currently uses:
                 *
                 * id
                 * sessionId
                 * url
                 *
                 * Keep these names aligned with the repository API.
                 */
                payment =
                    repo.attachSession(
                        id =
                            UUID.fromString(
                                payment.id,
                            ),
                        sessionId =
                            session.id,
                        url =
                            session.url,
                    )
            }

            call.respond(
                HttpStatusCode.Created,
                payment,
            )
        }

        /*
         * --------------------------------------------------------
         * Stripe Webhook
         * --------------------------------------------------------
         *
         * Stripe's browser redirect is NOT authoritative.
         *
         * The verified webhook is what transitions the payment
         * into SUCCEEDED.
         */

        post(
            "/payments/webhooks/stripe",
        ) {
            val payload =
                call.receiveText()

            val signature =
                call.request
                    .headers[
                        "Stripe-Signature"
                    ]
                    ?: return@post call.respond(
                        HttpStatusCode.BadRequest,
                    )

            /*
             * verifyStripeSignature() currently expects:
             *
             * payload
             * header
             * secret
             */
            if (
                webhookSecret.isBlank() ||
                !verifyStripeSignature(
                    payload = payload,
                    header = signature,
                    secret = webhookSecret,
                )
            ) {
                return@post call.respond(
                    HttpStatusCode.BadRequest,
                )
            }

            val webhook =
                Json
                    .parseToJsonElement(
                        payload,
                    )
                    .jsonObject

            val eventType =
                webhook["type"]
                    ?.jsonPrimitive
                    ?.content

            /*
             * For now the event that completes an order is:
             *
             * checkout.session.completed
             */
            if (
                eventType ==
                    "checkout.session.completed"
            ) {
                val session =
                    webhook["data"]
                        ?.jsonObject
                        ?.get("object")
                        ?.jsonObject

                val sessionId =
                    session
                        ?.get("id")
                        ?.jsonPrimitive
                        ?.content

                if (
                    sessionId != null
                ) {
                    val payment =
                        repo.markSucceededBySession(
                            sessionId,
                        )

                    if (
                        payment != null
                    ) {
                        notifyOrder(
                            payment,
                        )
                    }
                }
            }

            call.respond(
                HttpStatusCode.OK,
            )
        }

        /*
         * --------------------------------------------------------
         * Refund
         * --------------------------------------------------------
         */

        post(
            "/payments/{id}/refund",
        ) {
            val paymentId =
                call.parameters["id"]
                    ?.let {
                        runCatching {
                            UUID.fromString(
                                it,
                            )
                        }.getOrNull()
                    }
                    ?: return@post call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf(
                            "error" to
                                "Invalid payment id",
                        ),
                    )

            val existing =
                repo.findById(
                    paymentId,
                )
                    ?: return@post call.respond(
                        HttpStatusCode.NotFound,
                    )

            /*
             * With Stripe we first refund at the provider.
             *
             * Only after Stripe accepts the operation do we update
             * our local payment to REFUNDED.
             */
            if (
                existing.status ==
                    PaymentStatus.SUCCEEDED &&
                providerName ==
                    "STRIPE"
            ) {
                val sessionId =
                    existing.providerSessionId
                        ?: error(
                            "Stripe payment has no checkout session",
                        )

                val stripeProvider =
                    provider
                        ?: error(
                            "Stripe provider is not configured",
                        )

                stripeProvider.refund(
                    sessionId,
                )
            }

            val refunded =
                repo.refund(
                    paymentId,
                )
                    ?: error(
                        "Unable to refund payment $paymentId",
                    )

            call.respond(
                refunded,
            )
        }

        /*
         * --------------------------------------------------------
         * Get payment
         * --------------------------------------------------------
         */

        get(
            "/payments/{id}",
        ) {
            val paymentId =
                call.parameters["id"]
                    ?.let {
                        runCatching {
                            UUID.fromString(
                                it,
                            )
                        }.getOrNull()
                    }
                    ?: return@get call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf(
                            "error" to
                                "Invalid payment id",
                        ),
                    )

            val payment =
                repo.findById(
                    paymentId,
                )

            if (
                payment == null
            ) {
                call.respond(
                    HttpStatusCode.NotFound,
                )
            } else {
                call.respond(
                    payment,
                )
            }
        }

        /*
         * --------------------------------------------------------
         * Payments for an order
         * --------------------------------------------------------
         */

        get(
            "/orders/{orderId}/payments",
        ) {
            val orderId =
                call.parameters["orderId"]
                    ?.let {
                        runCatching {
                            UUID.fromString(
                                it,
                            )
                        }.getOrNull()
                    }
                    ?: return@get call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf(
                            "error" to
                                "Invalid orderId",
                        ),
                    )

            call.respond(
                repo.findByOrder(
                    orderId,
                ),
            )
        }
    }

    /*
     * Close the HTTP client when the application shuts down.
     */
    monitor.subscribe(
        ApplicationStopped,
    ) {
        client.close()
    }
}