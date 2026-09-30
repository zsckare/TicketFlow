package com.ticketflow.users

import at.favre.lib.crypto.bcrypt.BCrypt
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.server.config.ApplicationConfig
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.Base64
import java.util.UUID

data class AuthSession(
    val response: AuthResponse,
    val refreshToken: String,
)

class AuthService(
    private val repo: UserRepository,
    private val refreshTokens: RefreshTokenRepository,
    config: ApplicationConfig,
) {
    private val secret = config.property("jwt.secret").getString()
    private val issuer = config.property("jwt.issuer").getString()
    private val audience = config.property("jwt.audience").getString()
    private val accessMinutes = config.property("jwt.expiresMinutes").getString().toLong()
    private val refreshDays = config.property("jwt.refreshExpiresDays").getString().toLong()

    private val secureRandom = SecureRandom()

    fun register(request: RegisterRequest): AuthSession {
        val email = request.email.trim().lowercase()

        require(email.contains('@')) {
            "Invalid email"
        }
        require(request.password.length >= 8) {
            "Password must have at least 8 characters"
        }
        if (repo.findByEmail(email) != null) {
            throw IllegalStateException("Email already registered")
        }

        val hash = BCrypt.withDefaults()
            .hashToString(12, request.password.toCharArray())

        val user = repo.create(
            email = email,
            hash = hash,
            first = request.firstName.trim(),
            last = request.lastName.trim(),
        )

        return createSession(user)
    }

    fun createInternalUser(request: CreateInternalUserRequest): UserResponse {
        require(request.role == UserRole.ADMIN || request.role == UserRole.STAFF) { "Internal users must be ADMIN or STAFF" }
        val email = request.email.trim().lowercase()
        require(email.contains('@')) { "Invalid email" }
        require(request.password.length >= 10) { "Password must have at least 10 characters" }
        require(request.firstName.isNotBlank() && request.lastName.isNotBlank()) { "Name is required" }
        if (repo.findByEmail(email) != null) throw IllegalStateException("Email already registered")
        val hash = BCrypt.withDefaults().hashToString(12, request.password.toCharArray())
        return repo.create(email, hash, request.firstName.trim(), request.lastName.trim(), request.role)
    }

    fun resetPassword(userId: UUID, password: String) {
        require(password.length >= 10) { "Password must have at least 10 characters" }
        val hash = BCrypt.withDefaults().hashToString(12, password.toCharArray())
        if (!repo.updatePassword(userId, hash)) throw NoSuchElementException("User not found")
        refreshTokens.revokeAllForUser(userId)
    }

    fun login(request: LoginRequest): AuthSession {
        val userRecord = repo.findByEmail(
            request.email.trim().lowercase(),
        ) ?: throw SecurityException("Invalid credentials")

        if (userRecord.response.status != UserStatus.ACTIVE) {
            throw SecurityException("User is not active")
        }

        if (
            !BCrypt.verifyer()
                .verify(
                    request.password.toCharArray(),
                    userRecord.passwordHash,
                )
                .verified
        ) {
            throw SecurityException("Invalid credentials")
        }

        return createSession(userRecord.response)
    }

    /**
     * Consumes the current refresh token and rotates it.
     * A refresh token can therefore only be used once.
     */
    fun refresh(rawRefreshToken: String): AuthSession {
        val hash = hashRefreshToken(rawRefreshToken)
        val stored = refreshTokens.findActiveByHash(hash)
            ?: throw SecurityException("Invalid or expired refresh token")

        val user = repo.findById(stored.userId)
            ?: throw SecurityException("User no longer exists")

        if (user.status != UserStatus.ACTIVE) {
            refreshTokens.revoke(stored.id)
            throw SecurityException("User is not active")
        }

        refreshTokens.revoke(stored.id)

        return createSession(user)
    }

    fun logout(rawRefreshToken: String?) {
        if (rawRefreshToken.isNullOrBlank()) {
            return
        }

        refreshTokens.revokeByHash(
            hashRefreshToken(rawRefreshToken),
        )
    }

    private fun createSession(user: UserResponse): AuthSession {
        val rawRefreshToken = generateRefreshToken()
        val refreshExpiresAt = OffsetDateTime.now(ZoneOffset.UTC)
            .plusDays(refreshDays)

        refreshTokens.create(
            userId = UUID.fromString(user.id),
            tokenHash = hashRefreshToken(rawRefreshToken),
            expiresAt = refreshExpiresAt,
        )

        return AuthSession(
            response = createAccessResponse(user),
            refreshToken = rawRefreshToken,
        )
    }

    private fun createAccessResponse(user: UserResponse): AuthResponse {
        val now = Instant.now()
        val expiresAt = now.plusSeconds(accessMinutes * 60)

        val token = JWT.create()
            .withIssuer(issuer)
            .withAudience(audience)
            .withSubject(user.id)
            .withClaim("role", user.role.name)
            .withClaim("email", user.email)
            .withIssuedAt(now)
            .withExpiresAt(expiresAt)
            .sign(Algorithm.HMAC256(secret))

        return AuthResponse(
            accessToken = token,
            expiresIn = accessMinutes * 60,
            user = user,
        )
    }

    private fun generateRefreshToken(): String {
        val bytes = ByteArray(48)
        secureRandom.nextBytes(bytes)

        return Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(bytes)
    }

    private fun hashRefreshToken(value: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(Charsets.UTF_8))
            .joinToString("") { byte ->
                "%02x".format(byte.toInt() and 0xff)
            }
}
