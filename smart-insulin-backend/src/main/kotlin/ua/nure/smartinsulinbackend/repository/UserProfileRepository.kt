package ua.nure.smartinsulinbackend.repository

import org.springframework.data.jpa.repository.JpaRepository
import ua.nure.smartinsulinbackend.entity.UserProfile
import java.util.Optional

interface UserProfileRepository : JpaRepository<UserProfile, Long> {
    fun findByUserId(userId: Long): Optional<UserProfile>
}
