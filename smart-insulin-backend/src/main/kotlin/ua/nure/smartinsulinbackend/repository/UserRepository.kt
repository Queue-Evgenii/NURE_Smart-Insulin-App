package ua.nure.smartinsulinbackend.repository

import org.springframework.data.jpa.repository.JpaRepository
import ua.nure.smartinsulinbackend.entity.User
import java.util.Optional

interface UserRepository : JpaRepository<User, Long> {
    fun findByEmail(email: String): Optional<User>
}