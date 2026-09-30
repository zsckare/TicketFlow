package com.ticketflow.notifications.kafka

import com.ticketflow.notifications.Mailer
import com.ticketflow.notifications.NotificationRepository
import com.ticketflow.notifications.NotificationStatus
import com.ticketflow.notifications.OrderConfirmationEmail
import io.ktor.server.application.*
import kotlinx.coroutines.*
import kotlinx.serialization.json.Json
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.StringDeserializer
import java.time.Duration
import java.util.Properties
import java.util.UUID

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
    val mailer = Mailer(
        environment.config.property("mail.host").getString(),
        environment.config.property("mail.port").getString().toInt(),
        environment.config.property("mail.from").getString(),
    )
    val webBaseUrl = environment.config.property("web.baseUrl").getString()
    val json = Json { ignoreUnknownKeys = true }
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    scope.launch {
        while (isActive) {
            try {
                val records = consumer.poll(Duration.ofSeconds(1))

                for (record in records) {
                    val event = json.decodeFromString<OrderConfirmedEvent>(record.value())
                    val eventId = UUID.fromString(event.eventId)
                    val existing = repository.findOrderConfirmed(eventId)

                    // Kafka can redeliver records. A SENT notification is final and
                    // must never cause another email to be delivered.
                    if (existing?.status == NotificationStatus.SENT) continue

                    val content = OrderConfirmationEmail.build(event, webBaseUrl)
                    repository.prepareOrderConfirmed(event, content.subject, content.html)

                    try {
                        mailer.send(event.userEmail, content.subject, content.html)
                        repository.updateStatusForEvent(eventId, NotificationStatus.SENT)
                    } catch (mailError: Exception) {
                        repository.updateStatusForEvent(eventId, NotificationStatus.FAILED)
                        environment.log.error("Email delivery failed for order ${event.orderId}", mailError)
                        // Do not commit this batch. Kafka will redeliver and FAILED
                        // notifications are intentionally eligible for retry.
                        throw mailError
                    }
                }

                if (!records.isEmpty) consumer.commitSync()
            } catch (e: Exception) {
                environment.log.error("OrderConfirmed consumer failed", e)
                delay(1000)
            }
        }
    }

    monitor.subscribe(ApplicationStopped) {
        scope.cancel()
        consumer.wakeup()
        consumer.close()
    }
}
