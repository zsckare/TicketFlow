package com.ticketflow.notifications.kafka

import com.ticketflow.notifications.NotificationRepository
import io.ktor.server.application.*
import kotlinx.coroutines.*
import kotlinx.serialization.json.Json
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.StringDeserializer
import java.time.Duration
import java.util.Properties

fun Application.configureOrderConfirmedConsumer() {
    val props = Properties().apply {
        put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, environment.config.property("kafka.bootstrapServers").getString())
        put(ConsumerConfig.GROUP_ID_CONFIG, environment.config.property("kafka.groupId").getString())
        put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer::class.java.name)
        put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer::class.java.name)
        put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
        put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false")
    }
    val consumer = KafkaConsumer<String, String>(props)
    consumer.subscribe(listOf(environment.config.property("kafka.orderConfirmedTopic").getString()))
    val repository = NotificationRepository()
    val json = Json { ignoreUnknownKeys = true }
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    scope.launch {
        while (isActive) {
            try {
                val records = consumer.poll(Duration.ofSeconds(1))
                for (record in records) {
                    val event = json.decodeFromString<OrderConfirmedEvent>(record.value())
                    repository.createOrderConfirmed(event)
                }
                if (!records.isEmpty) consumer.commitSync()
            } catch (e: Exception) { environment.log.error("OrderConfirmed consumer failed", e); delay(1000) }
        }
    }
    monitor.subscribe(ApplicationStopped) { scope.cancel(); consumer.wakeup(); consumer.close() }
}
