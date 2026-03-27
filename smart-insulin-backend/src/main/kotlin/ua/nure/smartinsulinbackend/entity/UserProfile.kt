package ua.nure.smartinsulinbackend.entity

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "user_profiles")
class UserProfile(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    val user: User,

    /** Вага пацієнта (кг) */
    val weightKg: Double? = null,

    /** Зріст пацієнта (см) */
    val heightCm: Double? = null,

    /** Коефіцієнт чутливості до інсуліну: на скільки ммоль/л знижується глюкоза після 1 Од */
    val insulinSensitivityFactor: Double? = null,

    /** Інсуліно-вуглеводне співвідношення: скільки г вуглеводів перекриває 1 Од */
    val insulinToCarbRatio: Double? = null,

    /** Нижня межа цільового рівня глюкози (ммоль/л) */
    val targetGlucoseMin: Double? = null,

    /** Верхня межа цільового рівня глюкози (ммоль/л) */
    val targetGlucoseMax: Double? = null,

    /** Тривалість дії інсуліну (год), зазвичай 3–8 */
    val durationOfInsulinAction: Double? = null,

    /** Тип базального інсуліну (напр. Lantus, Tresiba) */
    val basalInsulinType: String? = null,

    /** Тип болюсного інсуліну (напр. NovoRapid, Humalog) */
    val bolusInsulinType: String? = null,

    @Column(nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    val updatedAt: Instant? = null
)
