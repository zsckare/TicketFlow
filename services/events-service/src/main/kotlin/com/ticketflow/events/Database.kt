package com.ticketflow.events

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.application.Application
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.Database

/**
 * Configura la infraestructura de base de datos del Events Service.
 *
 * Responsabilidades:
 * - Crear el pool de conexiones con HikariCP.
 * - Ejecutar las migraciones de Flyway.
 * - Conectar Exposed con PostgreSQL.
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

        // Evitamos que las conexiones permanezcan abiertas
        // indefinidamente cuando no están siendo utilizadas.
        isAutoCommit = false

        transactionIsolation = "TRANSACTION_REPEATABLE_READ"

        validate()
    }

    val dataSource = HikariDataSource(hikariConfig)

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
     * Exposed utiliza el mismo pool de conexiones.
     *
     * Exposed NO creará las tablas.
     */
    Database.connect(dataSource)
}