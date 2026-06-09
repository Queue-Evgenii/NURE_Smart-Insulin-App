package ua.nure.smartinsulinbackend.dto

import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import java.time.Instant

// ── Auth ──
data class LoginRequest(val email: String, val password: String)
data class RegisterRequest(val email: String, val password: String, val fullName: String, val diabetesType: Int? = null)
data class RefreshTokenRequest(val refreshToken: String)
data class TokenResponse(val accessToken: String, val refreshToken: String)

// ── Glucose ──
data class GlucoseReadingRequest(
    val glucoseValue: Double,
    val measurementType: String = "MANUAL",
    val measuredAt: Instant,
    val notes: String? = null,
)

data class GlucoseReadingResponse(
    val id: Long,
    val glucoseValue: Double,
    val measurementType: String,
    val measuredAt: Instant,
    val notes: String?,
    val createdAt: Instant,
)

// ── HbA1c ──
data class HbA1cRequest(
    val from: Instant,
    val to: Instant,
)

data class HbA1cResponse(
    val hba1c: Double,
    val readingsCount: Int,
    val averageGlucose: Double,
    val from: Instant,
    val to: Instant,
)

// ── User profile ──
data class UserProfileResponse(
    val email: String,
    val fullName: String,
    val diabetesType: Int?,
    val weightKg: Double?,
    val heightCm: Double?,
    val insulinSensitivityFactor: Double?,
    val insulinToCarbRatio: Double?,
    val targetGlucoseMin: Double?,
    val targetGlucoseMax: Double?,
    val durationOfInsulinAction: Double?,
    val insulinResistanceFactor: Double = 1.0,
    val carbAbsorptionMinutes: Int = 120,
    val activityCoefficients: ActivityCoefficientsDto = ActivityCoefficientsDto.DEFAULT,
    /** true when ISF/ICR were estimated from weight (1700/500 rules), not set manually */
    val usingWeightEstimation: Boolean = false,
    val basalInsulinType: String?,
    val bolusInsulinType: String?,
    /** true when ICR/ISF were derived from the patient's own history (section 2.1.1) */
    val usingAdaptiveCoefficients: Boolean = false,
    val lastCoefficientUpdate: Instant? = null,
)

data class UserProfileUpdateRequest(
    val fullName: String?,
    val diabetesType: Int?,

    @field:DecimalMin("20.0", message = "Вага повинна бути ≥ 20 кг")
    @field:DecimalMax("300.0", message = "Вага повинна бути ≤ 300 кг")
    val weightKg: Double?,

    @field:DecimalMin("50.0", message = "Зріст повинен бути ≥ 50 см")
    @field:DecimalMax("250.0", message = "Зріст повинен бути ≤ 250 см")
    val heightCm: Double?,

    @field:DecimalMin("0.5", message = "ISF повинен бути ≥ 0.5 ммоль/л/Од")
    @field:DecimalMax("20.0", message = "ISF повинен бути ≤ 20.0 ммоль/л/Од")
    val insulinSensitivityFactor: Double?,

    @field:DecimalMin("1.0", message = "ICR повинен бути ≥ 1 г/Од")
    @field:DecimalMax("50.0", message = "ICR повинен бути ≤ 50 г/Од")
    val insulinToCarbRatio: Double?,

    @field:DecimalMin("2.0", message = "Мін. цільова глюкоза ≥ 2.0 ммоль/л")
    @field:DecimalMax("7.0", message = "Мін. цільова глюкоза ≤ 7.0 ммоль/л")
    val targetGlucoseMin: Double?,

    @field:DecimalMin("4.0", message = "Макс. цільова глюкоза ≥ 4.0 ммоль/л")
    @field:DecimalMax("15.0", message = "Макс. цільова глюкоза ≤ 15.0 ммоль/л")
    val targetGlucoseMax: Double?,

    @field:DecimalMin("1.0", message = "DIA повинен бути ≥ 1 год")
    @field:DecimalMax("12.0", message = "DIA повинен бути ≤ 12 год")
    val durationOfInsulinAction: Double?,

    @field:DecimalMin("0.5", message = "Коефіцієнт резистентності ≥ 0.5")
    @field:DecimalMax("5.0", message = "Коефіцієнт резистентності ≤ 5.0")
    val insulinResistanceFactor: Double?,

    @field:Min(60, message = "Час всмоктування вуглеводів ≥ 60 хв")
    @field:Max(240, message = "Час всмоктування вуглеводів ≤ 240 хв")
    val carbAbsorptionMinutes: Int?,

    val activityCoefficients: ActivityCoefficientsDto?,

    val basalInsulinType: String?,
    val bolusInsulinType: String?,
)

// ── Activity coefficients (section 2.1.2) ──
data class ActivityCoefficientsDto(
    val lightAerobic: Double    = 0.90,
    val lightAnaerobic: Double  = 0.95,
    val lightMixed: Double      = 0.93,
    val moderateAerobic: Double = 0.75,
    val moderateAnaerobic: Double = 0.87,
    val moderateMixed: Double   = 0.82,
    val highAerobic: Double     = 0.60,
    val highAnaerobic: Double   = 0.77,
    val highMixed: Double       = 0.68,
    val maximalAerobic: Double  = 0.40,
    val maximalAnaerobic: Double = 0.67,
    val maximalMixed: Double    = 0.52,
) {
    fun validate(): List<String> {
        val errors = mutableListOf<String>()
        val fields = mapOf(
            "lightAerobic" to lightAerobic, "lightAnaerobic" to lightAnaerobic,
            "lightMixed" to lightMixed, "moderateAerobic" to moderateAerobic,
            "moderateAnaerobic" to moderateAnaerobic, "moderateMixed" to moderateMixed,
            "highAerobic" to highAerobic, "highAnaerobic" to highAnaerobic,
            "highMixed" to highMixed, "maximalAerobic" to maximalAerobic,
            "maximalAnaerobic" to maximalAnaerobic, "maximalMixed" to maximalMixed,
        )
        fields.forEach { (name, v) ->
            if (v < 0.10 || v > 1.00) errors += "$name must be between 0.10 and 1.00"
        }
        return errors
    }

    companion object {
        val DEFAULT = ActivityCoefficientsDto()
    }
}

// ── Meal records ──
data class MealRecordRequest(
    val mealName: String? = null,
    val carbohydratesG: Double,
    val glycemicIndex: Int? = null,
    val mealTime: Instant,
    val notes: String? = null,
)

data class MealRecordResponse(
    val id: Long,
    val mealName: String?,
    val carbohydratesG: Double,
    val glycemicIndex: Int?,
    val mealTime: Instant,
    val notes: String?,
    val createdAt: Instant,
)

// ── Insulin doses ──
data class InsulinDoseRequest(
    val doseUnits: Double,
    val doseType: String,
    val insulinType: String? = null,
    val injectedAt: Instant,
    val mealRecordId: Long? = null,
    val glucoseBefore: Double? = null,
    val notes: String? = null,
)

data class InsulinDoseResponse(
    val id: Long,
    val doseUnits: Double,
    val doseType: String,
    val insulinType: String?,
    val injectedAt: Instant,
    val mealRecordId: Long?,
    val mealName: String?,
    val glucoseBefore: Double?,
    val notes: String?,
    val createdAt: Instant,
)

// ── Carbs estimation via Gemini ──
data class CarbsEstimateRequest(val mealDescription: String)

data class CarbsEstimateResponse(
    val estimatedCarbsG: Int,
    val confidence: String,
    val breakdown: String,
    val mealDescription: String,
)

// ── AI Bolus recommendation ──
data class BolusRecommendationRequest(
    /** mmol/L */
    val currentGlucose: Double,
    /** mmol/L change over last 30 min — positive = rising, negative = falling */
    val glucoseTrend: Double = 0.0,
    val bolusForCarbs: Double,
    val correctionDose: Double,
    val currentIob: Double,
    val totalDose: Double,
    val carbsG: Double,
    /** Optional free-text description of planned physical activity */
    val plannedActivity: String? = null,
)

data class BolusRecommendationResponse(
    val message: String,
    /** Clinical flags detected before calling LLM, e.g. HYPOGLYCEMIA, HIGH_IOB */
    val safetyFlags: List<String>,
)

// ── Bolus calculation ──

/** Planned physical activity used to lower the dose (section 2.1.2). */
data class ActivityInput(
    /** AEROBIC | ANAEROBIC | MIXED */
    val type: String,
    /** LIGHT | MODERATE | HIGH | MAXIMAL */
    val intensity: String,
    /** Minutes between the injection and the start of activity */
    val minutesUntilStart: Int = 0,
    /** Planned activity duration in minutes */
    val durationMinutes: Int = 0,
)

data class BolusCalculationRequest(
    val currentGlucose: Double,
    val carbsG: Double,
    val mealRecordId: Long? = null,
    val activity: ActivityInput? = null,
)

data class BolusCalculationResponse(
    val bolusForCarbs: Double,
    val correctionDose: Double,
    val currentIob: Double,
    /** Base dose before any activity adjustment */
    val totalDose: Double,
    val mealRecordId: Long?,
    val missingParams: List<String>,
    /** true when ICR/ISF were derived from patient history (section 2.1.1) */
    val usingAdaptiveCoefficients: Boolean = false,
    /** Activity-adjusted dose; equals totalDose when no activity was supplied (section 2.1.2) */
    val adjustedDose: Double? = null,
    val activityFactor: Double? = null,
    val timeFactor: Double? = null,
    val durationFactor: Double? = null,
    val activityWarning: String? = null,
    /** true when ISF or ICR was estimated from body weight (1700/500 rules) */
    val usingWeightEstimation: Boolean = false,
)

// ── Glucose forecasting (section 2.2) ──
data class ForecastRequest(
    /** Carbohydrates recently consumed and still being absorbed (g) */
    val carbsOnBoard: Double = 0.0,
    /** Forecast horizon in minutes; defaults to 3 hours */
    val horizonMinutes: Int = 180,
)

data class ForecastPoint(
    val minutesAhead: Int,
    val predicted: Double,
    val lower: Double,
    val upper: Double,
)

data class ForecastResponse(
    val currentGlucose: Double,
    /** mmol/L per minute; negative = falling */
    val trendPerMinute: Double,
    val points: List<ForecastPoint>,
    /** e.g. HYPO_RISK, HYPER_RISK, LOW_CONFIDENCE */
    val riskFlags: List<String>,
    val readingsUsed: Int,
)