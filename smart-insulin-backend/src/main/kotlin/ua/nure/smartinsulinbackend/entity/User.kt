package ua.nure.smartinsulinbackend.entity

import jakarta.persistence.*
import org.springframework.security.core.userdetails.UserDetails

@Entity
@Table(name = "users")
class User(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(unique = true, nullable = false)
    private val email: String,

    @Column(nullable = false)
    private val password: String,

    val fullName: String,
    val diabetesType: Int? = null // Специфічне поле для твого проекту
) : UserDetails {
    override fun getAuthorities() = emptyList<org.springframework.security.core.GrantedAuthority>()
    override fun getPassword() = password
    override fun getUsername() = email
}