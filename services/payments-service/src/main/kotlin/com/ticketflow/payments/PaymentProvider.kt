package com.ticketflow.payments

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import kotlinx.serialization.json.*
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

data class CheckoutSession(val id: String, val url: String)
interface PaymentProvider {
    val name: String;
    suspend fun createCheckout(payment: PaymentResponse, request: CreateCheckoutRequest): CheckoutSession;
    suspend fun refund(providerSessionId: String, amount: String? = null)
}

class StripePaymentProvider(private val client: HttpClient, private val secretKey: String) : PaymentProvider {
    override val name = "STRIPE"
    override suspend fun createCheckout(payment: PaymentResponse, request: CreateCheckoutRequest): CheckoutSession {
        require(secretKey.isNotBlank()) { "STRIPE_SECRET_KEY is required when PAYMENT_PROVIDER=STRIPE" }
        val cents = (request.amount.toBigDecimal().movePointRight(2)).longValueExact()
        val response = client.submitForm("https://api.stripe.com/v1/checkout/sessions", Parameters.build {
            append("mode", "payment"); append("success_url", request.successUrl); append(
            "cancel_url",
            request.cancelUrl
        ); append("customer_email", request.userEmail)
            append("client_reference_id", request.orderId); append(
            "metadata[orderId]",
            request.orderId
        ); append("metadata[paymentId]", payment.id)
            append("line_items[0][quantity]", "1"); append(
            "line_items[0][price_data][currency]",
            request.currency.lowercase()
        ); append(
            "line_items[0][price_data][unit_amount]",
            cents.toString()
        ); append("line_items[0][price_data][product_data][name]", "TicketFlow order ${request.orderId.take(8)}")
        }) { header(HttpHeaders.Authorization, "Bearer $secretKey") }
        if (!response.status.isSuccess()) error("Stripe returned ${response.status}: ${response.body<String>()}")
        val obj = Json.parseToJsonElement(response.body<String>()).jsonObject
        return CheckoutSession(obj.getValue("id").jsonPrimitive.content, obj.getValue("url").jsonPrimitive.content)
    }

 override suspend fun refund(
  providerSessionId: String,
  amount: String?,
 ) {
  val sessionResponse = client.get(
   "https://api.stripe.com/v1/checkout/sessions/$providerSessionId"
  ) {
   header(HttpHeaders.Authorization, "Bearer $secretKey")
  }

  if (!sessionResponse.status.isSuccess()) {
   error("Unable to load Stripe Checkout Session")
  }

  val paymentIntent = Json
   .parseToJsonElement(sessionResponse.body<String>())
   .jsonObject["payment_intent"]
   ?.jsonPrimitive
   ?.content
   ?: error("Stripe session has no payment_intent")

  val refundResponse = client.submitForm(
   "https://api.stripe.com/v1/refunds",
   Parameters.build {
    append("payment_intent", paymentIntent)

    amount?.let {
     val cents = it
      .toBigDecimal()
      .movePointRight(2)
      .longValueExact()

     append("amount", cents.toString())
    }
   },
  ) {
   header(
    HttpHeaders.Authorization,
    "Bearer $secretKey",
   )
  }

  if (!refundResponse.status.isSuccess()) {
   error(
    "Stripe refund failed: ${refundResponse.body<String>()}"
   )
  }
 }
}

fun verifyStripeSignature(payload: String, header: String, secret: String, toleranceSeconds: Long = 300): Boolean {
    val parts =
        header.split(',').mapNotNull { val x = it.split('=', limit = 2); if (x.size == 2) x[0] to x[1] else null };
    val ts = parts.firstOrNull { it.first == "t" }?.second?.toLongOrNull() ?: return false
    if (kotlin.math.abs(System.currentTimeMillis() / 1000 - ts) > toleranceSeconds) return false
    val expected = hmacSha256Hex(secret, "$ts.$payload")
    return parts.filter { it.first == "v1" }.any { constantTimeEquals(expected, it.second) }
}

private fun hmacSha256Hex(secret: String, value: String): String {
    val mac = Mac.getInstance("HmacSHA256"); mac.init(
        SecretKeySpec(
            secret.toByteArray(),
            "HmacSHA256"
        )
    ); return mac.doFinal(value.toByteArray()).joinToString("") { "%02x".format(it) }
}

private fun constantTimeEquals(a: String, b: String): Boolean {
    if (a.length != b.length) return false;
    var r = 0; for (i in a.indices) r = r or (a[i].code xor b[i].code); return r == 0
}
