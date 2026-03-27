package ua.nure.smartinsulinbackend.entity

import jakarta.persistence.*
import java.time.Instant

enum class DoseType { BOLUS, BASAL, CORRECTION }

@Entity
@Table(name = "insulin_doses")
class InsulinDose(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,

    /** Кількість одиниць інсуліну */
    @Column(nullable = false)
    val doseUnits: Double,

    /** Тип дози: BOLUS (їдальній), BASAL (базальний), CORRECTION (корекційний) */
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    val doseType: DoseType,

    val insulinType: String? = null,

    @Column(nullable = false)
    val injectedAt: Instant,

    /** Прийом їжі, з яким пов'язане введення (необов'язково) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meal_record_id")
    val mealRecord: MealRecord? = null,

    /** Рівень глюкози до введення інсуліну (ммоль/л) */
    val glucoseBefore: Double? = null,

    val notes: String? = null,

    @Column(nullable = false, updatable = false)
    val createdAt: Instant = Instant.now()
)
