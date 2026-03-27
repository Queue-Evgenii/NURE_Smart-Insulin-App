package ua.nure.smartinsulinbackend.service

import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Service
import java.util.Date

@Service
class JwtService(
    @Value("\${jwt.secret}") private val secret: String,
    @Value("\${jwt.access-token-expiration:3600000}") private val accessTokenExpiration: Long,
    @Value("\${jwt.refresh-token-expiration:604800000}") private val refreshTokenExpiration: Long,
) {
    private val key by lazy { Keys.hmacShaKeyFor(secret.toByteArray()) }

    fun generateAccessToken(user: UserDetails): String = buildToken(user, accessTokenExpiration)
    fun generateRefreshToken(user: UserDetails): String = buildToken(user, refreshTokenExpiration)
    fun getRefreshTokenExpiration(): Long = refreshTokenExpiration

    private fun buildToken(user: UserDetails, expiration: Long): String =
        Jwts.builder()
            .subject(user.username)
            .issuedAt(Date())
            .expiration(Date(System.currentTimeMillis() + expiration))
            .signWith(key)
            .compact()

    fun extractEmail(token: String): String? =
        Jwts.parser().verifyWith(key).build().parseSignedClaims(token).payload.subject

    fun isTokenValid(token: String, userDetails: UserDetails): Boolean =
        try {
            extractEmail(token) == userDetails.username
        } catch (e: JwtException) {
            false
        }
}