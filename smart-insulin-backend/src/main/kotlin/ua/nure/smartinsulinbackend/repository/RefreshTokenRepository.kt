package ua.nure.smartinsulinbackend.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import ua.nure.smartinsulinbackend.entity.RefreshToken
import ua.nure.smartinsulinbackend.entity.User
import java.util.Optional

interface RefreshTokenRepository : JpaRepository<RefreshToken, Long> {
    fun findByToken(token: String): Optional<RefreshToken>

    @Modifying
    @Query("DELETE FROM RefreshToken r WHERE r.user = :user")
    fun deleteByUser(user: User)
}
