package ua.nure.smartinsulinbackend.config

import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Profile
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import ua.nure.smartinsulinbackend.entity.User
import ua.nure.smartinsulinbackend.entity.UserProfile
import ua.nure.smartinsulinbackend.repository.UserProfileRepository
import ua.nure.smartinsulinbackend.repository.UserRepository

@Component
@Profile("!prod")
class DataSeeder(
    private val userRepository: UserRepository,
    private val userProfileRepository: UserProfileRepository,
    private val passwordEncoder: PasswordEncoder,
) : ApplicationRunner {

    private val log = LoggerFactory.getLogger(DataSeeder::class.java)

    override fun run(args: ApplicationArguments) {
        seedUsers()
    }

    private fun seedUsers() {
        val seeds = listOf(
            SeedUser(
                email = "patient1@example.com",
                rawPassword = "Test1234!",
                fullName = "Олена Коваленко",
                diabetesType = 1,
                profile = SeedProfile(
                    weightKg = 62.0,
                    heightCm = 165.0,
                    insulinSensitivityFactor = 2.5,
                    insulinToCarbRatio = 10.0,
                    targetGlucoseMin = 4.0,
                    targetGlucoseMax = 7.8,
                    durationOfInsulinAction = 4.0,
                    basalInsulinType = "Lantus",
                    bolusInsulinType = "NovoRapid",
                ),
            ),
            SeedUser(
                email = "patient2@example.com",
                rawPassword = "Test1234!",
                fullName = "Микола Бондаренко",
                diabetesType = 2,
                profile = SeedProfile(
                    weightKg = 88.0,
                    heightCm = 178.0,
                    insulinSensitivityFactor = 3.0,
                    insulinToCarbRatio = 12.0,
                    targetGlucoseMin = 4.4,
                    targetGlucoseMax = 8.3,
                    durationOfInsulinAction = 5.0,
                    basalInsulinType = "Tresiba",
                    bolusInsulinType = "Humalog",
                ),
            ),
        )

        for (seed in seeds) {
            if (userRepository.findByEmail(seed.email).isPresent) {
                log.debug("Seeder: користувач {} вже існує, пропускаємо", seed.email)
                continue
            }

            val user = userRepository.save(
                User(
                    email = seed.email,
                    password = passwordEncoder.encode(seed.rawPassword).toString(),
                    fullName = seed.fullName,
                    diabetesType = seed.diabetesType,
                ),
            )

            userProfileRepository.save(
                UserProfile(
                    user = user,
                    weightKg = seed.profile.weightKg,
                    heightCm = seed.profile.heightCm,
                    insulinSensitivityFactor = seed.profile.insulinSensitivityFactor,
                    insulinToCarbRatio = seed.profile.insulinToCarbRatio,
                    targetGlucoseMin = seed.profile.targetGlucoseMin,
                    targetGlucoseMax = seed.profile.targetGlucoseMax,
                    durationOfInsulinAction = seed.profile.durationOfInsulinAction,
                    basalInsulinType = seed.profile.basalInsulinType,
                    bolusInsulinType = seed.profile.bolusInsulinType,
                ),
            )

            log.info("Seeder: створено тестового користувача {} ({})", seed.fullName, seed.email)
        }
    }

    // --------------- private data classes -----------------------------------

    private data class SeedUser(
        val email: String,
        val rawPassword: String,
        val fullName: String,
        val diabetesType: Int?,
        val profile: SeedProfile,
    )

    private data class SeedProfile(
        val weightKg: Double?,
        val heightCm: Double?,
        val insulinSensitivityFactor: Double?,
        val insulinToCarbRatio: Double?,
        val targetGlucoseMin: Double?,
        val targetGlucoseMax: Double?,
        val durationOfInsulinAction: Double?,
        val basalInsulinType: String?,
        val bolusInsulinType: String?,
    )
}
