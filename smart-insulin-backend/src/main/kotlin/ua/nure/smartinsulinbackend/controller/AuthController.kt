package ua.nure.smartinsulinbackend.controller

import org.springframework.web.bind.annotation.*
import ua.nure.smartinsulinbackend.service.JwtService
import ua.nure.smartinsulinbackend.dto.*
import ua.nure.smartinsulinbackend.service.AuthService

@RestController
@RequestMapping("/api/auth")
class AuthController(private val authService: AuthService) {

    @PostMapping("/login")
    fun login(@RequestBody request: LoginRequest): TokenResponse {
        return authService.login(request)
    }
}