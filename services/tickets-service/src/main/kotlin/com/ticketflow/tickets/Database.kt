package com.ticketflow.tickets

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.application.Application
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.Database

/**
 * Configura la conexión de Tickets Service con SU propia
 * base de datos PostgreSQL.
 *
 * Tickets Service nunca debe conectarse a events_db.
 */
fun Application.configureDatabase() {

    val config =
        environment.config

    val hikariConfig =
        HikariConfig().apply {

            jdbcUrl =
                config.property(
                    "database.jdbcUrl",
                ).getString()

            username =
                config.property(
                    "database.username",
                ).getString()

            password =
                config.property(
                    "database.password",
                ).getString()

            maximumPoolSize =
                config.property(
                    "database.maximumPoolSize",
                ).getString().toInt()

            driverClassName =
                "org.postgresql.Driver"

            isAutoCommit =
                false

            transactionIsolation =
                "TRANSACTION_REPEATABLE_READ"

            validate()
        }

    val dataSource =
        HikariDataSource(
            hikariConfig,
        )

    /**
     * Flyway es el único responsable de modificar
     * el schema de PostgreSQL.
     */
    Flyway.configure()
        .dataSource(dataSource)
        .locations("classpath:db/migration")
        .load()
        .migrate()

    /**
     * Exposed utiliza el mismo DataSource.
     */
    Database.connect(
        dataSource,
    )
}