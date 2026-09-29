package com.ticketflow.orders.outbox

import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopped
import kotlinx.coroutines.*
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.Properties
import java.util.UUID

data class PendingOutboxEvent(val id: UUID, val topic: String, val key: String, val payload: String, val attempts: Int)

fun Application.configureOutboxPublisher() {
    val bootstrapServers = environment.config.property("kafka.bootstrapServers").getString()
    val intervalMs = environment.config.property("outbox.pollIntervalMs").getString().toLong()
    val batchSize = environment.config.property("outbox.batchSize").getString().toInt()
    val props = Properties().apply {
        put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers)
        put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer::class.java.name)
        put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer::class.java.name)
        put(ProducerConfig.ACKS_CONFIG, "all")
        put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, "true")
    }
    val producer = KafkaProducer<String, String>(props)
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    scope.launch {
        while (isActive) {
            try { publishBatch(producer, batchSize) }
            catch (e: Exception) { environment.log.error("Outbox publisher iteration failed", e) }
            delay(intervalMs)
        }
    }
    monitor.subscribe(ApplicationStopped) { scope.cancel(); producer.close() }
}

private fun publishBatch(producer: KafkaProducer<String, String>, batchSize: Int) {
    val pending = transaction {
        OutboxEventsTable.selectAll()
            .where { OutboxEventsTable.publishedAt.isNull() }
            .orderBy(OutboxEventsTable.createdAt to SortOrder.ASC)
            .limit(batchSize)
            .map { PendingOutboxEvent(it[OutboxEventsTable.id], it[OutboxEventsTable.topic], it[OutboxEventsTable.eventKey], it[OutboxEventsTable.payload], it[OutboxEventsTable.attempts]) }
    }
    pending.forEach { event ->
        try {
            producer.send(ProducerRecord(event.topic, event.key, event.payload)).get()
            transaction { OutboxEventsTable.update({ OutboxEventsTable.id eq event.id }) { it[publishedAt] = OffsetDateTime.now(ZoneOffset.UTC); it[attempts] = event.attempts + 1; it[lastError] = null } }
        } catch (e: Exception) {
            transaction { OutboxEventsTable.update({ OutboxEventsTable.id eq event.id }) { it[attempts] = event.attempts + 1; it[lastError] = (e.message ?: e.javaClass.simpleName).take(1000) } }
        }
    }
}
