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
}
