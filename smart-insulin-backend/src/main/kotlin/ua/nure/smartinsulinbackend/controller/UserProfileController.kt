package ua.nure.smartinsulinbackend.controller

import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import ua.nure.smartinsulinbackend.dto.UserProfileResponse
import ua.nure.smartinsulinbackend.dto.UserProfileUpdateRequest
import ua.nure.smartinsulinbackend.entity.User
import ua.nure.smartinsulinbackend.service.UserProfileService

@RestController
@RequestMapping("/api/profile")
class UserProfileController(private val userProfileService: UserProfileService) {

    @GetMapping
    fun getProfile(@AuthenticationPrincipal user: User): UserProfileResponse =
        userProfileService.getProfile(user)

    @PutMapping
    fun updateProfile(
        @AuthenticationPrincipal user: User,
        @RequestBody request: UserProfileUpdateRequest,
    ): UserProfileResponse =
        userProfileService.updateProfile(user, request)
}
