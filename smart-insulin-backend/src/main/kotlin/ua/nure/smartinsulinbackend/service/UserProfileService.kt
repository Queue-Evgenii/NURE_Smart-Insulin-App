package ua.nure.smartinsulinbackend.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ua.nure.smartinsulinbackend.dto.UserProfileResponse
import ua.nure.smartinsulinbackend.dto.UserProfileUpdateRequest
import ua.nure.smartinsulinbackend.entity.User
import ua.nure.smartinsulinbackend.entity.UserProfile
import ua.nure.smartinsulinbackend.repository.UserProfileRepository
import ua.nure.smartinsulinbackend.repository.UserRepository
import java.time.Instant

@Service
@Transactional
class UserProfileService(
    private val userProfileRepository: UserProfileRepository,
    private val userRepository: UserRepository,
) {

    @Transactional(readOnly = true)
    fun getProfile(user: User): UserProfileResponse {
        val profile = userProfileRepository.findByUserId(user.id).orElse(null)
        return UserProfileResponse(
            email = user.username,
            fullName = user.fullName,
            diabetesType = user.diabetesType,
            weightKg = profile?.weightKg,
            heightCm = profile?.heightCm,
            insulinSensitivityFactor = profile?.insulinSensitivityFactor,
            insulinToCarbRatio = profile?.insulinToCarbRatio,
            targetGlucoseMin = profile?.targetGlucoseMin,
            targetGlucoseMax = profile?.targetGlucoseMax,
            durationOfInsulinAction = profile?.durationOfInsulinAction,
            basalInsulinType = profile?.basalInsulinType,
            bolusInsulinType = profile?.bolusInsulinType,
        )
    }

    fun updateProfile(user: User, request: UserProfileUpdateRequest): UserProfileResponse {
        // Update User entity fields if provided
        if (request.fullName != null || request.diabetesType != null) {
            val updatedUser = User(
                id = user.id,
                email = user.username,
                password = user.password,
                fullName = request.fullName ?: user.fullName,
                diabetesType = request.diabetesType ?: user.diabetesType,
            )
            userRepository.save(updatedUser)
        }

        val existing = userProfileRepository.findByUserId(user.id).orElse(null)
        val profile = if (existing != null) {
            UserProfile(
                id = existing.id,
                user = user,
                weightKg = request.weightKg ?: existing.weightKg,
                heightCm = request.heightCm ?: existing.heightCm,
                insulinSensitivityFactor = request.insulinSensitivityFactor ?: existing.insulinSensitivityFactor,
                insulinToCarbRatio = request.insulinToCarbRatio ?: existing.insulinToCarbRatio,
                targetGlucoseMin = request.targetGlucoseMin ?: existing.targetGlucoseMin,
                targetGlucoseMax = request.targetGlucoseMax ?: existing.targetGlucoseMax,
                durationOfInsulinAction = request.durationOfInsulinAction ?: existing.durationOfInsulinAction,
                basalInsulinType = request.basalInsulinType ?: existing.basalInsulinType,
                bolusInsulinType = request.bolusInsulinType ?: existing.bolusInsulinType,
                createdAt = existing.createdAt,
                updatedAt = Instant.now(),
            )
        } else {
            UserProfile(
                user = user,
                weightKg = request.weightKg,
                heightCm = request.heightCm,
                insulinSensitivityFactor = request.insulinSensitivityFactor,
                insulinToCarbRatio = request.insulinToCarbRatio,
                targetGlucoseMin = request.targetGlucoseMin,
                targetGlucoseMax = request.targetGlucoseMax,
                durationOfInsulinAction = request.durationOfInsulinAction,
                basalInsulinType = request.basalInsulinType,
                bolusInsulinType = request.bolusInsulinType,
                updatedAt = Instant.now(),
            )
        }
        userProfileRepository.save(profile)

        val refreshedUser = userRepository.findById(user.id).orElseThrow()
        return getProfile(refreshedUser)
    }
}
