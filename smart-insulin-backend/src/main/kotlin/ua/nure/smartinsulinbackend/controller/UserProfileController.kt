package ua.nure.smartinsulinbackend.controller

import jakarta.validation.Valid
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import ua.nure.smartinsulinbackend.dto.UserProfileResponse
import ua.nure.smartinsulinbackend.dto.UserProfileUpdateRequest
import ua.nure.smartinsulinbackend.entity.User
import ua.nure.smartinsulinbackend.service.AdaptiveCoefficientService
import ua.nure.smartinsulinbackend.service.UserProfileService

@RestController
@RequestMapping("/api/profile")
class UserProfileController(
    private val userProfileService: UserProfileService,
    private val adaptiveCoefficientService: AdaptiveCoefficientService,
) {

    @GetMapping
    fun getProfile(@AuthenticationPrincipal user: User): UserProfileResponse =
        userProfileService.getProfile(user)

    @PutMapping
    fun updateProfile(
        @AuthenticationPrincipal user: User,
        @Valid @RequestBody request: UserProfileUpdateRequest,
    ): UserProfileResponse =
        userProfileService.updateProfile(user, request)

    /** Recompute adaptive ICR/ISF from the patient's own history (section 2.1.1). */
    @PostMapping("/recalculate-coefficients")
    fun recalculateCoefficients(
        @AuthenticationPrincipal user: User,
    ): AdaptiveCoefficientService.CoefficientUpdateResult =
        adaptiveCoefficientService.recalculateForUser(user.id)
}
