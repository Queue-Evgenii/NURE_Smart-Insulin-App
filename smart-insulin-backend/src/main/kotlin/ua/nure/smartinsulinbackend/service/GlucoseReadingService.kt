package ua.nure.smartinsulinbackend.service

import com.sun.jna.NativeLong
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ua.nure.smartinsulinbackend.dto.*
import ua.nure.smartinsulinbackend.entity.GlucoseReading
import ua.nure.smartinsulinbackend.entity.MeasurementType
import ua.nure.smartinsulinbackend.entity.User
import ua.nure.smartinsulinbackend.library.HbA1cLibrary
import ua.nure.smartinsulinbackend.repository.GlucoseReadingRepository
import java.time.Instant

@Service
@Transactional
class GlucoseReadingService(
    private val glucoseReadingRepository: GlucoseReadingRepository,
    private val hbA1cLibrary: HbA1cLibrary,
) {

    fun addReading(user: User, request: GlucoseReadingRequest): GlucoseReadingResponse {
        val reading = glucoseReadingRepository.save(
            GlucoseReading(
                user = user,
                glucoseValue = request.glucoseValue,
                measurementType = MeasurementType.valueOf(request.measurementType),
                measuredAt = request.measuredAt,
                notes = request.notes,
            )
        )
        return reading.toResponse()
    }

    @Transactional(readOnly = true)
    fun getReadings(userId: Long, page: Int, size: Int): Page<GlucoseReadingResponse> {
        return glucoseReadingRepository
            .findByUserIdOrderByMeasuredAtDesc(userId, PageRequest.of(page, size))
            .map { it.toResponse() }
    }

    fun deleteReading(userId: Long, readingId: Long) {
        val reading = glucoseReadingRepository.findById(readingId)
            .orElseThrow { IllegalArgumentException("Reading not found") }
        require(reading.user.id == userId) { "Access denied" }
        glucoseReadingRepository.delete(reading)
    }

    @Transactional(readOnly = true)
    fun calculateHbA1c(userId: Long, from: Instant, to: Instant): HbA1cResponse {
        val readings = glucoseReadingRepository
            .findByUserIdAndMeasuredAtBetweenOrderByMeasuredAtAsc(userId, from, to)

        if (readings.isEmpty()) {
            return HbA1cResponse(
                hba1c = 0.0,
                readingsCount = 0,
                averageGlucose = 0.0,
                from = from,
                to = to,
            )
        }

        val values = readings.map { it.glucoseValue }.toDoubleArray()
        val hba1c = hbA1cLibrary.calculate_hba1c_pure(values, NativeLong(values.size.toLong()))
        val avg = values.average()

        return HbA1cResponse(
            hba1c = hba1c,
            readingsCount = values.size,
            averageGlucose = avg,
            from = from,
            to = to,
        )
    }

    private fun GlucoseReading.toResponse() = GlucoseReadingResponse(
        id = id,
        glucoseValue = glucoseValue,
        measurementType = measurementType.name,
        measuredAt = measuredAt,
        notes = notes,
        createdAt = createdAt,
    )
}
