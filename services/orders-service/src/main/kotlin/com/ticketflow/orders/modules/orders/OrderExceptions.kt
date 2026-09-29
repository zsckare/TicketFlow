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

class InvalidOrderStateException(
    message: String,
) : RuntimeException(message)

class OrderOperationException(
    message: String,
) : RuntimeException(message)