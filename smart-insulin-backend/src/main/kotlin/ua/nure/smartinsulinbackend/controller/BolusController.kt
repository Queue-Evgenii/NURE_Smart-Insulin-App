package ua.nure.smartinsulinbackend.controller

import org.springframework.web.bind.annotation.*
import org.springframework.security.core.annotation.AuthenticationPrincipal
import ua.nure.smartinsulinbackend.dto.BolusCalculationRequest
import ua.nure.smartinsulinbackend.dto.BolusCalculationResponse
import ua.nure.smartinsulinbackend.entity.User
import ua.nure.smartinsulinbackend.service.BolusCalculationService

@RestController
@RequestMapping("/api/bolus")
class BolusController(private val bolusCalculationService: BolusCalculationService) {

    @PostMapping("/calculate")
    fun calculate(
        @AuthenticationPrincipal user: User,
        @RequestBody request: BolusCalculationRequest,
    ): BolusCalculationResponse =
        bolusCalculationService.calculate(user, request)
}
