package ua.nure.smartinsulinbackend.service

import org.springframework.stereotype.Service
import ua.nure.smartinsulinbackend.dto.ActivityInput
import kotlin.math.max

/** Aerobic / anaerobic / mixed — different glucose-lowering profiles (section 2.1.2). */
enum class ActivityType { AEROBIC, ANAEROBIC, MIXED }

/** Subjective / heart-rate based intensity bands (ACSM classification). */
enum class ActivityIntensity { LIGHT, MODERATE, HIGH, MAXIMAL }

/**
 * Lowers the calculated bolus dose for planned physical activity
 * (diploma section 2.1.2 — "Алгоритм корекції дози на основі прогнозованої фізичної активності").
 *
 * Final dose = base × k_activity × k_time × k_duration, with a hard floor at 30 % of the base
 * (the dose may never be cut by more than 70 %, since carbs still require coverage).
 */
@Service
class ActivityAdjustmentService {

    /** k_activity — reduction factor by type × intensity (table 2.1). */
    private val coefficientTable: Map<ActivityIntensity, Map<ActivityType, Double>> = mapOf(
        ActivityIntensity.LIGHT to mapOf(
            ActivityType.AEROBIC to 0.90, ActivityType.ANAEROBIC to 0.95, ActivityType.MIXED to 0.93,
        ),
        ActivityIntensity.MODERATE to mapOf(
            ActivityType.AEROBIC to 0.75, ActivityType.ANAEROBIC to 0.87, ActivityType.MIXED to 0.82,
        ),
        ActivityIntensity.HIGH to mapOf(
            ActivityType.AEROBIC to 0.60, ActivityType.ANAEROBIC to 0.77, ActivityType.MIXED to 0.68,
        ),
        ActivityIntensity.MAXIMAL to mapOf(
            ActivityType.AEROBIC to 0.40, ActivityType.ANAEROBIC to 0.67, ActivityType.MIXED to 0.52,
        ),
    )

    /** Lowest allowed share of the base dose — never reduce by more than 70 %. */
    private val minDoseFraction = 0.30

    /** Warn the user when the adjusted dose drops below half of the base dose. */
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

    fun adjust(baseDose: Double, activity: ActivityInput?): ActivityAdjustment? {
        if (activity == null || baseDose <= 0.0) return null

        val type = parseEnum<ActivityType>(activity.type) ?: ActivityType.AEROBIC
        val intensity = parseEnum<ActivityIntensity>(activity.intensity) ?: ActivityIntensity.MODERATE

        val kActivity = coefficientTable[intensity]?.get(type) ?: 1.0
        val kTime = timeFactor(activity.minutesUntilStart)
        val kDuration = durationFactor(activity.durationMinutes)

        val combined = kActivity * kTime * kDuration
        val rawAdjusted = baseDose * combined
        val floor = baseDose * minDoseFraction
        val adjusted = max(rawAdjusted, floor)

        val warning = if (adjusted < baseDose * warningFraction)
            "Рекомендована доза значно менша від розрахованої. " +
                "Переконайтесь, що інформація про активність правильна."
        else null

        return ActivityAdjustment(
            baseDose = round1(baseDose),
            adjustedDose = round1(adjusted),
            activityFactor = round2(kActivity),
            timeFactor = round2(kTime),
            durationFactor = round2(kDuration),
            combinedFactor = round2(combined),
            warning = warning,
        )
    }

    /**
     * k_time — depends on when activity starts relative to the injection:
     *   0–30 min   → 1.0  (insulin not yet active);
     *   30–180 min → linear decline to 0.7 (overlaps the insulin peak, biggest cut);
     *   > 180 min  → 0.7.
     */
    private fun timeFactor(minutesUntilStart: Int): Double = when {
        minutesUntilStart <= 30 -> 1.0
        minutesUntilStart >= 180 -> 0.7
        else -> 1.0 - (minutesUntilStart - 30) / 150.0 * 0.3
    }

    /**
     * k_duration — longer activity burns more glucose (source [13]):
     *   ≤ 30 min → 1.0;  30–60 → 0.9;  60–90 → 0.8;  > 90 → 0.7.
     */
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
