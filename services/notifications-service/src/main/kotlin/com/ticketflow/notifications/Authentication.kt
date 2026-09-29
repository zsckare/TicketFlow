package com.ticketflow.notifications

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*

fun Application.configureAuthentication() {
    val secret = environment.config.property("jwt.secret").getString()
    val issuer = environment.config.property("jwt.issuer").getString()
    val audience = environment.config.property("jwt.audience").getString()
    install(Authentication) {
        jwt("auth-jwt") {
            realm = "ticketflow"
            verifier(JWT.require(Algorithm.HMAC256(secret)).withIssuer(issuer).withAudience(audience).build())
            validate { credential -> credential.payload.subject?.takeIf(String::isNotBlank)?.let { JWTPrincipal(credential.payload) } }
        }
    }
}
