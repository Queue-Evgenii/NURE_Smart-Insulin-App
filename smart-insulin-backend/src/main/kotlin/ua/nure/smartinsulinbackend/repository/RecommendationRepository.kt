package ua.nure.smartinsulinbackend.repository

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import ua.nure.smartinsulinbackend.entity.Recommendation
import ua.nure.smartinsulinbackend.entity.RecommendationType

interface RecommendationRepository : JpaRepository<Recommendation, Long> {
    fun findByUserIdOrderByCreatedAtDesc(userId: Long, pageable: Pageable): Page<Recommendation>
    fun countByUserIdAndIsReadFalse(userId: Long): Long
    fun findByUserIdAndRecommendationTypeOrderByCreatedAtDesc(
        userId: Long, type: RecommendationType, pageable: Pageable
    ): Page<Recommendation>
}
