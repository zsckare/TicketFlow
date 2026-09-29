package com.ticketflow.payments
import com.zaxxer.hikari.*
import io.ktor.server.application.*
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.Database
fun Application.configureDatabase(){val c=environment.config;val h=HikariConfig().apply{jdbcUrl=c.property("database.jdbcUrl").getString();username=c.property("database.username").getString();password=c.property("database.password").getString();driverClassName="org.postgresql.Driver";isAutoCommit=false;transactionIsolation="TRANSACTION_READ_COMMITTED"};val ds=HikariDataSource(h);Flyway.configure().dataSource(ds).load().migrate();Database.connect(ds)}