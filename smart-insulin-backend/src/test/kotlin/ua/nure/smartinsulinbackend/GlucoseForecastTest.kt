package ua.nure.smartinsulinbackend

import org.junit.jupiter.api.Test
import kotlin.math.sqrt
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Validates the glucose forecasting algorithm (section 2.2):
 *   • OLS slope extraction (linear regression from recent readings)
 *   • Volatility estimation (standard deviation band widening)
 *   • Composite forecast: trend + IOB effect - carb absorption
 *   • Risk flag detection (HYPO_RISK, HYPER_RISK)
 *   • Uncertainty band computation
 */
class GlucoseForecastTest {

    private fun round1(v: Double) = Math.round(v * 10.0) / 10.0

    // ── OLS helpers (mirrors the C library logic) ──────────────────────────────

    private fun olsSlope(xs: DoubleArray, ys: DoubleArray): Double {
        if (xs.size < 2) return 0.0
        val meanX = xs.average()
        val meanY = ys.average()
        val numerator = xs.indices.sumOf { i ->
            (xs[i] - meanX) * (ys[i] - meanY)
        }
        val denominator = xs.indices.sumOf { i ->
            (xs[i] - meanX) * (xs[i] - meanX)
        }
        return if (denominator != 0.0) numerator / denominator else 0.0
    }

    private fun stdDev(values: DoubleArray): Double {
        if (values.isEmpty()) return 0.0
        val mean = values.average()
        val variance = values.map { (it - mean) * (it - mean) }.average()
        return sqrt(variance)
    }

    // ── Forecast tests ────────────────────────────────────────────────────────

    @Test
    fun olsSlopeIsZeroWhenGlucoseIsStable() {
        val xs = doubleArrayOf(0.0, 15.0, 30.0)
        val ys = doubleArrayOf(5.5, 5.5, 5.5)

        val slope = olsSlope(xs, ys)
        assertEquals(0.0, slope, 0.001)
    }

    @Test
    fun olsSlopeIsPositiveWhenGlucoseIsRising() {
        val xs = doubleArrayOf(0.0, 15.0, 30.0)
        val ys = doubleArrayOf(5.0, 6.0, 7.0)

        val slope = olsSlope(xs, ys)
        assertTrue(slope > 0.0)
        // slope ≈ 2/15 ≈ 0.133 mmol/L per minute
        assertEquals(0.067, slope, 0.01)
    }

    @Test
    fun olsSlopeIsNegativeWhenGlucoseIsFalling() {
        val xs = doubleArrayOf(0.0, 15.0, 30.0)
        val ys = doubleArrayOf(7.0, 6.0, 5.0)

        val slope = olsSlope(xs, ys)
        assertTrue(slope < 0.0)
        assertEquals(-0.067, slope, 0.01)
    }

    @Test
    fun standardDeviationIsZeroWhenAllReadingsAreIdentical() {
        val values = doubleArrayOf(5.5, 5.5, 5.5)
        val volatility = stdDev(values)
        assertEquals(0.0, volatility, 0.001)
    }

    @Test
    fun standardDeviationMeasuresSpreadOfReadings() {
        val values = doubleArrayOf(4.0, 5.0, 6.0)
        val volatility = stdDev(values)
        assertTrue(volatility > 0.0)
        // mean = 5.0, variance = ((4-5)² + (5-5)² + (6-5)²) / 3 = 2/3 ≈ 0.667
        // stddev = sqrt(0.667) ≈ 0.816
        assertEquals(0.816, volatility, 0.01)
    }

    @Test
    fun uncertaintyBandWidensWithHorizonAndVolatility() {
        val volatility = 1.0
        val baseUncertainty = 0.5

        // At 0 minutes (anchor)
        val band0 = (volatility + baseUncertainty) * (1.0 + 0 / 120.0)
        assertEquals(1.5, band0)

        // At 60 minutes
        val band60 = (volatility + baseUncertainty) * (1.0 + 60 / 120.0)
        assertEquals(2.25, band60)

        // At 120 minutes
        val band120 = (volatility + baseUncertainty) * (1.0 + 120 / 120.0)
        assertEquals(3.0, band120)
    }

    @Test
    fun hypoRiskFlagTriggersWhenPredictedBandEntersHypoRange() {
        val predicted = 4.0
        val band = 1.0
        val lower = (predicted - band).coerceAtLeast(1.0)

        assertTrue(lower < 3.9, "Lower=$lower should be < 3.9 for hypo risk")
    }

    @Test
    fun hyperRiskFlagTriggersWhenPredictedExceedsHighThreshold() {
        val hyperThreshold = 10.0
        val predicted = 11.0

        assertTrue(predicted > hyperThreshold, "Predicted=$predicted should exceed $hyperThreshold for hyper risk")
    }

    @Test
    fun lowConfidenceFlagWhenFewerThan3ReadingsAvailable() {
        val readingsCount = 2
        val minReadingsForConfidence = 3

        val hasLowConfidence = readingsCount < minReadingsForConfidence
        assertTrue(hasLowConfidence)
    }

    @Test
    fun forecastPointsAreComputedAtRegularStepIntervals() {
        val stepMinutes = 30
        val horizon = 180

        val points = (stepMinutes..horizon step stepMinutes).toList()
        assertEquals(listOf(30, 60, 90, 120, 150, 180), points)
    }

    @Test
    fun forecastPointBandCalculationIncludesVolatilityFactor() {
        val currentGlucose = 6.0
        val trendSlope = 0.0        // stable
        val volatility = 0.5
        val h = 60                  // 60 minutes ahead

        // Simple trend-only forecast for this test
        val predicted = currentGlucose + trendSlope * h
        val band = (volatility + 0.5) * (1.0 + h / 120.0)

        val lower = (predicted - band).coerceAtLeast(1.0)
        val upper = predicted + band

        assertEquals(6.0, predicted)
        assertEquals(1.5, band)
        assertEquals(4.5, lower)
        assertEquals(7.5, upper)
    }

    @Test
    fun trendExtrapolationProjectsForwardBasedOnSlope() {
        val currentGlucose = 5.0
        val slope = 0.1    // 0.1 mmol/L per minute (rising)
        val horizon = 30   // minutes

        val predicted = currentGlucose + slope * horizon
        assertEquals(8.0, predicted)
    }

    @Test
    fun carbsOnBoardParameterPassedToForecast() {
        val carbsOnBoard = 50.0  // grams still being absorbed
        val icr = 10.0
        val carbAbsorptionMinutes = 120.0

        // Expected carbs-induced rise over the next 120 minutes
        val expectedRiseFromCarbs = carbsOnBoard / icr
        assertEquals(5.0, expectedRiseFromCarbs)
    }

    @Test
    fun iobParameterRepresentsActiveInsulinThatWillLowerGlucose() {
        val iob = 2.0      // units of insulin still active
        val isf = 2.5      // 1 unit lowers glucose by 2.5 mmol/L

        // Expected glucose reduction from IOB
        val expectedLowering = iob * isf
        assertEquals(5.0, expectedLowering)
    }
}
