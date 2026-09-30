package com.ticketflow.notifications

import jakarta.mail.Message
import jakarta.mail.Session
import jakarta.mail.Transport
import jakarta.mail.internet.MimeMessage
import java.util.Properties

class Mailer(
    private val host: String,
    private val port: Int,
    private val from: String,
) {
    fun send(to: String, subject: String, body: String) {
        val properties = Properties().apply {
            put("mail.smtp.host", host)
            put("mail.smtp.port", port.toString())
            put("mail.smtp.auth", "false")
        }
        val session = Session.getInstance(properties)
        val message = MimeMessage(session)
        message.setFrom(from)
        message.setRecipients(Message.RecipientType.TO, to)
        message.setSubject(subject, "UTF-8")
        message.setText(body, "UTF-8")
        Transport.send(message)
    }
}
