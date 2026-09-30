package com.ticketflow.payments

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.*
import java.util.UUID

fun Application.configureRouting() {
    val repo = PaymentRepository();
    val client = HttpClient(CIO) { install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) } }
    val providerName = environment.config.propertyOrNull("payments.provider")?.getString()?.uppercase() ?: "SIMULATED"
    val stripeSecret = environment.config.propertyOrNull("stripe.secretKey")?.getString().orEmpty();
    val webhookSecret = environment.config.propertyOrNull("stripe.webhookSecret")?.getString().orEmpty();
    val ordersUrl = environment.config.property("services.orders.baseUrl").getString();
    val internalSecret = environment.config.property("services.orders.internalSecret").getString()
    val provider: PaymentProvider? = if (providerName == "STRIPE") StripePaymentProvider(client, stripeSecret) else null
    suspend fun notifyOrder(payment: PaymentResponse) {
        val email = repo.customerEmail(UUID.fromString(payment.id))
            ?: return; client.post("$ordersUrl/internal/orders/${payment.orderId}/payment-succeeded") {
            header(
                "X-Internal-Service-Secret",
                internalSecret
            ); contentType(ContentType.Application.Json); setBody(
            mapOf(
                "paymentId" to payment.id,
                "userEmail" to email
            )
        )
        }
    }
    routing {
        get("/health") { call.respond(mapOf("status" to "UP", "provider" to providerName)) }
        get("/metrics") {
            val all = repo.findAll();
            val body = buildString {
                appendLine("# TYPE ticketflow_payments_total gauge"); PaymentStatus.entries.forEach { st ->
                appendLine("ticketflow_payments_total{status=\"${st.name.lowercase()}\"} ${all.count { it.status == st }}")
            }
            }; call.respondText(body, ContentType.Text.Plain)
        }
        post("/checkout") {
            if (call.request.headers["X-Internal-Service-Secret"] != internalSecret) return@post call.respond(
                HttpStatusCode.Unauthorized
            );
            val r =
                call.receive<CreateCheckoutRequest>(); require(r.idempotencyKey.isNotBlank()); require(r.currency.length == 3);
            var payment = repo.createPending(
                r,
                providerName
            ); if (payment.status == PaymentStatus.SUCCEEDED) return@post call.respond(payment)
            if (providerName == "SIMULATED") {
                payment = repo.markSucceeded(UUID.fromString(payment.id))!!; notifyOrder(payment); call.respond(
                    HttpStatusCode.Created,
                    payment
                )
            } else {
                if (payment.checkoutUrl == null) {
                    val s = provider!!.createCheckout(payment, r); payment =
                        repo.attachSession(UUID.fromString(payment.id), s.id, s.url)
                }; call.respond(HttpStatusCode.Created, payment)
            }
        }
        post("/payments/webhooks/stripe") {
            val payload = call.receiveText();
            val sig = call.request.headers["Stripe-Signature"]
                ?: return@post call.respond(HttpStatusCode.BadRequest); if (webhookSecret.isBlank() || !verifyStripeSignature(
                payload,
                sig,
                webhookSecret
            )
        ) return@post call.respond(HttpStatusCode.BadRequest)
            val obj =
                Json.parseToJsonElement(payload).jsonObject; if (obj["type"]?.jsonPrimitive?.content == "checkout.session.completed") {
            val session = obj["data"]?.jsonObject?.get("object")?.jsonObject;
            val sid = session?.get("id")?.jsonPrimitive?.content; if (sid != null) {
                repo.markSucceededBySession(sid)?.let { notifyOrder(it) }
            }
        }; call.respond(HttpStatusCode.OK)
        }
        post("/payments/{id}/refund") {
            if (call.request.headers["X-Internal-Service-Secret"] != internalSecret) return@post call.respond(
                HttpStatusCode.Unauthorized
            )
            val id = UUID.fromString(call.parameters["id"]);
            val existing = repo.findById(id) ?: return@post call.respond(HttpStatusCode.NotFound);
            val request = call.receive<RefundPaymentRequest>()
            if ((existing.status == PaymentStatus.SUCCEEDED || existing.status == PaymentStatus.PARTIALLY_REFUNDED) && providerName == "STRIPE") {
                val sid = existing.providerSessionId
                    ?: error("Stripe payment has no checkout session"); provider!!.refund(sid, request.amount)
            }
            call.respond(repo.refund(id, request.amount?.toBigDecimal(), request.reason, request.idempotencyKey)!!)
        }
        get("/payments/{id}") {
            val p =
                repo.findById(UUID.fromString(call.parameters["id"])); if (p == null) call.respond(HttpStatusCode.NotFound) else call.respond(
            p
        )
        }
        get("/orders/{orderId}/payments") { call.respond(repo.findByOrder(UUID.fromString(call.parameters["orderId"]))) }
    }
    monitor.subscribe(ApplicationStopped) { client.close() }
}
