package ua.nure.smartinsulinbackend.entity

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "meal_records")
class MealRecord(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,

    val mealName: String? = null,

    /** Кількість вуглеводів у їжі (грами) */
    @Column(nullable = false)
    val carbohydratesG: Double,

    /** Глікемічний індекс страви (1–100) */
    val glycemicIndex: Int? = null,

    @Column(nullable = false)
    val mealTime: Instant,

    val notes: String? = null,

    @Column(nullable = false, updatable = false)
    val createdAt: Instant = Instant.now()
)
