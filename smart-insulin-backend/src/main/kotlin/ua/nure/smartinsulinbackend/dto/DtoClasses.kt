package ua.nure.smartinsulinbackend.dto

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
    val basalInsulinType: String?,
    val bolusInsulinType: String?,
    /** true when ICR/ISF were derived from the patient's own history (section 2.1.1) */
    val usingAdaptiveCoefficients: Boolean = false,
    val lastCoefficientUpdate: Instant? = null,
)

data class UserProfileUpdateRequest(
    val fullName: String?,
    val diabetesType: Int?,
    val weightKg: Double?,
    val heightCm: Double?,
    val insulinSensitivityFactor: Double?,
    val insulinToCarbRatio: Double?,
    val targetGlucoseMin: Double?,
    val targetGlucoseMax: Double?,
    val durationOfInsulinAction: Double?,
    val basalInsulinType: String?,
    val bolusInsulinType: String?,
)

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