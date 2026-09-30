package com.ticketflow.orders

import com.ticketflow.orders.clients.events.EventsClient
import com.ticketflow.orders.clients.payments.PaymentsClient
import com.ticketflow.orders.clients.tickets.TicketsClient
import com.ticketflow.orders.modules.orders.OrderRepository
import com.ticketflow.orders.modules.orders.OrderService
import com.ticketflow.orders.modules.orders.orderRoutes
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.routing.*
import io.ktor.server.response.respondText
import io.ktor.http.ContentType

/**
 * Configures the HTTP routes exposed by Orders Service and wires all
 * dependencies required by the order domain.
 */
fun Application.configureRouting() {
    val config = environment.config

    val ticketsClient = TicketsClient(
        httpClient = serviceHttpClient,
        baseUrl = config.property("services.tickets.baseUrl").getString(),
    )

    val internalSecret = config.propertyOrNull("services.internalSecret")?.getString()
        ?: System.getenv("INTERNAL_SERVICE_SECRET") ?: "ticketflow-internal-dev-secret"

    val paymentsClient = PaymentsClient(
        httpClient = serviceHttpClient,
        baseUrl = config.property("services.payments.baseUrl").getString(),
        internalSecret = internalSecret,
    )

    val eventsClient = EventsClient(
        httpClient = serviceHttpClient,
        baseUrl = config.property("services.events.baseUrl").getString(),
    )

    /*
     * Secret used to sign and validate TicketFlow QR payloads.
     *
     * The development value allows the local Docker environment to work
     * without additional configuration. Production should always provide
     * TICKET_QR_SECRET explicitly.
     */
    val qrSecret = config.propertyOrNull("security.qrSecret")
        ?.getString()
        ?: System.getenv("TICKET_QR_SECRET")
        ?: "ticketflow-qr-dev-secret"

    /*
     * Shared secret used exclusively for service-to-service endpoints.
     *
     * Payments Service sends this value when notifying Orders Service that
     * the payment provider has confirmed a payment.
     */

    val repository = OrderRepository()
    val orderService = OrderService(
        repository = repository,
        ticketsClient = ticketsClient,
        paymentsClient = paymentsClient,
        eventsClient = eventsClient,
        qrSecret = qrSecret,
    )

    routing {
        get("/metrics") {
            val orders = repository.findAll()
            val tickets = repository.allTicketStatuses()
            val confirmed = orders.count { it.status.name == "CONFIRMED" }
            val reserved = orders.count { it.status.name == "RESERVED" }
            val failed = orders.count { it.status.name == "FAILED" }
            val used = tickets.count { it.name == "USED" }
            val issued = tickets.count { it.name == "ISSUED" }
            val body = buildString {
                appendLine("# HELP ticketflow_orders_total Orders by status")
                appendLine("# TYPE ticketflow_orders_total gauge")
                appendLine("ticketflow_orders_total{status=\"confirmed\"} $confirmed")
                appendLine("ticketflow_orders_total{status=\"reserved\"} $reserved")
                appendLine("ticketflow_orders_total{status=\"failed\"} $failed")
                appendLine("# HELP ticketflow_checkins_total Used tickets")
                appendLine("# TYPE ticketflow_checkins_total gauge")
                appendLine("ticketflow_checkins_total $used")
                appendLine("ticketflow_tickets_pending_checkin $issued")
            }
            call.respondText(body, ContentType.Text.Plain)
        }
        orderRoutes(
            service = orderService,
            internalSecret = internalSecret,
        )
    }
}