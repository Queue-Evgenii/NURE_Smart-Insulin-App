package ua.nure.smartinsulinbackend.service

import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ua.nure.smartinsulinbackend.dto.InsulinDoseRequest
import ua.nure.smartinsulinbackend.dto.InsulinDoseResponse
import ua.nure.smartinsulinbackend.entity.DoseType
import ua.nure.smartinsulinbackend.entity.InsulinDose
import ua.nure.smartinsulinbackend.entity.User
import ua.nure.smartinsulinbackend.repository.InsulinDoseRepository
import ua.nure.smartinsulinbackend.repository.MealRecordRepository

@Service
@Transactional
class InsulinDoseService(
    private val insulinDoseRepository: InsulinDoseRepository,
    private val mealRecordRepository: MealRecordRepository,
) {

    fun addDose(user: User, request: InsulinDoseRequest): InsulinDoseResponse {
        val mealRecord = request.mealRecordId?.let {
            mealRecordRepository.findById(it)
                .orElseThrow { IllegalArgumentException("Meal record not found") }
                .also { m -> require(m.user.id == user.id) { "Access denied" } }
        }

        val dose = insulinDoseRepository.save(
            InsulinDose(
                user = user,
                doseUnits = request.doseUnits,
                doseType = DoseType.valueOf(request.doseType),
                insulinType = request.insulinType,
                injectedAt = request.injectedAt,
                mealRecord = mealRecord,
                glucoseBefore = request.glucoseBefore,
                notes = request.notes,
            )
        )
        return dose.toResponse()
    }

    @Transactional(readOnly = true)
    fun getDoses(userId: Long, page: Int, size: Int): Page<InsulinDoseResponse> =
        insulinDoseRepository
            .findByUserIdOrderByInjectedAtDesc(userId, PageRequest.of(page, size))
            .map { it.toResponse() }

    fun deleteDose(userId: Long, doseId: Long) {
        val dose = insulinDoseRepository.findById(doseId)
            .orElseThrow { IllegalArgumentException("Dose not found") }
        require(dose.user.id == userId) { "Access denied" }
        insulinDoseRepository.delete(dose)
    }

    private fun InsulinDose.toResponse() = InsulinDoseResponse(
        id = id,
        doseUnits = doseUnits,
        doseType = doseType.name,
        insulinType = insulinType,
        injectedAt = injectedAt,
        mealRecordId = mealRecord?.id,
        mealName = mealRecord?.mealName,
        glucoseBefore = glucoseBefore,
        notes = notes,
        createdAt = createdAt,
    )
}
