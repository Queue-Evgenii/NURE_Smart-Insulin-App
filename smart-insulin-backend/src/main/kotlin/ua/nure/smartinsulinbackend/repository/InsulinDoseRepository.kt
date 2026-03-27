package ua.nure.smartinsulinbackend.repository

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import ua.nure.smartinsulinbackend.entity.InsulinDose
import java.time.Instant

interface InsulinDoseRepository : JpaRepository<InsulinDose, Long> {
    fun findByUserIdOrderByInjectedAtDesc(userId: Long, pageable: Pageable): Page<InsulinDose>
    fun findByUserIdAndInjectedAtBetweenOrderByInjectedAtAsc(
        userId: Long, from: Instant, to: Instant
    ): List<InsulinDose>
}
