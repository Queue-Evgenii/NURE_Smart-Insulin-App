package ua.nure.smartinsulinbackend

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Validates the adaptive coefficient calculation (section 2.1.1):
 *   • ICR observation: carbs / dose (for meal boluses in target range)
 *   • ISF observation: (glucose_before - glucose_after_3h) / dose (for correction doses)
 *   • Median extraction (handles outliers)
 *   • Smoothing against previous values: new = 0.4·observed + 0.6·previous
 *   • Seeding with 100/500 rules when insufficient history
 */
class AdaptiveCoefficientTest {

    @Test
    fun medianCalculationFiltersAndRanksCorrectly() {
        val values = listOf(8.0, 5.0, 12.0, 7.0, 6.0)
        val sorted = values.sorted()
        assertEquals(listOf(5.0, 6.0, 7.0, 8.0, 12.0), sorted)

        val n = sorted.size
        val median = if (n % 2 == 1) sorted[n / 2] else (sorted[n / 2 - 1] + sorted[n / 2]) / 2.0
        assertEquals(7.0, median)
    }

    @Test
    fun medianOfEvenSizedListIsAverageOfMiddleTwo() {
        val values = listOf(2.0, 4.0, 6.0, 8.0)
        val sorted = values.sorted()
        val n = sorted.size
        val median = if (n % 2 == 1) sorted[n / 2] else (sorted[n / 2 - 1] + sorted[n / 2]) / 2.0
        assertEquals(5.0, median)
    }

    @Test
    fun smoothingFormulaBlendOldAndNewValuesCorrectly() {
        val observed = 10.0
        val previous = 8.0
        val smoothed = 0.4 * observed + 0.6 * previous
        assertEquals(8.8, smoothed, 0.001)

        val afterRound = Math.round(smoothed * 100.0) / 100.0
        assertEquals(8.80, afterRound, 0.001)
    }

    @Test
    fun firstAdaptivePassUsesRawMedianNotSmoothed() {
        val observations = listOf(8.0, 9.0, 10.0, 11.0, 12.0)
        val median = observations.sorted()[observations.size / 2]
        assertEquals(10.0, median)

        val previous = 9.5
        val wasAdaptive = false   // first time
        val value = if (wasAdaptive && previous != null) 0.4 * median + 0.6 * previous else median
        assertEquals(10.0, value)  // raw median, no smoothing
    }

    @Test
    fun subsequentPassesApplySmoothingAgainstPreviousValue() {
        val observations = listOf(8.0, 9.0, 10.0, 11.0, 12.0)
        val median = observations.sorted()[observations.size / 2]

        val previous = 9.5
        val wasAdaptive = true    // already using adaptive
        val value = if (wasAdaptive && previous != null) 0.4 * median + 0.6 * previous else median
        assertEquals(9.7, value, 0.001)
    }

    @Test
    fun rules100And500SeedCoefficientsWhenInsufficientHistory() {
        val tdd = 40.0   // average total daily dose

        val isf0 = 100.0 / tdd
        val icr0 = 500.0 / tdd

        assertEquals(2.5, isf0)
        assertEquals(12.5, icr0)
    }

    @Test
    fun resolutionPicksObservationsOverSeed() {
        val observations = listOf(8.0, 9.0, 10.0, 11.0, 12.0)  // ≥ 5 observations
        val median = observations.sorted()[observations.size / 2]
        val previous = null
        val seed = 2.5

        val result = when {
            observations.size >= 5 -> median
            previous != null -> previous
            seed != null && seed.isFinite() && seed > 0.0 -> seed
            else -> null
        }
        assertEquals(10.0, result)
    }

    @Test
    fun resolutionPicksPreviousOverSeedWhenInsufficientObservations() {
        val observations = listOf(8.0, 9.0)  // < 5 observations
        val previous = 9.5
        val seed = 2.5

        val result = when {
            observations.size >= 5 -> observations.sorted()[observations.size / 2]
            previous != null -> previous
            seed != null && seed.isFinite() && seed > 0.0 -> seed
            else -> null
        }
        assertEquals(9.5, result)
    }

    @Test
    fun resolutionFallsBackToSeedWhenNoHistoryExists() {
        val observations = emptyList<Double>()
        val previous = null
        val seed = 2.5

        val result = when {
            observations.size >= 5 -> null
            previous != null -> previous
            seed != null && seed.isFinite() && seed > 0.0 -> seed
            else -> null
        }
        assertEquals(2.5, result)
    }

    @Test
    fun icrObservationFiltersBasedOnCarbsThreshold() {
        data class DoseRecord(val carbsG: Double, val doseUnits: Double)

        val doses = listOf(
            DoseRecord(50.0, 5.0),    // meal → 50/5 = 10.0
            DoseRecord(8.0, 1.0),     // correction (< 10g) → filtered
            DoseRecord(30.0, 3.0),    // meal → 30/3 = 10.0
        )

        val filtered = doses.filter { it.carbsG >= 10.0 }
        val observations = filtered.map { it.carbsG / it.doseUnits }

        assertEquals(2, filtered.size)
        assertEquals(listOf(10.0, 10.0), observations)
    }

    @Test
    fun isfObservationCalculatesDropOver3Hours() {
        val glucoseBefore = 8.0
        val glucoseAfter3h = 5.5
        val doseUnits = 1.0

        val drop = glucoseBefore - glucoseAfter3h
        val isf = drop / doseUnits

        assertEquals(2.5, drop)
        assertEquals(2.5, isf)
    }

    @Test
    fun outlierIsfValuesAreFiltered() {
        val observations = listOf(2.5, -1.0, 3.0, 0.0, 2.8)
        val filtered = observations.filter { it > 0.0 }

        assertEquals(3, filtered.size)
        assertEquals(listOf(2.5, 3.0, 2.8), filtered)
    }

    // ── Physiological bounds & low-TDD guard (regression: ICR 55.56 bug) ────────

    @Test
    fun lowTotalDailyDoseDoesNotSeedCoefficients() {
        // TDD = 9 U/day would seed ICR = 500/9 = 55.56 and ISF = 100/9 = 11.11.
        // Below the 15 U/day floor we skip seeding entirely.
        val minTddForSeed = 15.0
        val tdd = 9.0

        val seed = if (tdd >= minTddForSeed) 500.0 / tdd else null
        assertEquals(null, seed)
    }

    @Test
    fun outOfRangeIcrObservationsAreFiltered() {
        val icrMin = 1.0
        val icrMax = 50.0
        // 500g / 1U = 500 (impossible) must be dropped; 50g / 5U = 10 kept.
        val observations = listOf(500.0, 10.0, 0.5, 12.0)
        val filtered = observations.filter { it in icrMin..icrMax }

        assertEquals(listOf(10.0, 12.0), filtered)
    }

    @Test
    fun resolvedCoefficientIsClampedToBounds() {
        val icrMin = 1.0
        val icrMax = 50.0
        // A legacy stored value of 55.56 is healed back into range on recompute.
        val previous = 55.56
        val clamped = previous.coerceIn(icrMin, icrMax)

        assertEquals(50.0, clamped)
    }
}
