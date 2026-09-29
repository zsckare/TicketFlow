package com.ticketflow

import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlin.test.Test
import kotlin.test.assertEquals

class ServerTest {

    @Test
    fun `unknown route returns not found`() = testApplication {
        val response = client.get("/")

        assertEquals(
            HttpStatusCode.NotFound,
            response.status,
        )
    }
}