package ua.nure.smartinsulinbackend.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.ObjectMapper
import ua.nure.smartinsulinbackend.dto.ActivityCoefficientsDto
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
    private val mapper: ObjectMapper,
) {

    @Transactional(readOnly = true)
    fun getProfile(user: User): UserProfileResponse {
        val profile = userProfileRepository.findByUserId(user.id).orElse(null)
        val coefficients = profile?.activityCoefficients
            ?.let { runCatching { mapper.readValue(it, ActivityCoefficientsDto::class.java) }.getOrNull() }
            ?: ActivityCoefficientsDto.DEFAULT

        val weightKg = profile?.weightKg
        val tdd = weightKg?.let { it * 0.5 }
        val usingWeightEstimation = profile?.insulinSensitivityFactor == null
            && profile?.insulinToCarbRatio == null
            && tdd != null

        return UserProfileResponse(
            email                    = user.username,
            fullName                 = user.fullName,
            diabetesType             = user.diabetesType,
            weightKg                 = profile?.weightKg,
            heightCm                 = profile?.heightCm,
            insulinSensitivityFactor = profile?.insulinSensitivityFactor,
            insulinToCarbRatio       = profile?.insulinToCarbRatio,
            targetGlucoseMin         = profile?.targetGlucoseMin,
            targetGlucoseMax         = profile?.targetGlucoseMax,
            durationOfInsulinAction  = profile?.durationOfInsulinAction,
            insulinResistanceFactor  = profile?.insulinResistanceFactor ?: 1.0,
            carbAbsorptionMinutes    = profile?.carbAbsorptionMinutes ?: 120,
            activityCoefficients     = coefficients,
            usingWeightEstimation    = usingWeightEstimation,
            basalInsulinType         = profile?.basalInsulinType,
            bolusInsulinType         = profile?.bolusInsulinType,
            usingAdaptiveCoefficients = profile?.usingAdaptiveCoefficients ?: false,
            lastCoefficientUpdate    = profile?.lastCoefficientUpdate,
        )
    }

    fun updateProfile(user: User, request: UserProfileUpdateRequest): UserProfileResponse {
        if (request.fullName != null || request.diabetesType != null) {
            val updatedUser = User(
                id           = user.id,
                email        = user.username,
                password     = user.password,
                fullName     = request.fullName ?: user.fullName,
                diabetesType = request.diabetesType ?: user.diabetesType,
            )
            userRepository.save(updatedUser)
        }

        val existing = userProfileRepository.findByUserId(user.id).orElse(null)

        val newCoefficientsJson: String? = request.activityCoefficients
            ?.let { mapper.writeValueAsString(it) }

        val profile = if (existing != null) {
            UserProfile(
                id                       = existing.id,
                user                     = user,
                weightKg                 = request.weightKg ?: existing.weightKg,
                heightCm                 = request.heightCm ?: existing.heightCm,
                insulinSensitivityFactor = request.insulinSensitivityFactor ?: existing.insulinSensitivityFactor,
                insulinToCarbRatio       = request.insulinToCarbRatio ?: existing.insulinToCarbRatio,
                targetGlucoseMin         = request.targetGlucoseMin ?: existing.targetGlucoseMin,
                targetGlucoseMax         = request.targetGlucoseMax ?: existing.targetGlucoseMax,
                durationOfInsulinAction  = request.durationOfInsulinAction ?: existing.durationOfInsulinAction,
                insulinResistanceFactor  = request.insulinResistanceFactor ?: existing.insulinResistanceFactor,
                carbAbsorptionMinutes    = request.carbAbsorptionMinutes ?: existing.carbAbsorptionMinutes,
                activityCoefficients     = newCoefficientsJson ?: existing.activityCoefficients,
                basalInsulinType         = request.basalInsulinType ?: existing.basalInsulinType,
                bolusInsulinType         = request.bolusInsulinType ?: existing.bolusInsulinType,
                lastCoefficientUpdate    = existing.lastCoefficientUpdate,
                usingAdaptiveCoefficients = existing.usingAdaptiveCoefficients,
                createdAt                = existing.createdAt,
                updatedAt                = Instant.now(),
            )
        } else {
            UserProfile(
                user                     = user,
                weightKg                 = request.weightKg,
                heightCm                 = request.heightCm,
                insulinSensitivityFactor = request.insulinSensitivityFactor,
                insulinToCarbRatio       = request.insulinToCarbRatio,
                targetGlucoseMin         = request.targetGlucoseMin,
                targetGlucoseMax         = request.targetGlucoseMax,
                durationOfInsulinAction  = request.durationOfInsulinAction,
                insulinResistanceFactor  = request.insulinResistanceFactor ?: 1.0,
                carbAbsorptionMinutes    = request.carbAbsorptionMinutes ?: 120,
                activityCoefficients     = newCoefficientsJson,
                basalInsulinType         = request.basalInsulinType,
                bolusInsulinType         = request.bolusInsulinType,
                updatedAt                = Instant.now(),
            )
        }
        userProfileRepository.save(profile)

        val refreshedUser = userRepository.findById(user.id).orElseThrow()
        return getProfile(refreshedUser)
    }
}
