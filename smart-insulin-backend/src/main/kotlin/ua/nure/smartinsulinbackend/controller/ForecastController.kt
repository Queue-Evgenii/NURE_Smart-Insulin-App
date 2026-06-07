package ua.nure.smartinsulinbackend.controller

import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import ua.nure.smartinsulinbackend.dto.ForecastRequest
import ua.nure.smartinsulinbackend.dto.ForecastResponse
import ua.nure.smartinsulinbackend.entity.User
import ua.nure.smartinsulinbackend.service.GlucoseForecastService

@RestController
@RequestMapping("/api/forecast")
class ForecastController(private val glucoseForecastService: GlucoseForecastService) {

    /** Predict short-term glucose trajectory (section 2.2). */
    @PostMapping
    fun forecast(
        @AuthenticationPrincipal user: User,
        @RequestBody request: ForecastRequest,
    ): ForecastResponse =
        glucoseForecastService.forecast(user, request)
}
