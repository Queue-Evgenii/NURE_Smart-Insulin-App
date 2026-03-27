package ua.nure.smartinsulinbackend.entity

import jakarta.persistence.*
import java.time.Instant

enum class MeasurementType { CGM, MANUAL }

@Entity
@Table(name = "glucose_readings")
class GlucoseReading(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,

    /** Рівень глюкози в крові (ммоль/л) */
    @Column(nullable = false)
    val glucoseValue: Double,

    /** Тип вимірювання: CGM (безперервний моніторинг) або MANUAL (ручне) */
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    val measurementType: MeasurementType = MeasurementType.MANUAL,

    @Column(nullable = false)
    val measuredAt: Instant,

    val notes: String? = null,

    @Column(nullable = false, updatable = false)
    val createdAt: Instant = Instant.now()
)
