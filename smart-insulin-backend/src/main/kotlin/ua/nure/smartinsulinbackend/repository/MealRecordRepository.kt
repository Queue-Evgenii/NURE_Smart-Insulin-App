package ua.nure.smartinsulinbackend.repository

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import ua.nure.smartinsulinbackend.entity.MealRecord
import java.time.Instant

interface MealRecordRepository : JpaRepository<MealRecord, Long> {
    fun findByUserIdOrderByMealTimeDesc(userId: Long, pageable: Pageable): Page<MealRecord>
    fun findByUserIdAndMealTimeBetweenOrderByMealTimeAsc(
        userId: Long, from: Instant, to: Instant
    ): List<MealRecord>
}
