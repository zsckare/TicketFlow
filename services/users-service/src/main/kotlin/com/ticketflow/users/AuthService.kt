package com.ticketflow.users
import at.favre.lib.crypto.bcrypt.BCrypt
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.server.config.*
import java.time.Instant
import java.util.UUID
class AuthService(private val repo:UserRepository,config:ApplicationConfig){private val secret=config.property("jwt.secret").getString();private val issuer=config.property("jwt.issuer").getString();private val audience=config.property("jwt.audience").getString();private val minutes=config.property("jwt.expiresMinutes").getString().toLong();
 fun register(r:RegisterRequest):AuthResponse{val email=r.email.trim().lowercase();require(email.contains('@')){"Invalid email"};require(r.password.length>=8){"Password must have at least 8 characters"};if(repo.findByEmail(email)!=null)throw IllegalStateException("Email already registered");val hash=BCrypt.withDefaults().hashToString(12,r.password.toCharArray());return auth(repo.create(email,hash,r.firstName.trim(),r.lastName.trim()))}
 fun login(r:LoginRequest):AuthResponse{val u=repo.findByEmail(r.email.trim().lowercase())?:throw SecurityException("Invalid credentials");if(u.response.status!=UserStatus.ACTIVE)throw SecurityException("User is not active");if(!BCrypt.verifyer().verify(r.password.toCharArray(),u.passwordHash).verified)throw SecurityException("Invalid credentials");return auth(u.response)}
 private fun auth(u:UserResponse):AuthResponse{val now=Instant.now();val exp=now.plusSeconds(minutes*60);val token=JWT.create().withIssuer(issuer).withAudience(audience).withSubject(u.id).withClaim("role",u.role.name).withClaim("email",u.email).withIssuedAt(now).withExpiresAt(exp).sign(Algorithm.HMAC256(secret));return AuthResponse(token,expiresIn=minutes*60,user=u)}
}