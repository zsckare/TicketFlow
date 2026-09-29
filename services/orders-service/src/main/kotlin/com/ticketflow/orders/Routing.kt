package com.ticketflow.orders

import com.ticketflow.orders.clients.tickets.TicketsClient
import com.ticketflow.orders.modules.orders.OrderRepository
import com.ticketflow.orders.modules.orders.OrderService
import com.ticketflow.orders.modules.orders.orderRoutes
import io.ktor.server.application.Application
import io.ktor.server.routing.routing

fun Application.configureRouting() {

    val ticketsBaseUrl =
        environment.config
            .property("services.tickets.baseUrl")
            .getString()

    val ticketsClient =
        TicketsClient(
            httpClient = serviceHttpClient,
            baseUrl = ticketsBaseUrl,
        )

    val orderRepository =
        OrderRepository()

    val orderService =
        OrderService(
            repository = orderRepository,
            ticketsClient = ticketsClient,
        )

    routing {
        orderRoutes(orderService)
    }
}