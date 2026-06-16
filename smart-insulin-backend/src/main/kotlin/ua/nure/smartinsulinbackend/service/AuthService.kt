package ua.nure.smartinsulinbackend.service

import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ua.nure.smartinsulinbackend.dto.*
import ua.nure.smartinsulinbackend.entity.RefreshToken
import ua.nure.smartinsulinbackend.entity.User
import ua.nure.smartinsulinbackend.repository.RefreshTokenRepository
import ua.nure.smartinsulinbackend.repository.UserRepository
import java.security.MessageDigest
import java.time.Instant

@Service
@Transactional
class AuthService(
    private val userRepository: UserRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val jwtService: JwtService,
    private val authenticationManager: AuthenticationManager,
    private val passwordEncoder: PasswordEncoder,
) {
    fun login(request: LoginRequest): TokenResponse {
        authenticationManager.authenticate(
            UsernamePasswordAuthenticationToken(request.email, request.password)
        )
        val user = userRepository.findByEmail(request.email).orElseThrow()
        return issueTokens(user)
    }

    fun register(request: RegisterRequest): TokenResponse {
        require(!userRepository.findByEmail(request.email).isPresent) {
            "Email вже зареєстровано"
        }
        val user = userRepository.save(
            User(
                email = request.email,
                password = passwordEncoder.encode(request.password).toString(),
                fullName = request.fullName,
                diabetesType = request.diabetesType,
            )
        )
        return issueTokens(user)
    }

    fun refresh(request: RefreshTokenRequest): TokenResponse {
        val tokenHash = hashToken(request.refreshToken)
        val stored = refreshTokenRepository.findByToken(tokenHash)
            .orElseThrow { IllegalArgumentException("Невірний або прострочений refresh token") }
        if (stored.expiryDate.isBefore(Instant.now())) {
            refreshTokenRepository.delete(stored)
            throw IllegalArgumentException("Refresh token прострочено")
        }
        val user = stored.user
        refreshTokenRepository.delete(stored)
        refreshTokenRepository.flush()
        return issueTokens(user)
    }

    fun logout(request: RefreshTokenRequest) {
        val tokenHash = hashToken(request.refreshToken)
        refreshTokenRepository.findByToken(tokenHash)
            .ifPresent { refreshTokenRepository.delete(it) }
    }

    private fun issueTokens(user: User): TokenResponse {
        // Keep at most MAX_CONCURRENT_SESSIONS refresh tokens per user. Prune any expired
        // ones, then — if still at the limit — evict the oldest to make room for the new one.
        val now = Instant.now()
        val existing = refreshTokenRepository.findByUserOrderByCreatedAtAsc(user)
        val (expired, active) = existing.partition { it.expiryDate.isBefore(now) }
        val toDelete = expired.toMutableList()
        val overflow = active.size - (MAX_CONCURRENT_SESSIONS - 1)
        if (overflow > 0) toDelete += active.take(overflow)
        if (toDelete.isNotEmpty()) {
            refreshTokenRepository.deleteAll(toDelete)
            refreshTokenRepository.flush()
        }

        val accessToken = jwtService.generateAccessToken(user)
        val rawRefreshToken = jwtService.generateRefreshToken(user)
        refreshTokenRepository.save(
            RefreshToken(
                token = hashToken(rawRefreshToken),
                expiryDate = Instant.now().plusMillis(jwtService.getRefreshTokenExpiration()),
                user = user,
            )
        )
        return TokenResponse(accessToken, rawRefreshToken)
    }

    private fun hashToken(token: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(token.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
    }

    private companion object {
        /** Max simultaneous logged-in sessions (devices) per account. */
        const val MAX_CONCURRENT_SESSIONS = 3
    }
}