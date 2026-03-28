package ua.nure.smartinsulinbackend.controller

import org.springframework.data.domain.Page
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import ua.nure.smartinsulinbackend.dto.GlucoseReadingRequest
import ua.nure.smartinsulinbackend.dto.GlucoseReadingResponse
import ua.nure.smartinsulinbackend.dto.HbA1cRequest
import ua.nure.smartinsulinbackend.dto.HbA1cResponse
import ua.nure.smartinsulinbackend.entity.User
import ua.nure.smartinsulinbackend.service.GlucoseReadingService

@RestController
@RequestMapping("/api/glucose")
class GlucoseReadingController(private val glucoseReadingService: GlucoseReadingService) {

    @PostMapping
    fun addReading(
        @AuthenticationPrincipal user: User,
        @RequestBody request: GlucoseReadingRequest,
    ): ResponseEntity<GlucoseReadingResponse> =
        ResponseEntity.status(HttpStatus.CREATED).body(glucoseReadingService.addReading(user, request))

    @GetMapping
    fun getReadings(
        @AuthenticationPrincipal user: User,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): Page<GlucoseReadingResponse> =
        glucoseReadingService.getReadings(user.id, page, size)

    @DeleteMapping("/{id}")
    fun deleteReading(
        @AuthenticationPrincipal user: User,
        @PathVariable id: Long,
    ): ResponseEntity<Void> {
        glucoseReadingService.deleteReading(user.id, id)
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/hba1c")
    fun calculateHbA1c(
        @AuthenticationPrincipal user: User,
        @RequestBody request: HbA1cRequest,
    ): HbA1cResponse =
        glucoseReadingService.calculateHbA1c(user.id, request.from, request.to)
}
