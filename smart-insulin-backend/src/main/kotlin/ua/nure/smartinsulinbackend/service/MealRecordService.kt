package ua.nure.smartinsulinbackend.service

import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ua.nure.smartinsulinbackend.dto.MealRecordRequest
import ua.nure.smartinsulinbackend.dto.MealRecordResponse
import ua.nure.smartinsulinbackend.entity.MealRecord
import ua.nure.smartinsulinbackend.entity.User
import ua.nure.smartinsulinbackend.repository.MealRecordRepository

@Service
@Transactional
class MealRecordService(private val mealRecordRepository: MealRecordRepository) {

    fun addMeal(user: User, request: MealRecordRequest): MealRecordResponse {
        val record = mealRecordRepository.save(
            MealRecord(
                user = user,
                mealName = request.mealName,
                carbohydratesG = request.carbohydratesG,
                glycemicIndex = request.glycemicIndex,
                mealTime = request.mealTime,
                notes = request.notes,
            )
        )
        return record.toResponse()
    }

    @Transactional(readOnly = true)
    fun getMeals(userId: Long, page: Int, size: Int): Page<MealRecordResponse> =
        mealRecordRepository
            .findByUserIdOrderByMealTimeDesc(userId, PageRequest.of(page, size))
            .map { it.toResponse() }

    fun deleteMeal(userId: Long, mealId: Long) {
        val record = mealRecordRepository.findById(mealId)
            .orElseThrow { IllegalArgumentException("Meal not found") }
        require(record.user.id == userId) { "Access denied" }
        mealRecordRepository.delete(record)
    }

    private fun MealRecord.toResponse() = MealRecordResponse(
        id = id,
        mealName = mealName,
        carbohydratesG = carbohydratesG,
        glycemicIndex = glycemicIndex,
        mealTime = mealTime,
        notes = notes,
        createdAt = createdAt,
    )
}
