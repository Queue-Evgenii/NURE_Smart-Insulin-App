package ua.nure.smartinsulinbackend.controller

import org.springframework.data.domain.Page
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import ua.nure.smartinsulinbackend.dto.MealRecordRequest
import ua.nure.smartinsulinbackend.dto.MealRecordResponse
import ua.nure.smartinsulinbackend.entity.User
import ua.nure.smartinsulinbackend.service.MealRecordService

@RestController
@RequestMapping("/api/meals")
class MealRecordController(private val mealRecordService: MealRecordService) {

    @PostMapping
    fun addMeal(
        @AuthenticationPrincipal user: User,
        @RequestBody request: MealRecordRequest,
    ): ResponseEntity<MealRecordResponse> =
        ResponseEntity.status(HttpStatus.CREATED).body(mealRecordService.addMeal(user, request))

    @GetMapping
    fun getMeals(
        @AuthenticationPrincipal user: User,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): Page<MealRecordResponse> =
        mealRecordService.getMeals(user.id, page, size)

    @DeleteMapping("/{id}")
    fun deleteMeal(
        @AuthenticationPrincipal user: User,
        @PathVariable id: Long,
    ): ResponseEntity<Void> {
        mealRecordService.deleteMeal(user.id, id)
        return ResponseEntity.noContent().build()
    }
}
