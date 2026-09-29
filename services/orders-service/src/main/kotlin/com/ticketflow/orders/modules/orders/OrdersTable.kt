package com.ticketflow.orders.modules.orders

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestampWithTimeZone

object OrdersTable : Table("orders") {

    val id = uuid("id")

    val userId =
        uuid("user_id")
            .nullable()

    val inventoryId =
        uuid("inventory_id")

    val reservationId =
        uuid("reservation_id")
            .nullable()

    val paymentId =
        uuid("payment_id")
            .nullable()

    val amount =
        decimal(
            name = "amount",
            precision = 12,
            scale = 2,
        )

    val currency =
        varchar(
            name = "currency",
            length = 3,
        )

    val status =
        varchar(
            name = "status",
            length = 30,
        )

    val failureReason =
        varchar(
            name = "failure_reason",
            length = 500,
        ).nullable()

    val createdAt =
        timestampWithTimeZone("created_at")

    val updatedAt =
        timestampWithTimeZone("updated_at")

    override val primaryKey =
        PrimaryKey(id)
}