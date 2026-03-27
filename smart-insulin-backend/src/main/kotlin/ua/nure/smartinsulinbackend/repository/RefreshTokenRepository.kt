package ua.nure.smartinsulinbackend.repository

import org.springframework.data.jpa.repository.JpaRepository
import ua.nure.smartinsulinbackend.entity.RefreshToken
import ua.nure.smartinsulinbackend.entity.User
import java.util.Optional

interface RefreshTokenRepository : JpaRepository<RefreshToken, Long> {
    fun findByToken(token: String): Optional<RefreshToken>
    fun deleteByUser(user: User)
}
