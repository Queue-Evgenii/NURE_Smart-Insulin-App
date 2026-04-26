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

// ── Bolus calculation ──
data class BolusCalculationRequest(
    val currentGlucose: Double,
    val carbsG: Double,
    val mealRecordId: Long? = null,
)

data class BolusCalculationResponse(
    val bolusForCarbs: Double,
    val correctionDose: Double,
    val currentIob: Double,
    val totalDose: Double,
    val mealRecordId: Long?,
    val missingParams: List<String>,
)