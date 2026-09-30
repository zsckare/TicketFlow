package com.ticketflow.orders

import com.ticketflow.orders.clients.events.EventsClient
import com.ticketflow.orders.clients.payments.PaymentsClient
import com.ticketflow.orders.clients.tickets.TicketsClient
import com.ticketflow.orders.modules.orders.OrderRepository
import com.ticketflow.orders.modules.orders.OrderService
import com.ticketflow.orders.modules.orders.orderRoutes
import io.ktor.server.application.Application
import io.ktor.server.routing.routing

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

    val paymentsClient = PaymentsClient(
        httpClient = serviceHttpClient,
        baseUrl = config.property("services.payments.baseUrl").getString(),
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
    val internalSecret = config.propertyOrNull("services.internalSecret")
        ?.getString()
        ?: System.getenv("INTERNAL_SERVICE_SECRET")
        ?: "ticketflow-internal-dev-secret"

    val orderService = OrderService(
        repository = OrderRepository(),
        ticketsClient = ticketsClient,
        paymentsClient = paymentsClient,
        eventsClient = eventsClient,
        qrSecret = qrSecret,
    )

    routing {
        orderRoutes(
            service = orderService,
            internalSecret = internalSecret,
        )
    }
}