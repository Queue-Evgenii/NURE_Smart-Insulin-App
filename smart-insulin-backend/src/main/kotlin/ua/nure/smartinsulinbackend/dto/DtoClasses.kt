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