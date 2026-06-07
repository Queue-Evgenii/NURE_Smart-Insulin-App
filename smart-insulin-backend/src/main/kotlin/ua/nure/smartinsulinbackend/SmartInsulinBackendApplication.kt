package ua.nure.smartinsulinbackend

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
class SmartInsulinBackendApplication

fun main(args: Array<String>) {
    runApplication<SmartInsulinBackendApplication>(*args)
}
