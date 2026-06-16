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

    // Multiple concurrent sessions per user (up to a limit enforced in AuthService),
    // so this is many-to-one, not one-to-one.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    val user: User,

    /** When this session was opened — used to evict the oldest session past the limit. */
    @Column(nullable = false)
    val createdAt: Instant = Instant.now(),
)