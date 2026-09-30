package com.ticketflow.users

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.http.Cookie
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.util.UUID

private const val REFRESH_COOKIE_NAME = "ticketflow.refreshToken"
private const val REFRESH_COOKIE_PATH = "/api/auth"

fun Application.configureRouting() {
    val repo = UserRepository()
    val refreshTokens = RefreshTokenRepository()
    val auth = AuthService(repo, refreshTokens, environment.config)
    val config = environment.config

    val refreshDays = config
        .property("jwt.refreshExpiresDays")
        .getString()
        .toLong()

    val refreshCookieSecure = config
        .property("jwt.refreshCookieSecure")
        .getString()
        .toBoolean()

    install(Authentication) {
        jwt("auth-jwt") {
            realm = "ticketflow"
            verifier(
                JWT.require(
                    Algorithm.HMAC256(
                        config.property("jwt.secret").getString(),
                    ),
                )
                    .withIssuer(config.property("jwt.issuer").getString())
                    .withAudience(config.property("jwt.audience").getString())
                    .build(),
            )
            validate {
                if (it.payload.subject != null) {
                    JWTPrincipal(it.payload)
                } else {
                    null
                }
            }
        }
    }

    routing {
        get("/health") {
            call.respond(
                mapOf("status" to "UP"),
            )
        }

        post("/auth/register") {
            val session = auth.register(call.receive())
            call.setRefreshCookie(
                token = session.refreshToken,
                refreshDays = refreshDays,
                secure = refreshCookieSecure,
            )
            call.respond(
                HttpStatusCode.Created,
                session.response,
            )
        }

        post("/auth/login") {
            val session = auth.login(call.receive())
            call.setRefreshCookie(
                token = session.refreshToken,
                refreshDays = refreshDays,
                secure = refreshCookieSecure,
            )
            call.respond(session.response)
        }

        /**
         * No access token is required here. The HttpOnly cookie is the
         * credential used to create a fresh short-lived access token.
         */
        post("/auth/refresh") {
            val rawRefreshToken = call.request.cookies[REFRESH_COOKIE_NAME]
                ?: throw SecurityException("Refresh token is missing")

            val session = auth.refresh(rawRefreshToken)

            call.setRefreshCookie(
                token = session.refreshToken,
                refreshDays = refreshDays,
                secure = refreshCookieSecure,
            )

            call.respond(session.response)
        }

        post("/auth/logout") {
            auth.logout(
                call.request.cookies[REFRESH_COOKIE_NAME],
            )

            call.clearRefreshCookie(
                secure = refreshCookieSecure,
            )

            call.respond(HttpStatusCode.NoContent)
        }

        authenticate("auth-jwt") {
            get("/users/me") {
                val id = UUID.fromString(
                    call.principal<JWTPrincipal>()!!.payload.subject,
                )

                call.respond(
                    repo.findById(id)
                        ?: return@get call.respond(HttpStatusCode.NotFound),
                )
            }

            get("/users") {
                requireAdmin(call)
                call.respond(repo.findAll())
            }

            get("/users/{id}") {
                requireAdmin(call)
                val id = UUID.fromString(call.parameters["id"])
                call.respond(
                    repo.findById(id)
                        ?: return@get call.respond(HttpStatusCode.NotFound),
                )
            }

            patch("/users/{id}/status") {
                requireAdmin(call)
                val id = UUID.fromString(call.parameters["id"])
                val request = call.receive<UpdateStatusRequest>()
                call.respond(
                    repo.updateStatus(id, request.status)
                        ?: return@patch call.respond(HttpStatusCode.NotFound),
                )
            }
        }
    }
}

/**
 * The browser never exposes this value to JavaScript.
 * SameSite=Lax is sufficient because TicketFlow uses a same-site web/API flow.
 */
private fun ApplicationCall.setRefreshCookie(
    token: String,
    refreshDays: Long,
    secure: Boolean,
) {
    response.cookies.append(
        Cookie(
            name = REFRESH_COOKIE_NAME,
            value = token,
            path = REFRESH_COOKIE_PATH,
            maxAge = (refreshDays * 24 * 60 * 60).toInt(),
            secure = secure,
            httpOnly = true,
            extensions = mapOf("SameSite" to "Lax"),
        ),
    )
}

private fun ApplicationCall.clearRefreshCookie(
    secure: Boolean,
) {
    response.cookies.append(
        Cookie(
            name = REFRESH_COOKIE_NAME,
            value = "",
            path = REFRESH_COOKIE_PATH,
            maxAge = 0,
            secure = secure,
            httpOnly = true,
            extensions = mapOf("SameSite" to "Lax"),
        ),
    )
}

private suspend fun requireAdmin(call: ApplicationCall) {
    if (
        call.principal<JWTPrincipal>()
            ?.payload
            ?.getClaim("role")
            ?.asString() != UserRole.ADMIN.name
    ) {
        throw SecurityException("Admin role required")
    }
}
