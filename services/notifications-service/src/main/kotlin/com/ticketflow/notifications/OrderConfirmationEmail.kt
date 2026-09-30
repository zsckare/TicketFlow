package com.ticketflow.notifications

import com.ticketflow.notifications.kafka.OrderConfirmedEvent
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object OrderConfirmationEmail {
    data class Content(
        val subject: String,
        val html: String,
    )

    fun build(event: OrderConfirmedEvent, webBaseUrl: String): Content {
        val first = event.items.firstOrNull()
        val eventName = first?.eventName ?: "Tu evento"
        val ticketsUrl = "${webBaseUrl.trimEnd('/')}/tickets"
        val subject = "Confirmación de compra · $eventName"

        val ticketRows = if (event.items.isEmpty()) {
            """<tr><td colspan="2" style="padding:12px 0;color:#64748b;">Tus boletos ya están disponibles en TicketFlow.</td></tr>"""
        } else {
            event.items.joinToString("") { item ->
                val location = listOfNotNull(item.sectionName, item.seatLabel?.let { "Asiento $it" }).joinToString(" · ")
                """
                <tr>
                  <td style="padding:12px 0;border-bottom:1px solid #e2e8f0;">
                    <strong>${escape(item.eventName ?: eventName)}</strong><br>
                    <span style="color:#64748b;">${escape(location.ifBlank { "Admisión general" })}</span>
                  </td>
                  <td style="padding:12px 0;border-bottom:1px solid #e2e8f0;text-align:right;white-space:nowrap;">
                    ${escape(money(item.unitPrice, item.currency))}
                  </td>
                </tr>
                """.trimIndent()
            }
        }

        val details = buildList {
            first?.eventStartsAt?.let { add("<strong>Fecha:</strong> ${escape(formatDate(it))}") }
            first?.venueName?.let { add("<strong>Lugar:</strong> ${escape(it)}") }
            add("<strong>Orden:</strong> #${escape(event.orderId.take(8).uppercase())}")
        }.joinToString("<br>")

        val html = """
            <!doctype html>
            <html lang="es">
            <body style="margin:0;background:#f8fafc;font-family:Arial,Helvetica,sans-serif;color:#0f172a;">
              <div style="max-width:640px;margin:0 auto;padding:32px 16px;">
                <div style="background:#ffffff;border:1px solid #e2e8f0;border-radius:16px;overflow:hidden;">
                  <div style="padding:28px 32px;background:#0f172a;color:#ffffff;">
                    <div style="font-size:13px;letter-spacing:.12em;text-transform:uppercase;opacity:.7;">TicketFlow</div>
                    <h1 style="margin:8px 0 0;font-size:26px;">¡Tu compra está confirmada!</h1>
                  </div>
                  <div style="padding:32px;">
                    <p style="margin-top:0;line-height:1.6;">Tu pago fue confirmado y tus boletos ya están listos.</p>
                    <div style="margin:24px 0;padding:16px;background:#f8fafc;border-radius:12px;line-height:1.7;">$details</div>
                    <table style="width:100%;border-collapse:collapse;font-size:14px;">$ticketRows</table>
                    <div style="margin-top:18px;text-align:right;font-size:18px;"><strong>Total: ${escape(money(event.amount, event.currency))}</strong></div>
                    <div style="margin:30px 0;text-align:center;">
                      <a href="${escape(ticketsUrl)}" style="display:inline-block;background:#0f172a;color:#ffffff;text-decoration:none;padding:13px 22px;border-radius:10px;font-weight:bold;">Ver mis boletos</a>
                    </div>
                    <p style="margin-bottom:0;color:#64748b;font-size:13px;line-height:1.6;">Presenta el QR de My Tickets en la entrada. Cada QR es único y no debe compartirse.</p>
                  </div>
                </div>
              </div>
            </body>
            </html>
        """.trimIndent()

        return Content(subject, html)
    }

    private val defaultZoneId = ZoneId.of("America/Monterrey")
    private fun formatDate(value: String): String = runCatching {
        OffsetDateTime.parse(value).atZoneSameInstant(defaultZoneId)
            .format(DateTimeFormatter.ofPattern("dd/MM/yyyy · hh:mm a", Locale.forLanguageTag("es-MX")))
    }.getOrDefault(value)

    private fun money(value: String, currency: String): String = runCatching {
        val formatter = NumberFormat.getCurrencyInstance(Locale("es", "MX"))
        formatter.currency = java.util.Currency.getInstance(currency.uppercase())
        formatter.format(BigDecimal(value))
    }.getOrDefault("$value ${currency.uppercase()}")

    private fun escape(value: String): String = value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&#39;")
}
