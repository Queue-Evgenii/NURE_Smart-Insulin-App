package ua.nure.smartinsulinbackend

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import ua.nure.smartinsulinbackend.dto.BolusRecommendationRequest
import ua.nure.smartinsulinbackend.service.GeminiService
import kotlin.test.assertTrue

data class RecommendationScenario(
    val scenarioId: Int,
    val scenarioName: String,
    val currentGlucose: Double,
    val glucoseTrend: Double,
    val bolusForCarbs: Double,
    val correctionDose: Double,
    val currentIob: Double,
    val totalDose: Double,
    val carbsG: Double,
    val plannedActivity: String?,
    val expectedFlags: Set<String>,
    val mustContainUk: String,   // Ukrainian word that must appear in response
    val mustNotContain: String,  // phrase that must NOT appear (safety check)
)

/**
 * Integration test for Gemini recommendation generation.
 *
 * Validates:
 *   1. Safety flags detected correctly (pre-LLM check)
 *   2. Response is in Ukrainian and non-empty
 *   3. Response contains expected clinical context keywords
 *   4. Response does NOT contain forbidden patterns (safety validator)
 *   5. Hypoglycemia case returns safe hardcoded message without LLM call
 *
 * Requires: GEMINI_API_KEY env variable.
 * Run: GEMINI_API_KEY=<key> ./gradlew test --tests "*.GeminiRecommendationTest"
 */
class GeminiRecommendationTest {

    private fun loadDataset(): List<RecommendationScenario> {
        val stream = javaClass.getResourceAsStream("/recommendation_dataset.csv")
            ?: error("recommendation_dataset.csv not found in test resources")
        return stream.bufferedReader().use { reader ->
            reader.readLines()
                .drop(1)
                .filter { it.isNotBlank() }
                .map { line ->
                    val c = line.split(",")
                    val flagsRaw = c[10].trim().trim('"')
                    RecommendationScenario(
                        scenarioId      = c[0].trim().toInt(),
                        scenarioName    = c[1].trim(),
                        currentGlucose  = c[2].trim().toDouble(),
                        glucoseTrend    = c[3].trim().toDouble(),
                        bolusForCarbs   = c[4].trim().toDouble(),
                        correctionDose  = c[5].trim().toDouble(),
                        currentIob      = c[6].trim().toDouble(),
                        totalDose       = c[7].trim().toDouble(),
                        carbsG          = c[8].trim().toDouble(),
                        plannedActivity = c[9].trim().ifBlank { null },
                        expectedFlags   = if (flagsRaw.isBlank()) emptySet()
                                          else flagsRaw.split("|").toSet(),
                        mustContainUk   = c[11].trim().trim('"'),
                        mustNotContain  = c[12].trim(),
                    )
                }
        }
    }

    @Test
    fun `Gemini recommendation - clinical accuracy and safety on dataset`() {
        val apiKey = System.getenv("GEMINI_API_KEY")
        assumeTrue(apiKey != null && apiKey.isNotBlank(),
            "GEMINI_API_KEY not set — skipping Gemini recommendation test")

        val service = GeminiService(apiKey!!, "gemini-2.0-flash", ObjectMapper())
        val scenarios = loadDataset()

        println("\n╔══════════════════════════════════════════════════════════════╗")
        println("║       GEMINI RECOMMENDATION — CLINICAL VALIDATION           ║")
        println("╚══════════════════════════════════════════════════════════════╝")
        println("Model   : gemini-2.0-flash")
        println("Dataset : recommendation_dataset.csv  (${scenarios.size} scenarios)")
        println()

        data class Result(
            val scenario: RecommendationScenario,
            val message: String,
            val actualFlags: List<String>,
            val flagsMatch: Boolean,
            val containsExpected: Boolean,
            val noForbidden: Boolean,
            val isUkrainian: Boolean,
        ) {
            val passed get() = flagsMatch && containsExpected && noForbidden && isUkrainian
        }

        val results = scenarios.map { s ->
            val req = BolusRecommendationRequest(
                currentGlucose = s.currentGlucose,
                glucoseTrend   = s.glucoseTrend,
                bolusForCarbs  = s.bolusForCarbs,
                correctionDose = s.correctionDose,
                currentIob     = s.currentIob,
                totalDose      = s.totalDose,
                carbsG         = s.carbsG,
                plannedActivity = s.plannedActivity,
            )
            val resp = service.generateRecommendation(req)

            val flagsMatch = s.expectedFlags.isEmpty() ||
                s.expectedFlags.all { it in resp.safetyFlags }
            val containsExpected = s.mustContainUk.isBlank() ||
                resp.message.contains(s.mustContainUk, ignoreCase = true)
            val noForbidden = s.mustNotContain.isBlank() ||
                !resp.message.contains(s.mustNotContain, ignoreCase = true)
            val isUkrainian = resp.message.any { it in 'А'..'я' || it == 'і' || it == 'ї' || it == 'є' || it == 'ґ' }

            Result(s, resp.message, resp.safetyFlags, flagsMatch, containsExpected, noForbidden, isUkrainian)
        }

        // ── Print report ──────────────────────────────────────────────────────

        results.forEach { r ->
            val mark = if (r.passed) "✓" else "✗"
            println("$mark  #${r.scenario.scenarioId} — ${r.scenario.scenarioName}")
            println("   Glucose: ${r.scenario.currentGlucose} mmol/L  |  Dose: ${r.scenario.totalDose} U  |  IOB: ${r.scenario.currentIob} U")
            println("   Flags detected : ${r.actualFlags.ifEmpty { listOf("none") }}")
            println("   Flags expected : ${r.scenario.expectedFlags.ifEmpty { setOf("any") }}")
            if (!r.flagsMatch)       println("   ✗ FLAGS MISMATCH")
            if (!r.containsExpected) println("   ✗ MISSING expected word: \"${r.scenario.mustContainUk}\"")
            if (!r.noForbidden)      println("   ✗ FORBIDDEN phrase found: \"${r.scenario.mustNotContain}\"")
            if (!r.isUkrainian)      println("   ✗ Response not in Ukrainian")
            println("   Response: ${r.message.take(200)}${if (r.message.length > 200) "…" else ""}")
            println()
        }

        val passed = results.count { it.passed }
        val pct    = passed * 100.0 / results.size

        println("╔══════════════════════════════════════════════════════════════╗")
        println("║  SUMMARY                                                    ║")
        println("╠══════════════════════════════════════════════════════════════╣")
        println("║  Passed  : $passed/${results.size} (${"%.1f".format(pct)}%)                                  ║")
        println("║  Flags OK: ${results.count{it.flagsMatch}}/${results.size}                                         ║")
        println("║  Content : ${results.count{it.containsExpected}}/${results.size} contain expected keywords             ║")
        println("║  Safety  : ${results.count{it.noForbidden}}/${results.size} passed safety validator               ║")
        println("║  Language: ${results.count{it.isUkrainian}}/${results.size} in Ukrainian                          ║")
        println("╚══════════════════════════════════════════════════════════════╝")

        // ── Assertions ────────────────────────────────────────────────────────

        assertTrue(pct >= 75.0,
            "At least 75% of scenarios must pass all checks, got ${"%.1f".format(pct)}%")

        // Safety-critical: hypoglycemia must always return hardcoded safe message
        val hypoResult = results.first { "HYPOGLYCEMIA" in it.scenario.expectedFlags }
        assertTrue("HYPOGLYCEMIA" in hypoResult.actualFlags,
            "HYPOGLYCEMIA flag must be detected")
        assertTrue(hypoResult.message.contains("НЕ вводьте", ignoreCase = true),
            "Hypoglycemia response must contain 'НЕ вводьте'")

        // All responses must be in Ukrainian
        assertTrue(results.all { it.isUkrainian },
            "All responses must contain Ukrainian text")
    }
}
