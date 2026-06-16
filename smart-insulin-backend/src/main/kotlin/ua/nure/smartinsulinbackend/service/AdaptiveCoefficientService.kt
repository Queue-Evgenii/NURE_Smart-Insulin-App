package ua.nure.smartinsulinbackend.service

import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ua.nure.smartinsulinbackend.entity.DoseType
import ua.nure.smartinsulinbackend.entity.InsulinDose
import ua.nure.smartinsulinbackend.entity.UserProfile
import ua.nure.smartinsulinbackend.repository.GlucoseReadingRepository
import ua.nure.smartinsulinbackend.repository.InsulinDoseRepository
import ua.nure.smartinsulinbackend.repository.UserProfileRepository
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset

/**
 * Derives individualised ICR and ISF coefficients from a patient's own history
 * (diploma section 2.1.1 — "Модель врахування вуглеводного коефіцієнта та фактора чутливості").
 *
 * Method:
 *   • ICR — for meal boluses taken while glucose was inside the target range
 *           (so the correction component ≈ 0), observe carbs / dose, take the median.
 *   • ISF — for correction injections (carbs < 10 g) observe
 *           (glucose_before − glucose_after_3h) / dose, take the median.
 *           Post-prandial glucose is auto-detected from readings 2.5–3.5 h after the dose.
 *   • Seeds — when history is insufficient, the "100" and "500" rules apply:
 *           ISF₀ = 100 / TDD,  ICR₀ = 500 / TDD  (TDD = average total daily dose).
 *   • Smoothing — new = 0.4·observed + 0.6·previous, to avoid abrupt jumps.
 *
 * Recomputed on demand and weekly via the scheduler.
 */
@Service
class AdaptiveCoefficientService(
    private val userProfileRepository: UserProfileRepository,
    private val insulinDoseRepository: InsulinDoseRepository,
    private val glucoseReadingRepository: GlucoseReadingRepository,
) {
    private val log = LoggerFactory.getLogger(AdaptiveCoefficientService::class.java)

    private val analysisWindowDays = 30L
    private val minObservations = 5
    private val recalcIntervalDays = 7L
    private val smoothNew = 0.4
    private val smoothPrev = 0.6
    private val correctionCarbsThreshold = 10.0   // g — below this a dose is treated as a correction
    private val postprandialFromHours = 2.5
    private val postprandialToHours = 3.5

    // Physiological bounds. They must mirror the manual-entry validation in
    // UserProfileUpdateRequest (ISF 0.5–20 mmol/L/U, ICR 1–50 g/U). The adaptive
    // path persists the profile directly, bypassing the DTO validation, so it has
    // to enforce these limits itself — otherwise an absurd value (e.g. ICR 55.56)
    // can slip into the profile and silently under-dose the patient.
    private val icrMin = 1.0
    private val icrMax = 50.0
    private val isfMin = 0.5
    private val isfMax = 20.0

    // Below this average total daily dose the 100/500 rules produce unreliable
    // coefficients (sparse history or a few tiny test doses), so we skip seeding
    // and leave the value unset rather than emit garbage.
    private val minTddForSeed = 15.0

    data class CoefficientUpdateResult(
        val insulinToCarbRatio: Double?,
        val insulinSensitivityFactor: Double?,
        val icrObservations: Int,
        val isfObservations: Int,
        val usingAdaptiveCoefficients: Boolean,
        val applied: Boolean,
    )

    /** Recompute and persist coefficients for a single user. */
    @Transactional
    fun recalculateForUser(userId: Long): CoefficientUpdateResult {
        val profile = userProfileRepository.findByUserId(userId).orElse(null)
            ?: return CoefficientUpdateResult(null, null, 0, 0, false, false)

        val windowStart = Instant.now().minus(Duration.ofDays(analysisWindowDays))
        val doses = insulinDoseRepository
            .findByUserIdAndInjectedAtAfterOrderByInjectedAtAsc(userId, windowStart)

        val icrObs = observeIcr(profile, doses)
        val isfObs = observeIsf(userId, doses)
        val tdd = averageTotalDailyDose(doses, windowStart)

        val newIcr = resolveCoefficient(
            observations = icrObs,
            previous = profile.insulinToCarbRatio,
            wasAdaptive = profile.usingAdaptiveCoefficients,
            seed = tdd?.takeIf { it >= minTddForSeed }?.let { 500.0 / it },
            min = icrMin,
            max = icrMax,
        )
        val newIsf = resolveCoefficient(
            observations = isfObs,
            previous = profile.insulinSensitivityFactor,
            wasAdaptive = profile.usingAdaptiveCoefficients,
            seed = tdd?.takeIf { it >= minTddForSeed }?.let { 100.0 / it },
            min = isfMin,
            max = isfMax,
        )

        val nowAdaptive = profile.usingAdaptiveCoefficients ||
            icrObs.size >= minObservations || isfObs.size >= minObservations

        val updated = profile.copyWith(
            insulinToCarbRatio = newIcr ?: profile.insulinToCarbRatio,
            insulinSensitivityFactor = newIsf ?: profile.insulinSensitivityFactor,
            lastCoefficientUpdate = Instant.now(),
            usingAdaptiveCoefficients = nowAdaptive,
        )
        userProfileRepository.save(updated)

        log.info(
            "Adaptive coefficients for user {}: ICR={} ({} obs), ISF={} ({} obs), adaptive={}",
            userId, newIcr, icrObs.size, newIsf, isfObs.size, nowAdaptive,
        )

        return CoefficientUpdateResult(
            insulinToCarbRatio = updated.insulinToCarbRatio,
            insulinSensitivityFactor = updated.insulinSensitivityFactor,
            icrObservations = icrObs.size,
            isfObservations = isfObs.size,
            usingAdaptiveCoefficients = nowAdaptive,
            applied = true,
        )
    }

    /** Weekly batch recompute for every profile whose coefficients are stale (> 7 days). */
    @Scheduled(cron = "0 0 3 * * SUN")
    @Transactional
    fun scheduledRecalculation() {
        val staleBefore = Instant.now().minus(Duration.ofDays(recalcIntervalDays))
        val profiles = userProfileRepository.findAll()
            .filter { it.lastCoefficientUpdate == null || it.lastCoefficientUpdate!!.isBefore(staleBefore) }

        log.info("Scheduled coefficient recalculation for {} profile(s)", profiles.size)
        profiles.forEach { profile ->
            try {
                recalculateForUser(profile.user.id)
            } catch (e: Exception) {
                log.warn("Failed to recalculate coefficients for user {}: {}", profile.user.id, e.message)
            }
        }
    }

    // ── Observation extraction ────────────────────────────────────────────────

    /** Meal boluses taken inside the target range → carbs / dose. */
    private fun observeIcr(profile: UserProfile, doses: List<InsulinDose>): List<Double> =
        doses.asSequence()
            .filter { it.doseType == DoseType.BOLUS && it.doseUnits > 0.0 }
            .filter { (it.mealRecord?.carbohydratesG ?: 0.0) >= correctionCarbsThreshold }
            .filter { glucoseInTarget(it.glucoseBefore, profile) }
            .map { it.mealRecord!!.carbohydratesG / it.doseUnits }
            .filter { it.isFinite() && it in icrMin..icrMax }   // drop physiologically impossible outliers
            .toList()

    /** Correction injections → (glucose_before − glucose_after_3h) / dose. */
    private fun observeIsf(userId: Long, doses: List<InsulinDose>): List<Double> =
        doses.asSequence()
            .filter { it.doseUnits > 0.0 && it.glucoseBefore != null }
            .filter { isCorrectionDose(it) }
            .mapNotNull { dose ->
                val after = glucoseReadingRepository.findFirstByUserIdAndMeasuredAtBetweenOrderByMeasuredAtAsc(
                    userId,
                    dose.injectedAt.plus(Duration.ofMinutes((postprandialFromHours * 60).toLong())),
                    dose.injectedAt.plus(Duration.ofMinutes((postprandialToHours * 60).toLong())),
                ) ?: return@mapNotNull null
                val drop = dose.glucoseBefore!! - after.glucoseValue
                if (drop <= 0.0) null else drop / dose.doseUnits
            }
            .filter { it.isFinite() && it in isfMin..isfMax }   // drop physiologically impossible outliers
            .toList()

    private fun isCorrectionDose(dose: InsulinDose): Boolean =
        dose.doseType == DoseType.CORRECTION ||
            (dose.mealRecord?.carbohydratesG ?: 0.0) < correctionCarbsThreshold

    private fun glucoseInTarget(glucose: Double?, profile: UserProfile): Boolean {
        if (glucose == null) return true   // meal-only entry — assume negligible correction
        val min = profile.targetGlucoseMin ?: return true
        val max = profile.targetGlucoseMax ?: return true
        return glucose in min..max
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    /**
     * Picks the next coefficient value:
     *   enough observations → smoothed median (or raw median on first adaptive pass);
     *   too few but a previous value exists → keep it;
     *   nothing yet → fall back to the 100/500-rule seed.
     */
    private fun resolveCoefficient(
        observations: List<Double>,
        previous: Double?,
        wasAdaptive: Boolean,
        seed: Double?,
        min: Double,
        max: Double,
    ): Double? {
        val value = when {
            observations.size >= minObservations -> {
                val med = median(observations)
                if (wasAdaptive && previous != null) smoothNew * med + smoothPrev * previous else med
            }
            previous != null -> previous
            seed != null && seed.isFinite() && seed > 0.0 -> seed
            else -> null
        }
        // Final safety net — clamps the median/seed and heals any legacy out-of-range
        // value already stored in the profile.
        return value?.let { round2(it.coerceIn(min, max)) }
    }

    /** Average total daily dose over the window, used by the 100/500 rules. */
    private fun averageTotalDailyDose(doses: List<InsulinDose>, windowStart: Instant): Double? {
        if (doses.isEmpty()) return null
        val activeDays = doses
            .map { it.injectedAt.atZone(ZoneOffset.UTC).toLocalDate() }
            .distinct().count()
        if (activeDays == 0) return null
        val total = doses.sumOf { it.doseUnits }
        val tdd = total / activeDays
        return if (tdd > 0.0) tdd else null
    }

    private fun median(values: List<Double>): Double {
        val sorted = values.sorted()
        val n = sorted.size
        return if (n % 2 == 1) sorted[n / 2] else (sorted[n / 2 - 1] + sorted[n / 2]) / 2.0
    }

    private fun round2(v: Double) = Math.round(v * 100.0) / 100.0

    private fun UserProfile.copyWith(
        insulinToCarbRatio: Double?,
        insulinSensitivityFactor: Double?,
        lastCoefficientUpdate: Instant,
        usingAdaptiveCoefficients: Boolean,
    ) = UserProfile(
        id = id,
        user = user,
        weightKg = weightKg,
        heightCm = heightCm,
        insulinSensitivityFactor = insulinSensitivityFactor,
        insulinToCarbRatio = insulinToCarbRatio,
        targetGlucoseMin = targetGlucoseMin,
        targetGlucoseMax = targetGlucoseMax,
        durationOfInsulinAction = durationOfInsulinAction,
        // Preserve fields the recalculation does not touch — omitting them previously
        // reset them to their defaults (1.0 / 120 / null) on every recompute.
        insulinResistanceFactor = insulinResistanceFactor,
        carbAbsorptionMinutes = carbAbsorptionMinutes,
        activityCoefficients = activityCoefficients,
        basalInsulinType = basalInsulinType,
        bolusInsulinType = bolusInsulinType,
        lastCoefficientUpdate = lastCoefficientUpdate,
        usingAdaptiveCoefficients = usingAdaptiveCoefficients,
        createdAt = createdAt,
        updatedAt = Instant.now(),
    )
}
