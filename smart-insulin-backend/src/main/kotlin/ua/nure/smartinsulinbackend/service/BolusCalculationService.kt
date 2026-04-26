package ua.nure.smartinsulinbackend.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ua.nure.smartinsulinbackend.dto.BolusCalculationRequest
import ua.nure.smartinsulinbackend.dto.BolusCalculationResponse
import ua.nure.smartinsulinbackend.entity.User
import ua.nure.smartinsulinbackend.library.HbA1cLibrary
import ua.nure.smartinsulinbackend.repository.InsulinDoseRepository
import ua.nure.smartinsulinbackend.repository.UserProfileRepository
import java.time.Instant
import kotlin.math.max
import kotlin.math.roundToLong

@Service
@Transactional(readOnly = true)
class BolusCalculationService(
    private val userProfileRepository: UserProfileRepository,
    private val insulinDoseRepository: InsulinDoseRepository,
    private val hbA1cLibrary: HbA1cLibrary,
) {

    fun calculate(user: User, request: BolusCalculationRequest): BolusCalculationResponse {
        val profile = userProfileRepository.findByUserId(user.id).orElse(null)

        val missingParams = mutableListOf<String>()
        val insulinToCarbRatio = profile?.insulinToCarbRatio
            ?: run { missingParams += "insulinToCarbRatio"; null }
        val insulinSensitivityFactor = profile?.insulinSensitivityFactor
            ?: run { missingParams += "insulinSensitivityFactor"; null }
        val targetGlucoseMin = profile?.targetGlucoseMin
            ?: run { missingParams += "targetGlucoseMin"; null }
        val targetGlucoseMax = profile?.targetGlucoseMax
            ?: run { missingParams += "targetGlucoseMax"; null }
        val durationOfInsulinAction = profile?.durationOfInsulinAction
            ?: run { missingParams += "durationOfInsulinAction"; null }

        val bolusForCarbs = if (insulinToCarbRatio != null && insulinToCarbRatio > 0)
            request.carbsG / insulinToCarbRatio else 0.0

        val correctionDose = if (insulinSensitivityFactor != null && insulinSensitivityFactor > 0
            && targetGlucoseMin != null && targetGlucoseMax != null
        ) {
            val targetMid = (targetGlucoseMin + targetGlucoseMax) / 2.0
            (request.currentGlucose - targetMid) / insulinSensitivityFactor
        } else 0.0

        // Sum active insulin from all doses injected within the DIA window
        val currentIob = if (durationOfInsulinAction != null && durationOfInsulinAction > 0) {
            val diaMins = durationOfInsulinAction * 60.0
            val windowStart = Instant.now().minusSeconds((diaMins * 60).roundToLong())
            val recentDoses = insulinDoseRepository
                .findByUserIdAndInjectedAtBetweenOrderByInjectedAtAsc(user.id, windowStart, Instant.now())
            val now = Instant.now()
            recentDoses.sumOf { dose ->
                val timeSinceMins = (now.epochSecond - dose.injectedAt.epochSecond) / 60.0
                hbA1cLibrary.calculate_iob(dose.doseUnits, timeSinceMins, diaMins)
            }
        } else 0.0

        val totalDose = max(0.0, bolusForCarbs + correctionDose - currentIob)

        return BolusCalculationResponse(
            bolusForCarbs = round1(bolusForCarbs),
            correctionDose = round1(correctionDose),
            currentIob = round1(currentIob),
            totalDose = round1(totalDose),
            mealRecordId = request.mealRecordId,
            missingParams = missingParams,
        )
    }

    private fun round1(v: Double) = Math.round(v * 10.0) / 10.0
}
