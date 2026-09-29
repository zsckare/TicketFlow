package com.ticketflow.orders

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.application.Application
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.Database

/**
 * Configures the Orders Service database.
 *
 * Important:
 * - Orders Service owns its own PostgreSQL database.
 * - Flyway owns the schema.
 * - Other microservices must never access this database directly.
 */
fun Application.configureDatabase() {
    val jdbcUrl =
        environment.config.property("database.jdbcUrl").getString()

    val username =
        environment.config.property("database.username").getString()

    val password =
        environment.config.property("database.password").getString()

    val maximumPoolSize =
        environment.config
            .property("database.maximumPoolSize")
            .getString()
            .toInt()

    val hikariConfig = HikariConfig().apply {
        this.jdbcUrl = jdbcUrl
        this.username = username
        this.password = password

        driverClassName = "org.postgresql.Driver"

        this.maximumPoolSize = maximumPoolSize

        isAutoCommit = false

        transactionIsolation = "TRANSACTION_READ_COMMITTED"

        validate()
    }

    val dataSource = HikariDataSource(hikariConfig)

    // Flyway is the only component responsible for schema creation
    // and schema migrations.
    Flyway.configure()
        .dataSource(dataSource)
        .locations("classpath:db/migration")
        .load()
        .migrate()

    Database.connect(dataSource)
}