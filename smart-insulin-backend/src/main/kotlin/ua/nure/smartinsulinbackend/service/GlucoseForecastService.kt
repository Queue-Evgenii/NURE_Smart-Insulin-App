package ua.nure.smartinsulinbackend.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ua.nure.smartinsulinbackend.dto.ForecastPoint
import ua.nure.smartinsulinbackend.dto.ForecastRequest
import ua.nure.smartinsulinbackend.dto.ForecastResponse
import ua.nure.smartinsulinbackend.entity.User
import ua.nure.smartinsulinbackend.library.HbA1cLibrary
import ua.nure.smartinsulinbackend.repository.GlucoseReadingRepository
import ua.nure.smartinsulinbackend.repository.InsulinDoseRepository
import ua.nure.smartinsulinbackend.repository.MealRecordRepository
import ua.nure.smartinsulinbackend.repository.UserProfileRepository
import java.time.Instant
import kotlin.math.roundToLong

/**
 * Short-horizon glucose forecasting (diploma section 2.2 — "Алгоритми прогнозування глікемічних станів").
 *
 * Combines, via the native C library:
 *   • the recent trend (OLS slope over the last readings),
 *   • the decaying effect of active insulin (IOB),
 *   • the rise from carbohydrates still being absorbed,
 * and reports a predicted value with an uncertainty band that widens with glucose volatility.
 */
@Service
@Transactional(readOnly = true)
class GlucoseForecastService(
    private val glucoseReadingRepository: GlucoseReadingRepository,
    private val insulinDoseRepository: InsulinDoseRepository,
    private val userProfileRepository: UserProfileRepository,
    private val mealRecordRepository: MealRecordRepository,
    private val hbA1cLibrary: HbA1cLibrary,
) {
    private val trendWindowMinutes = 45L
    private val stepMinutes = 30
    private val hypoThreshold = 3.9
    private val hyperThreshold = 10.0
    private val minReadingsForConfidence = 3

    fun forecast(user: User, request: ForecastRequest): ForecastResponse {
        val recent = glucoseReadingRepository
            .findTop12ByUserIdOrderByMeasuredAtDesc(user.id)
            .sortedBy { it.measuredAt }           // oldest → newest

        if (recent.isEmpty()) {
            return ForecastResponse(
                currentGlucose = 0.0, trendPerMinute = 0.0,
                points = emptyList(), riskFlags = listOf("NO_DATA"), readingsUsed = 0,
            )
        }

        val latest = recent.last()
        val currentGlucose = latest.glucoseValue
        val anchor = latest.measuredAt

        // Trend points — readings within the trend window, expressed in minutes relative to the anchor.
        val trendReadings = recent.filter {
            it.measuredAt.isAfter(anchor.minusSeconds(trendWindowMinutes * 60))
        }.ifEmpty { recent.takeLast(3) }

        val xs = trendReadings.map { (it.measuredAt.epochSecond - anchor.epochSecond) / 60.0 }.toDoubleArray()
        val ys = trendReadings.map { it.glucoseValue }.toDoubleArray()

        val slopePerMin = if (xs.size >= 2)
            hbA1cLibrary.ols_slope(xs, ys, xs.size.toLong()) else 0.0

        val volatility = hbA1cLibrary.std_dev(
            recent.map { it.glucoseValue }.toDoubleArray(), recent.size.toLong(),
        )

        // Clinical parameters (trend-only forecast when they are absent).
        val profile = userProfileRepository.findByUserId(user.id).orElse(null)
        val tddEstimate = profile?.weightKg?.let { it * 0.5 }
        val isf = profile?.insulinSensitivityFactor
            ?: tddEstimate?.let { 94.0 / it }
            ?: 0.0
        val icr = profile?.insulinToCarbRatio
            ?: tddEstimate?.let { 500.0 / it }
            ?: 0.0
        val diaMin = (profile?.durationOfInsulinAction ?: 0.0) * 60.0
        val carbAbsorptionMinutes = (profile?.carbAbsorptionMinutes ?: 120).toDouble()
        val iob = if (diaMin > 0.0) currentIob(user.id, diaMin) else 0.0
        // If the client did not supply carbsOnBoard, derive it from meals eaten within the
        // absorption window — linear decay: COB = carbs × (1 − t / carbAbsorptionMinutes).
        val carbsOnBoard = if (request.carbsOnBoard > 0.0) {
            request.carbsOnBoard
        } else {
            currentCob(user.id, anchor, carbAbsorptionMinutes)
        }

        val horizon = request.horizonMinutes.coerceIn(stepMinutes, 360)
        val riskFlags = mutableSetOf<String>()
        if (recent.size < minReadingsForConfidence) riskFlags += "LOW_CONFIDENCE"

        val points = mutableListOf<ForecastPoint>()
        var h = stepMinutes
        while (h <= horizon) {
            val predicted = hbA1cLibrary.forecast_glucose(
                currentGlucose, slopePerMin, h.toDouble(),
                iob, isf, diaMin, carbsOnBoard, icr, carbAbsorptionMinutes,
            )
            // Band widens with volatility and horizon; minimum 0.5 mmol/L of inherent uncertainty.
            val band = (volatility + 0.5) * (1.0 + h / 120.0)
            val lower = (predicted - band).coerceAtLeast(1.0)
            val upper = predicted + band

            if (predicted < hypoThreshold) riskFlags += "HYPO_RISK"
            else if (predicted > hyperThreshold) riskFlags += "HYPER_RISK"

            points.add(ForecastPoint(
                minutesAhead = h,
                predicted = round1(predicted),
                lower = round1(lower),
                upper = round1(upper),
            ))
            h += stepMinutes
        }

        return ForecastResponse(
            currentGlucose = round1(currentGlucose),
            trendPerMinute = round3(slopePerMin),
            points = points,
            riskFlags = riskFlags.toList(),
            readingsUsed = recent.size,
        )
    }

    private fun currentCob(userId: Long, anchor: Instant, carbAbsorptionMinutes: Double): Double {
        val windowStart = anchor.minusSeconds((carbAbsorptionMinutes * 60).toLong())
        return mealRecordRepository
            .findByUserIdAndMealTimeBetweenOrderByMealTimeAsc(userId, windowStart, anchor)
            .sumOf { meal ->
                val minutesAgo = (anchor.epochSecond - meal.mealTime.epochSecond) / 60.0
                val remaining = 1.0 - minutesAgo / carbAbsorptionMinutes
                meal.carbohydratesG * remaining.coerceAtLeast(0.0)
            }
    }

    private fun currentIob(userId: Long, diaMin: Double): Double {
        val now = Instant.now()
        val windowStart = now.minusSeconds((diaMin * 60).roundToLong())
        return insulinDoseRepository
            .findByUserIdAndInjectedAtBetweenOrderByInjectedAtAsc(userId, windowStart, now)
            .sumOf { dose ->
                val timeSinceMins = (now.epochSecond - dose.injectedAt.epochSecond) / 60.0
                hbA1cLibrary.calculate_iob(dose.doseUnits, timeSinceMins, diaMin)
            }
    }

    private fun round1(v: Double) = Math.round(v * 10.0) / 10.0
    private fun round3(v: Double) = Math.round(v * 1000.0) / 1000.0
}
