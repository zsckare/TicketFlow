package com.ticketflow.notifications

import com.ticketflow.notifications.kafka.OrderConfirmedEvent
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Currency
import java.util.Locale

object OrderConfirmationEmail {

    data class Content(
        val subject: String,
        val html: String,
    )

    private val spanishLocale =
        Locale.forLanguageTag("es-MX")

    /**
     * Zona horaria por defecto de TicketFlow.
     *
     * Por ahora todos los eventos se presentan utilizando
     * America/Monterrey.
     *
     * Más adelante este valor podrá venir directamente del
     * Venue para soportar eventos en distintas zonas horarias.
     */
    private val defaultZoneId =
        ZoneId.of("America/Monterrey")

    /**
     * Formato utilizado para mostrar la fecha y hora del evento
     * al comprador.
     *
     * Ejemplo:
     * 30/09/2026 · 10:22 p. m.
     */
    private val eventDateFormatter =
        DateTimeFormatter.ofPattern(
            "dd/MM/yyyy · hh:mm a",
            spanishLocale,
        )

    fun build(
        event: OrderConfirmedEvent,
        webBaseUrl: String,
    ): Content {
        val first =
            event.items.firstOrNull()

        val eventName =
            first?.eventName
                ?: "Tu evento"

        val ticketsUrl =
            "${webBaseUrl.trimEnd('/')}/tickets"

        val subject =
            "Confirmación de compra · $eventName"

        val ticketRows =
            if (event.items.isEmpty()) {
                """
                <tr>
                  <td
                    colspan="2"
                    style="padding:12px 0;color:#64748b;"
                  >
                    Tus boletos ya están disponibles en TicketFlow.
                  </td>
                </tr>
                """.trimIndent()
            } else {
                event.items.joinToString("") { item ->
                    val location =
                        ticketLocation(
                            sectionName =
                                item.sectionName,
                            seatLabel =
                                item.seatLabel,
                        )

                    """
                    <tr>
                      <td
                        style="padding:12px 0;border-bottom:1px solid #e2e8f0;"
                      >
                        <strong>
                          ${escape(item.eventName ?: eventName)}
                        </strong>
                        <br>

                        <span style="color:#64748b;">
                          ${escape(location)}
                        </span>
                      </td>

                      <td
                        style="padding:12px 0;border-bottom:1px solid #e2e8f0;text-align:right;white-space:nowrap;"
                      >
                        ${escape(money(item.unitPrice, item.currency))}
                      </td>
                    </tr>
                    """.trimIndent()
                }
            }

        val details =
            buildList {
                first?.eventStartsAt?.let {
                    add(
                        "<strong>Fecha:</strong> ${
                            escape(
                                formatDate(it),
                            )
                        }",
                    )
                }

                first?.venueName?.let {
                    add(
                        "<strong>Lugar:</strong> ${
                            escape(it)
                        }",
                    )
                }

                add(
                    "<strong>Orden:</strong> #${
                        escape(
                            event.orderId
                                .take(8)
                                .uppercase(),
                        )
                    }",
                )
            }.joinToString("<br>")

        val html =
            """
            <!doctype html>
            <html lang="es">
            <body
              style="
                margin:0;
                background:#f8fafc;
                font-family:Arial,Helvetica,sans-serif;
                color:#0f172a;
              "
            >
              <div
                style="
                  max-width:640px;
                  margin:0 auto;
                  padding:32px 16px;
                "
              >

                <div
                  style="
                    background:#ffffff;
                    border:1px solid #e2e8f0;
                    border-radius:16px;
                    overflow:hidden;
                  "
                >

                  <div
                    style="
                      padding:28px 32px;
                      background:#0f172a;
                      color:#ffffff;
                    "
                  >
                    <div
                      style="
                        font-size:13px;
                        letter-spacing:.12em;
                        text-transform:uppercase;
                        opacity:.7;
                      "
                    >
                      TicketFlow
                    </div>

                    <h1
                      style="
                        margin:8px 0 0;
                        font-size:26px;
                      "
                    >
                      ¡Tu compra está confirmada!
                    </h1>
                  </div>

                  <div style="padding:32px;">

                    <p
                      style="
                        margin-top:0;
                        line-height:1.6;
                      "
                    >
                      Tu pago fue confirmado y tus boletos ya están listos.
                    </p>

                    <div
                      style="
                        margin:24px 0;
                        padding:16px;
                        background:#f8fafc;
                        border-radius:12px;
                        line-height:1.7;
                      "
                    >
                      $details
                    </div>

                    <table
                      style="
                        width:100%;
                        border-collapse:collapse;
                        font-size:14px;
                      "
                    >
                      $ticketRows
                    </table>

                    <div
                      style="
                        margin-top:18px;
                        text-align:right;
                        font-size:18px;
                      "
                    >
                      <strong>
                        Total: ${
                            escape(
                                money(
                                    event.amount,
                                    event.currency,
                                ),
                            )
                        }
                      </strong>
                    </div>

                    <div
                      style="
                        margin:30px 0;
                        text-align:center;
                      "
                    >
                      <a
                        href="${escape(ticketsUrl)}"
                        style="
                          display:inline-block;
                          background:#0f172a;
                          color:#ffffff;
                          text-decoration:none;
                          padding:13px 22px;
                          border-radius:10px;
                          font-weight:bold;
                        "
                      >
                        Ver mis boletos
                      </a>
                    </div>

                    <p
                      style="
                        margin-bottom:0;
                        color:#64748b;
                        font-size:13px;
                        line-height:1.6;
                      "
                    >
                      Presenta el QR de My Tickets en la entrada.
                      Cada QR es único y no debe compartirse.
                    </p>

                  </div>
                </div>
              </div>
            </body>
            </html>
            """.trimIndent()

        return Content(
            subject = subject,
            html = html,
        )
    }

    /**
     * Construye una descripción legible de la ubicación
     * correspondiente al boleto.
     *
     * Ejemplos:
     *
     * General
     * Sección Numerada
     * Sección Numerada · Asiento C1
     */
    private fun ticketLocation(
        sectionName: String?,
        seatLabel: String?,
    ): String {
        val normalizedSection =
            sectionName
                ?.trim()
                ?.takeIf {
                    it.isNotBlank()
                }
                ?.let(
                    ::humanizeSectionName,
                )

        val normalizedSeat =
            seatLabel
                ?.trim()
                ?.takeIf {
                    it.isNotBlank()
                }
                ?.let {
                    "Asiento $it"
                }

        return listOfNotNull(
            normalizedSection,
            normalizedSeat,
        )
            .joinToString(" · ")
            .ifBlank {
                "Admisión general"
            }
    }

    /**
     * Evita mostrar nombres técnicos como GENERAL o
     * GENERAL_ADMISSION directamente al comprador.
     *
     * Los nombres normales creados por el administrador
     * se conservan sin modificaciones.
     */
    private fun humanizeSectionName(
        value: String,
    ): String =
        when (
            value
                .trim()
                .uppercase()
        ) {
            "GENERAL",
            "GENERAL_ADMISSION",
            -> "General"

            else ->
                value
        }

    /**
     * Convierte el instante almacenado para el evento a la
     * zona horaria por defecto de TicketFlow.
     *
     * Ejemplo:
     *
     * 2026-10-01T04:22:00Z
     *
     * se presenta como:
     *
     * 30/09/2026 · 10:22 p. m.
     *
     * utilizando America/Monterrey.
     */
    private fun formatDate(
        value: String,
    ): String =
        runCatching {
            OffsetDateTime
                .parse(value)
                .atZoneSameInstant(
                    defaultZoneId,
                )
                .format(
                    eventDateFormatter,
                )
        }.getOrDefault(
            value,
        )

    /**
     * Formatea el importe utilizando configuración regional
     * mexicana y agrega explícitamente el código ISO de moneda.
     *
     * Ejemplo:
     *
     * $800.00 MXN
     */
    private fun money(
        value: String,
        currency: String,
    ): String =
        runCatching {
            val currencyCode =
                currency.uppercase()

            val formatter =
                NumberFormat.getCurrencyInstance(
                    spanishLocale,
                )

            formatter.currency =
                Currency.getInstance(
                    currencyCode,
                )

            val formatted =
                formatter.format(
                    BigDecimal(value),
                )

            "$formatted $currencyCode"
        }.getOrDefault(
            "$value ${currency.uppercase()}",
        )

    /**
     * Escapa cualquier contenido dinámico antes de
     * insertarlo dentro del HTML del email.
     */
    private fun escape(
        value: String,
    ): String =
        value
            .replace(
                "&",
                "&amp;",
            )
            .replace(
                "<",
                "&lt;",
            )
            .replace(
                ">",
                "&gt;",
            )
            .replace(
                "\"",
                "&quot;",
            )
            .replace(
                "'",
                "&#39;",
            )
}