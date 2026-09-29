package com.ticketflow.users
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.application.*
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.Database
fun Application.configureDatabase(){val c=environment.config;val h=HikariConfig().apply{jdbcUrl=c.property("database.jdbcUrl").getString();username=c.property("database.username").getString();password=c.property("database.password").getString();driverClassName="org.postgresql.Driver";maximumPoolSize=c.property("database.maximumPoolSize").getString().toInt();isAutoCommit=false;transactionIsolation="TRANSACTION_READ_COMMITTED"};val ds=HikariDataSource(h);Flyway.configure().dataSource(ds).locations("classpath:db/migration").load().migrate();Database.connect(ds)}