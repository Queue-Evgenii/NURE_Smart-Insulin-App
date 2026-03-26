package ua.nure.smartinsulinbackend.service

import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service
import ua.nure.smartinsulinbackend.repository.UserRepository

@Service
class UserService(private val userRepository: UserRepository) : UserDetailsService {

    // Spring Security interface implementation
    override fun loadUserByUsername(username: String) =
        userRepository.findByEmail(username)
            .orElseThrow { UsernameNotFoundException("Користувача з email $username не знайдено") }

}