package ua.nure.smartinsulinbackend.library

import com.sun.jna.Library

interface HbA1cLibrary : Library {
    fun calculate_hba1c_pure(measurements: DoubleArray, length: Long): Double
    fun calculate_iob(dose: Double, timeSinceInj: Double, dia: Double): Double

    // ── Glucose forecasting primitives (diploma section 2.2) ──────────────
    fun ols_slope(x: DoubleArray, y: DoubleArray, length: Long): Double
    fun ols_intercept(x: DoubleArray, y: DoubleArray, length: Long): Double
    fun std_dev(values: DoubleArray, length: Long): Double
    fun forecast_glucose(
        currentGlucose: Double,
        slopePerMin: Double,
        horizonMin: Double,
        iob: Double,
        isf: Double,
        diaMin: Double,
        carbsOnBoard: Double,
        icr: Double,
        carbAbsorptionMin: Double,
    ): Double
}