package ua.nure.smartinsulinbackend.service

import tools.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import ua.nure.smartinsulinbackend.dto.BolusRecommendationRequest
import ua.nure.smartinsulinbackend.dto.BolusRecommendationResponse
import ua.nure.smartinsulinbackend.dto.CarbsEstimateResponse
import ua.nure.smartinsulinbackend.repository.DocumentChunkRepository

@Service
class GeminiService(
    @Value("\${gemini.api.key:}") private val apiKey: String,
    @Value("\${gemini.model:gemini-2.0-flash}") private val model: String,
    private val objectMapper: ObjectMapper,
) {
    // Optional: injected in full Spring context; null when instantiated directly in tests
    @Autowired(required = false)
    private var embeddingService: EmbeddingService? = null

    @Autowired(required = false)
    private var documentChunkRepository: DocumentChunkRepository? = null

    private val log = LoggerFactory.getLogger(GeminiService::class.java)
    private val restClient = RestClient.create()

    // ── Carbs estimation ──────────────────────────────────────────────────────

    fun estimateCarbs(mealDescription: String): CarbsEstimateResponse {
        check(apiKey.isNotBlank()) { "gemini.api.key is not configured" }

        val prompt = """
            Ти дієтолог-асистент для хворих на цукровий діабет І типу.
            Визнач кількість вуглеводів у грамах для вказаної страви.

            Страва: "$mealDescription"

            Відповідь ЛИШЕ у форматі JSON, без жодного тексту до або після:
            {"carbs_g": <ціле число>, "confidence": "<low|medium|high>", "breakdown": "<1-2 речення з поясненням>"}
        """.trimIndent()

        val raw = callGemini(prompt)

        val text = extractText(raw)
        val json = stripFences(text)
        val node = objectMapper.readTree(json)
        return CarbsEstimateResponse(
            estimatedCarbsG = node["carbs_g"].asInt(),
            confidence      = node["confidence"]?.asText() ?: "medium",
            breakdown       = node["breakdown"]?.asText() ?: "",
            mealDescription = mealDescription,
        )
    }

    // ── Bolus recommendation ─────────────────────────────────────────────────

    fun generateRecommendation(req: BolusRecommendationRequest): BolusRecommendationResponse {
        check(apiKey.isNotBlank()) { "gemini.api.key is not configured" }

        val safetyFlags = detectSafetyFlags(req)

        // If critical hypoglycemia — return safe message without calling LLM
        if (safetyFlags.contains("HYPOGLYCEMIA")) {
            return BolusRecommendationResponse(
                message = "⚠️ Рівень глюкози нижче 3.9 ммоль/л — гіпоглікемія. " +
                    "НЕ вводьте інсулін. Негайно прийміть 15–20г швидких вуглеводів " +
                    "(сік, глюкоза, цукор) та перевірте глюкозу через 15 хвилин.",
                safetyFlags = safetyFlags,
            )
        }

        // Build RAG query from clinical context and retrieve relevant protocol fragments
        val ragQuery = buildRagQuery(req, safetyFlags)
        val protocolContext = retrieveProtocolContext(ragQuery)

        val systemInstruction = buildSystemInstruction(protocolContext)
        val userPrompt = buildClinicalPrompt(req, safetyFlags)

        val raw = callGemini(userPrompt, systemInstruction)
        val rawMessage = extractText(raw)

        val validated = validateResponse(rawMessage, req.totalDose)
        return BolusRecommendationResponse(message = validated, safetyFlags = safetyFlags)
    }

    // ── Safety pre-check ─────────────────────────────────────────────────────

    private fun detectSafetyFlags(req: BolusRecommendationRequest): List<String> {
        val flags = mutableListOf<String>()
        if (req.currentGlucose < 3.9)  flags += "HYPOGLYCEMIA"
        if (req.currentGlucose > 10.0) flags += "HIGH_GLUCOSE"
        if (req.currentIob > 2.5)      flags += "HIGH_IOB"
        if (req.totalDose > 15.0)      flags += "LARGE_DOSE"
        if (req.totalDose == 0.0 && req.currentGlucose >= 3.9) flags += "ZERO_DOSE"
        if (!req.plannedActivity.isNullOrBlank()) flags += "PHYSICAL_ACTIVITY"
        return flags
    }

    // ── Prompt builders ───────────────────────────────────────────────────────

    /**
     * Retrieves the most relevant fragments from medical protocol PDFs
     * (ADA Standards of Care 2025, МОЗ України Протокол ЦД І типу)
     * using cosine similarity search in pgvector.
     *
     * Falls back to an empty string if no documents have been ingested yet.
     */
    private fun retrieveProtocolContext(clinicalQuery: String): String {
        val es = embeddingService ?: return ""
        val repo = documentChunkRepository ?: return ""
        val chunks = try {
            val queryVector = es.embed(clinicalQuery)
            repo.findTopSimilar(queryVector, limit = 5)
        } catch (e: Exception) {
            log.warn("RAG retrieval failed, proceeding without protocol context: {}", e.message)
            return ""
        }

        if (chunks.isEmpty()) return ""

        return buildString {
            appendLine("РЕЛЕВАНТНІ ФРАГМЕНТИ З МЕДИЧНИХ ПРОТОКОЛІВ:")
            chunks.forEachIndexed { i, chunk ->
                appendLine("--- [${chunk.source}, фрагмент ${chunk.chunkIndex}] ---")
                appendLine(chunk.content.take(600))
            }
        }
    }

    private fun buildSystemInstruction(protocolContext: String) = buildString {
        appendLine("""
            Ти медичний асистент для хворих на цукровий діабет І типу.
            Ти допомагаєш пацієнту зрозуміти розраховану дозу інсуліну.
        """.trimIndent())

        if (protocolContext.isNotBlank()) {
            appendLine()
            append(protocolContext)
        }

        appendLine()
        appendLine("""
            ПРАВИЛА ВІДПОВІДІ:
            • Пиши українською мовою, зрозуміло для пацієнта без медичної освіти.
            • НЕ пропонуй іншу числову дозу — тільки пояснюй розраховану.
            • Максимум 4–5 речень.
            • Якщо є ризики — попереди коротко і чітко.
            • НЕ замінюй консультацію лікаря.
        """.trimIndent())
    }

    /** Short query used to retrieve semantically relevant protocol fragments via pgvector. */
    private fun buildRagQuery(req: BolusRecommendationRequest, flags: List<String>): String {
        val parts = mutableListOf("розрахунок болюсної дози інсуліну цукровий діабет І типу")
        if ("HYPOGLYCEMIA"     in flags) parts += "гіпоглікемія лікування"
        if ("HIGH_GLUCOSE"     in flags) parts += "гіперглікемія корекція"
        if ("HIGH_IOB"         in flags) parts += "активний інсулін передозування"
        if ("PHYSICAL_ACTIVITY" in flags) parts += "фізична активність інсулінотерапія"
        return parts.joinToString(". ")
    }

    private fun buildClinicalPrompt(req: BolusRecommendationRequest, flags: List<String>): String {
        val trendArrow = when {
            req.glucoseTrend > 0.3  -> "↑ зростає"
            req.glucoseTrend < -0.3 -> "↓ знижується"
            else                    -> "→ стабільна"
        }

        val activityLine = if (!req.plannedActivity.isNullOrBlank())
            "Планова фізична активність: ${req.plannedActivity}" else ""

        val flagsLine = if (flags.isNotEmpty())
            "Виявлені клінічні попередження: ${flags.joinToString()}" else ""

        return """
            КЛІНІЧНІ ДАНІ ПАЦІЄНТА:
            • Поточна глюкоза: ${req.currentGlucose} ммоль/л ($trendArrow за останні 30 хв)
            • Вуглеводи в страві: ${req.carbsG} г
            • Доза на вуглеводи: ${req.bolusForCarbs} Од
            • Корекційна доза: ${req.correctionDose} Од
            • Активний інсулін (IOB): ${req.currentIob} Од
            • Розрахована доза до введення: ${req.totalDose} Од
            ${if (activityLine.isNotEmpty()) "• $activityLine" else ""}
            ${if (flagsLine.isNotEmpty()) "• $flagsLine" else ""}

            ЗАВДАННЯ: Напиши пацієнту коротке пояснення цієї дози та будь-які важливі застереження.
        """.trimIndent()
    }

    // ── Response validator ───────────────────────────────────────────────────

    /**
     * Ensures the LLM response does not suggest a different dose or contain unsafe patterns.
     * If validation fails — returns a safe fallback message.
     */
    private fun validateResponse(message: String, totalDose: Double): String {
        val unsafePatterns = listOf(
            Regex("""введіть\s+\d+""", RegexOption.IGNORE_CASE),
            Regex("""збільшіть\s+дозу""", RegexOption.IGNORE_CASE),
            Regex("""зменшіть\s+дозу\s+до\s+\d+""", RegexOption.IGNORE_CASE),
        )
        val hasDifferentDose = unsafePatterns.any { it.containsMatchIn(message) }
        if (hasDifferentDose) {
            log.warn("Gemini response failed safety validation, returning fallback")
            return "Розрахована доза: ${totalDose} Од. " +
                "Для отримання персоналізованих рекомендацій зверніться до лікаря."
        }
        return message
    }

    // ── Low-level Gemini call ────────────────────────────────────────────────

    @Suppress("UNCHECKED_CAST")
    private fun callGemini(userText: String, systemInstruction: String? = null): Map<*, *> {
        check(apiKey.isNotBlank()) { "gemini.api.key is not configured" }
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"

        val body = buildMap<String, Any> {
            if (systemInstruction != null) {
                put("system_instruction", mapOf("parts" to listOf(mapOf("text" to systemInstruction))))
            }
            put("contents", listOf(mapOf("parts" to listOf(mapOf("text" to userText)))))
            put("generationConfig", mapOf("temperature" to 0.2))
        }

        log.debug("Gemini call model={}", model)
        return restClient.post()
            .uri(url)
            .contentType(MediaType.APPLICATION_JSON)
            .body(body)
            .retrieve()
            .body(Map::class.java) as Map<*, *>
    }

    @Suppress("UNCHECKED_CAST")
    private fun extractText(raw: Map<*, *>): String =
        (raw["candidates"] as? List<*>)
            ?.firstOrNull()?.let { it as? Map<*, *> }
            ?.let { it["content"] as? Map<*, *> }
            ?.let { it["parts"] as? List<*> }
            ?.firstOrNull()?.let { it as? Map<*, *> }
            ?.let { it["text"] as? String }
            ?: error("Unexpected Gemini response structure: $raw")

    private fun stripFences(text: String) =
        text.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
}
