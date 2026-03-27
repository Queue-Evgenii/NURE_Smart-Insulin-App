package ua.nure.smartinsulinbackend.service

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Service
import java.util.Date

@Service
class JwtService {
    private val secret = "your-256-bit-secret-key-must-be-very-long-and-secure"
    private val key = Keys.hmacShaKeyFor(secret.toByteArray())

    fun generateAccessToken(user: UserDetails): String = buildToken(user, 3600000) // 1 год
    fun generateRefreshToken(user: UserDetails): String = buildToken(user, 604800000) // 7 днів

    private fun buildToken(user: UserDetails, expiration: Long): String =
        Jwts.builder()
            .subject(user.username)
            .issuedAt(Date())
            .expiration(Date(System.currentTimeMillis() + expiration))
            .signWith(key)
            .compact()

    fun extractEmail(token: String): String? =
        Jwts.parser().verifyWith(key).build().parseSignedClaims(token).payload.subject
}