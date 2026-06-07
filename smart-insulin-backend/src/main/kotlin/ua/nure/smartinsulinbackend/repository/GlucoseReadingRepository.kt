package ua.nure.smartinsulinbackend.repository

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import ua.nure.smartinsulinbackend.entity.GlucoseReading
import java.time.Instant

interface GlucoseReadingRepository : JpaRepository<GlucoseReading, Long> {
    fun findByUserIdOrderByMeasuredAtDesc(userId: Long, pageable: Pageable): Page<GlucoseReading>
    fun findByUserIdAndMeasuredAtBetweenOrderByMeasuredAtAsc(
        userId: Long, from: Instant, to: Instant
    ): List<GlucoseReading>

    /** Earliest reading within a window — used to auto-detect post-prandial glucose (~3h after a dose). */
    fun findFirstByUserIdAndMeasuredAtBetweenOrderByMeasuredAtAsc(
        userId: Long, from: Instant, to: Instant
    ): GlucoseReading?

    /** Most recent readings, newest first — used for trend estimation in forecasting. */
    fun findTop12ByUserIdOrderByMeasuredAtDesc(userId: Long): List<GlucoseReading>
}
