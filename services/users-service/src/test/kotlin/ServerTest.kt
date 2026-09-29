package com.ticketflow.users

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlin.test.Test

class ServerTest {

    @Test
    fun `inspect invalid register response`() = testApplication {
        val response = client.post("/auth/register") {
            contentType(ContentType.Application.Json)

            setBody(
                """
                {
                    "email": ""
                }
                """.trimIndent(),
            )
        }

        println("STATUS = ${response.status}")
        println("BODY   = ${response.bodyAsText()}")
    }
}