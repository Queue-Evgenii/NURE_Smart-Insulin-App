package ua.nure.smartinsulinbackend.controller

import org.springframework.data.domain.Page
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import ua.nure.smartinsulinbackend.dto.InsulinDoseRequest
import ua.nure.smartinsulinbackend.dto.InsulinDoseResponse
import ua.nure.smartinsulinbackend.entity.User
import ua.nure.smartinsulinbackend.service.InsulinDoseService

@RestController
@RequestMapping("/api/doses")
class InsulinDoseController(private val insulinDoseService: InsulinDoseService) {

    @PostMapping
    fun addDose(
        @AuthenticationPrincipal user: User,
        @RequestBody request: InsulinDoseRequest,
    ): ResponseEntity<InsulinDoseResponse> =
        ResponseEntity.status(HttpStatus.CREATED).body(insulinDoseService.addDose(user, request))

    @GetMapping
    fun getDoses(
        @AuthenticationPrincipal user: User,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): Page<InsulinDoseResponse> =
        insulinDoseService.getDoses(user.id, page, size)

    @DeleteMapping("/{id}")
    fun deleteDose(
        @AuthenticationPrincipal user: User,
        @PathVariable id: Long,
    ): ResponseEntity<Void> {
        insulinDoseService.deleteDose(user.id, id)
        return ResponseEntity.noContent().build()
    }
}
