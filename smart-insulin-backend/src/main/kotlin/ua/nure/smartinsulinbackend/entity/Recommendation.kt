package ua.nure.smartinsulinbackend.entity

import jakarta.persistence.*
import java.time.Instant

enum class RecommendationType { DOSE_CALCULATION, FORECAST, ALERT }

@Entity
@Table(name = "recommendations")
class Recommendation(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,

    /** Тип рекомендації: розрахунок дози, прогноз, або сповіщення */
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    val recommendationType: RecommendationType,

    /** Рекомендована доза інсуліну (Од), якщо застосовно */
    val recommendedDose: Double? = null,

    /** Прогнозований рівень глюкози (ммоль/л) */
    val predictedGlucose: Double? = null,

    /** Горизонт прогнозу (хвилини) */
    val predictionHorizonMinutes: Int? = null,

    /** JSON з контекстними даними, що використовувались під час розрахунку */
    @Column(columnDefinition = "TEXT")
    val contextJson: String? = null,

    /** Текст рекомендації для відображення користувачу */
    @Column(columnDefinition = "TEXT")
    val message: String? = null,

    @Column(nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @Column(nullable = false)
    val isRead: Boolean = false
)
