package ua.nure.smartinsulinbackend.service

import org.springframework.stereotype.Service
import ua.nure.smartinsulinbackend.dto.ActivityCoefficientsDto
import ua.nure.smartinsulinbackend.dto.ActivityInput
import kotlin.math.max

enum class ActivityType { AEROBIC, ANAEROBIC, MIXED }
enum class ActivityIntensity { LIGHT, MODERATE, HIGH, MAXIMAL }

@Service
class ActivityAdjustmentService {

    private val minDoseFraction = 0.30
    private val warningFraction = 0.50

    data class ActivityAdjustment(
        val baseDose: Double,
        val adjustedDose: Double,
        val activityFactor: Double,
        val timeFactor: Double,
        val durationFactor: Double,
        val combinedFactor: Double,
        val warning: String?,
    )

    fun adjust(
        baseDose: Double,
        activity: ActivityInput?,
        coefficients: ActivityCoefficientsDto = ActivityCoefficientsDto.DEFAULT,
    ): ActivityAdjustment? {
        if (activity == null || baseDose <= 0.0) return null

        val type      = parseEnum<ActivityType>(activity.type) ?: ActivityType.AEROBIC
        val intensity = parseEnum<ActivityIntensity>(activity.intensity) ?: ActivityIntensity.MODERATE

        val kActivity = lookupCoefficient(intensity, type, coefficients)
        val kTime     = timeFactor(activity.minutesUntilStart)
        val kDuration = durationFactor(activity.durationMinutes)

        val combined   = kActivity * kTime * kDuration
        val rawAdjusted = baseDose * combined
        val adjusted   = max(rawAdjusted, baseDose * minDoseFraction)

        val warning = if (adjusted < baseDose * warningFraction)
            "Рекомендована доза значно менша від розрахованої. " +
                "Переконайтесь, що інформація про активність правильна."
        else null

        return ActivityAdjustment(
            baseDose        = round1(baseDose),
            adjustedDose    = round1(adjusted),
            activityFactor  = round2(kActivity),
            timeFactor      = round2(kTime),
            durationFactor  = round2(kDuration),
            combinedFactor  = round2(combined),
            warning         = warning,
        )
    }

    private fun lookupCoefficient(
        intensity: ActivityIntensity,
        type: ActivityType,
        c: ActivityCoefficientsDto,
    ): Double = when (intensity) {
        ActivityIntensity.LIGHT    -> when (type) {
            ActivityType.AEROBIC    -> c.lightAerobic
            ActivityType.ANAEROBIC  -> c.lightAnaerobic
            ActivityType.MIXED      -> c.lightMixed
        }
        ActivityIntensity.MODERATE -> when (type) {
            ActivityType.AEROBIC    -> c.moderateAerobic
            ActivityType.ANAEROBIC  -> c.moderateAnaerobic
            ActivityType.MIXED      -> c.moderateMixed
        }
        ActivityIntensity.HIGH     -> when (type) {
            ActivityType.AEROBIC    -> c.highAerobic
            ActivityType.ANAEROBIC  -> c.highAnaerobic
            ActivityType.MIXED      -> c.highMixed
        }
        ActivityIntensity.MAXIMAL  -> when (type) {
            ActivityType.AEROBIC    -> c.maximalAerobic
            ActivityType.ANAEROBIC  -> c.maximalAnaerobic
            ActivityType.MIXED      -> c.maximalMixed
        }
    }

    private fun timeFactor(minutesUntilStart: Int): Double = when {
        minutesUntilStart <= 30  -> 1.0
        minutesUntilStart >= 180 -> 0.7
        else -> 1.0 - (minutesUntilStart - 30) / 150.0 * 0.3
    }

    private fun durationFactor(durationMinutes: Int): Double = when {
        durationMinutes <= 30 -> 1.0
        durationMinutes <= 60 -> 0.9
        durationMinutes <= 90 -> 0.8
        else -> 0.7
    }

    private inline fun <reified T : Enum<T>> parseEnum(value: String?): T? =
        value?.let { runCatching { enumValueOf<T>(it.trim().uppercase()) }.getOrNull() }

    private fun round1(v: Double) = Math.round(v * 10.0) / 10.0
    private fun round2(v: Double) = Math.round(v * 100.0) / 100.0
}
