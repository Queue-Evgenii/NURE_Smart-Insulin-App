package ua.nure.smartinsulinbackend.config

import com.sun.jna.Native
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import ua.nure.smartinsulinbackend.library.HbA1cLibrary

@Configuration
class NativeLibraryConfig {

    @Bean
    fun hbA1cLibrary(): HbA1cLibrary {
        return Native.load("hba1c", HbA1cLibrary::class.java)
    }
}