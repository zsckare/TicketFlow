package com.ticketflow.orders.modules.orders

class OrderNotFoundException(
    orderId: String,
) : RuntimeException(
    "Order not found: $orderId"
)

class TicketNotAvailableException :
    RuntimeException(
        "Ticket is not available"
    )