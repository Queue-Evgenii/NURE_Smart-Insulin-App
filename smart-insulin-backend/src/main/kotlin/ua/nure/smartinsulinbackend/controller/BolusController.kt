package ua.nure.smartinsulinbackend.controller

import org.springframework.web.bind.annotation.*
import org.springframework.security.core.annotation.AuthenticationPrincipal
import ua.nure.smartinsulinbackend.dto.BolusCalculationRequest
import ua.nure.smartinsulinbackend.dto.BolusCalculationResponse
import ua.nure.smartinsulinbackend.dto.BolusRecommendationRequest
import ua.nure.smartinsulinbackend.dto.BolusRecommendationResponse
import ua.nure.smartinsulinbackend.dto.CarbsEstimateRequest
import ua.nure.smartinsulinbackend.dto.CarbsEstimateResponse
import ua.nure.smartinsulinbackend.entity.User
import ua.nure.smartinsulinbackend.service.BolusCalculationService
import ua.nure.smartinsulinbackend.service.GeminiService

@RestController
@RequestMapping("/api/bolus")
class BolusController(
    private val bolusCalculationService: BolusCalculationService,
    private val geminiService: GeminiService,
) {

    @PostMapping("/calculate")
    fun calculate(
        @AuthenticationPrincipal user: User,
        @RequestBody request: BolusCalculationRequest,
    ): BolusCalculationResponse =
        bolusCalculationService.calculate(user, request)

    @PostMapping("/estimate-carbs")
    fun estimateCarbs(
        @AuthenticationPrincipal user: User,
        @RequestBody request: CarbsEstimateRequest,
    ): CarbsEstimateResponse =
        geminiService.estimateCarbs(request.mealDescription)

    @PostMapping("/recommendation")
    fun recommendation(
        @AuthenticationPrincipal user: User,
        @RequestBody request: BolusRecommendationRequest,
    ): BolusRecommendationResponse =
        geminiService.generateRecommendation(request)
}
