package ua.nure.smartinsulinbackend.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.ObjectMapper
import ua.nure.smartinsulinbackend.dto.ActivityCoefficientsDto
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
    private val activityAdjustmentService: ActivityAdjustmentService,
    private val mapper: ObjectMapper,
) {

    fun calculate(user: User, request: BolusCalculationRequest): BolusCalculationResponse {
        val profile = userProfileRepository.findByUserId(user.id).orElse(null)

        // ── ISF / ICR: manual → weight-based estimate (1700/500 rules) → missing ──────
        val weightKg = profile?.weightKg
        val tddEstimate = weightKg?.let { it * 0.5 }   // 0.5 U/kg — conservative T1DM start

        val missingParams = mutableListOf<String>()
        var usingWeightEstimation = false

        val insulinToCarbRatio = (profile?.insulinToCarbRatio
            ?: tddEstimate?.let { tdd ->
                usingWeightEstimation = true
                500.0 / tdd           // 500 rule (Walsh & Roberts)
            })
            ?.coerceIn(ICR_MIN, ICR_MAX)   // guard against out-of-range stored/estimated values
            ?: run { missingParams += "insulinToCarbRatio"; null }

        val insulinSensitivityFactor = (profile?.insulinSensitivityFactor
            ?: tddEstimate?.let { tdd ->
                usingWeightEstimation = true
                94.0 / tdd            // 1700 rule converted to mmol/L (÷18)
            })
            ?.coerceIn(ISF_MIN, ISF_MAX)   // guard against out-of-range stored/estimated values
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

        val resistanceFactor = profile?.insulinResistanceFactor ?: 1.0
        val totalDose = max(0.0, (bolusForCarbs + correctionDose - currentIob) * resistanceFactor)

        val coefficients = profile?.activityCoefficients
            ?.let { runCatching { mapper.readValue(it, ActivityCoefficientsDto::class.java) }.getOrNull() }
            ?: ActivityCoefficientsDto.DEFAULT

        val adjustment = activityAdjustmentService.adjust(totalDose, request.activity, coefficients)

        return BolusCalculationResponse(
            bolusForCarbs     = round1(bolusForCarbs),
            correctionDose    = round1(correctionDose),
            currentIob        = round1(currentIob),
            totalDose         = round1(totalDose),
            mealRecordId      = request.mealRecordId,
            missingParams     = missingParams,
            usingAdaptiveCoefficients = profile?.usingAdaptiveCoefficients ?: false,
            adjustedDose      = adjustment?.adjustedDose ?: round1(totalDose),
            activityFactor    = adjustment?.activityFactor,
            timeFactor        = adjustment?.timeFactor,
            durationFactor    = adjustment?.durationFactor,
            activityWarning   = adjustment?.warning,
            usingWeightEstimation = usingWeightEstimation,
        )
    }

    private fun round1(v: Double) = Math.round(v * 10.0) / 10.0

    private companion object {
        // Physiological bounds, mirroring UserProfileUpdateRequest validation.
        const val ICR_MIN = 1.0
        const val ICR_MAX = 50.0
        const val ISF_MIN = 0.5
        const val ISF_MAX = 20.0
    }
}
