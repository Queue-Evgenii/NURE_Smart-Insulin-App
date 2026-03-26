package ua.nure.smartinsulinbackend.entity

import jakarta.persistence.*
import java.time.Instant

@Entity
class RefreshToken(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false, unique = true)
    val token: String,

    @Column(nullable = false)
    val expiryDate: Instant,

    @OneToOne
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    val user: User
)