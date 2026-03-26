package ua.nure.smartinsulinbackend.dto

data class LoginRequest(val email: String, val password: String)
data class TokenResponse(val accessToken: String, val refreshToken: String)