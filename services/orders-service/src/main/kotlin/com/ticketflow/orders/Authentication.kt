package com.ticketflow.orders

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.server.application.Application
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.JWTCredential
import io.ktor.server.application.install

fun Application.configureAuthentication() {
    val config = environment.config
    val secret = config.property("jwt.secret").getString()
    val issuer = config.property("jwt.issuer").getString()
    val audience = config.property("jwt.audience").getString()

    install(Authentication) {
        jwt("auth-jwt") {
            realm = "ticketflow"
            verifier(
                JWT.require(Algorithm.HMAC256(secret))
                    .withIssuer(issuer)
                    .withAudience(audience)
                    .build()
            )
            validate { credential: JWTCredential ->
                if (!credential.payload.subject.isNullOrBlank()) {
                    JWTPrincipal(credential.payload)
                } else {
                    null
                }
            }
        }
    }
}
