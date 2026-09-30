package com.ticketflow.orders.modules.orders

import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import net.sf.jasperreports.engine.JREmptyDataSource
import net.sf.jasperreports.engine.JRException
import net.sf.jasperreports.engine.JasperCompileManager
import net.sf.jasperreports.engine.JasperExportManager
import net.sf.jasperreports.engine.JasperFillManager
import java.awt.Color
import java.awt.Image
import java.awt.image.BufferedImage
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Renders the customer-facing ticket from a JRXML template.
 *
 * Authorization and ticket enrichment happen in OrderService. This class is
 * deliberately presentation-only: it never reads the database and never
 * creates a new admission credential. The QR contains the signed payload that
 * OrderService already generated for the issued ticket.
 */
object TicketPdfService {
    private const val TEMPLATE = "/reports/tickets/ticket.jrxml"
    private val locale = Locale.forLanguageTag("es-MX")

    fun render(
        ticket: IssuedTicketResponse,
        unitPrice: String,
        currency: String,
    ): ByteArray {
        val resource = checkNotNull(javaClass.getResourceAsStream(TEMPLATE)) {
            "Ticket Jasper report resource was not found: $TEMPLATE"
        }

        val report = try {
            resource.use { JasperCompileManager.compileReport(it) }
        } catch (error: JRException) {
            throw IllegalStateException("Ticket Jasper report could not be compiled.", error)
        }

        val params = mapOf<String, Any?>(
            "EVENT_NAME" to (ticket.eventName ?: "Evento TicketFlow"),
            "EVENT_DATE" to formattedEventDate(ticket),
            "VENUE_NAME" to (ticket.venueName ?: "Venue por confirmar"),
            "VENUE_ADDRESS" to listOfNotNull(ticket.venueAddress, ticket.venueCity)
                .filter { it.isNotBlank() }
                .joinToString(" • ")
                .ifBlank { "Ubicación por confirmar" },
            "SECTION_NAME" to (ticket.sectionName ?: "General"),
            "ACCESS_LABEL" to accessLabel(ticket),
            "PRICE" to formatMoney(unitPrice, currency),
            "ORDER_NUMBER" to ticket.orderId.take(8).uppercase(),
            "TICKET_NUMBER" to ticket.id.take(8).uppercase(),
            "STATUS" to statusLabel(ticket.status),
            "QR_IMAGE" to ticket.qrPayload?.let(::qrImage),
            "SHOW_QR" to (ticket.status == IssuedTicketStatus.ISSUED && ticket.qrPayload != null),
            "GENERATED_AT" to OffsetDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")),
        )

        val print = JasperFillManager.fillReport(report, params, JREmptyDataSource(1))
        return JasperExportManager.exportReportToPdf(print)
    }

    private fun formattedEventDate(ticket: IssuedTicketResponse): String {
        val startsAt = ticket.eventStartsAt ?: return "Fecha por confirmar"
        val zone = runCatching { ZoneId.of(ticket.venueTimezone ?: "America/Monterrey") }
            .getOrDefault(ZoneId.of("America/Monterrey"))
        return runCatching {
            OffsetDateTime.parse(startsAt)
                .atZoneSameInstant(zone)
                .format(DateTimeFormatter.ofPattern("EEEE d 'de' MMMM 'de' yyyy • hh:mm a", locale))
        }.getOrDefault("Fecha por confirmar")
    }

    private fun accessLabel(ticket: IssuedTicketResponse): String =
        if (!ticket.seatLabel.isNullOrBlank()) "Asiento ${ticket.seatLabel}"
        else if (ticket.sectionType == "GENERAL_ADMISSION") "Admisión general"
        else "Acceso general"

    private fun statusLabel(status: IssuedTicketStatus): String = when (status) {
        IssuedTicketStatus.ISSUED -> "VÁLIDO"
        IssuedTicketStatus.USED -> "UTILIZADO"
        IssuedTicketStatus.CANCELLED -> "CANCELADO"
    }

    private fun formatMoney(value: String, currency: String): String {
        val amount = value.toBigDecimalOrNull() ?: BigDecimal.ZERO
        val formatter = NumberFormat.getCurrencyInstance(locale)
        return "${formatter.format(amount)} $currency"
    }

    /** Creates a raster QR that Jasper can embed reliably in the PDF. */
    private fun qrImage(payload: String): Image {
        val matrix = QRCodeWriter().encode(payload, BarcodeFormat.QR_CODE, 360, 360)
        val image = BufferedImage(matrix.width, matrix.height, BufferedImage.TYPE_INT_RGB)
        for (y in 0 until matrix.height) {
            for (x in 0 until matrix.width) {
                image.setRGB(x, y, if (matrix[x, y]) Color.BLACK.rgb else Color.WHITE.rgb)
            }
        }
        return image
    }
}
