package ua.nure.smartinsulinbackend.service

import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.stereotype.Service
import ua.nure.smartinsulinbackend.service.JwtService
import ua.nure.smartinsulinbackend.dto.*
import ua.nure.smartinsulinbackend.repository.UserRepository

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val jwtService: JwtService,
    private val authenticationManager: AuthenticationManager
) {
    fun login(request: LoginRequest): TokenResponse {
        authenticationManager.authenticate(
            UsernamePasswordAuthenticationToken(request.email, request.password)
        )

        val user = userRepository.findByEmail(request.email).orElseThrow()

        val accessToken = jwtService.generateAccessToken(user)
        val refreshToken = jwtService.generateRefreshToken(user)

        // Для диплома: тут варто зберегти RefreshToken у базу (таблиця refresh_tokens)

        return TokenResponse(accessToken, refreshToken)
    }
}