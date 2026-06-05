package ua.nure.smartinsulinbackend

import tools.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import ua.nure.smartinsulinbackend.service.GeminiService
import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.test.assertTrue

data class MealScenario(
    val category: String,
    val mealDescription: String,
    val expectedMin: Int,
    val expectedMax: Int,
)

/**
 * Integration test: validates Gemini carbs estimation accuracy on a curated dataset.
 *
 * Requires GEMINI_API_KEY environment variable. Skipped automatically if not set.
 * Run with: GEMINI_API_KEY=<key> ./gradlew test --tests "*.GeminiCarbsEstimationTest"
 *
 * Metrics reported:
 *   - Within expected range: count and %
 *   - MAE vs midpoint of expected range
 *   - Per-category breakdown
 */
class GeminiCarbsEstimationTest {

    private fun loadDataset(): List<MealScenario> {
        val stream = javaClass.getResourceAsStream("/meals_carbs_dataset.csv")
            ?: error("meals_carbs_dataset.csv not found in test resources")
        return stream.bufferedReader().use { reader ->
            reader.readLines()
                .drop(1)
                .filter { it.isNotBlank() }
                .map { line ->
                    val c = line.split(",")
                    MealScenario(
                        category        = c[0].trim(),
                        mealDescription = c[1].trim(),
                        expectedMin     = c[2].trim().toInt(),
                        expectedMax     = c[3].trim().toInt(),
                    )
                }
        }
    }

    @Test
    fun `Gemini carbs estimation - accuracy on dataset of real Ukrainian meals`() {
        val apiKey = System.getenv("GEMINI_API_KEY")
        assumeTrue(apiKey != null && apiKey.isNotBlank(),
            "GEMINI_API_KEY not set — skipping Gemini integration test")

        val service = GeminiService(apiKey!!, "gemini-2.5-flash", ObjectMapper())
        val scenarios = loadDataset()

        data class Result(
            val scenario: MealScenario,
            val estimated: Int,
            val confidence: String,
            val breakdown: String,
            val inRange: Boolean,
            val errorVsMid: Double,
        )

        val results = mutableListOf<Result>()

        println("\n╔══════════════════════════════════════════════════════════════╗")
        println("║       GEMINI CARBS ESTIMATION — DATASET VALIDATION          ║")
        println("╚══════════════════════════════════════════════════════════════╝")
        println("Model   : gemini-2.5-flash")
        println("Dataset : meals_carbs_dataset.csv  (${scenarios.size} meals)")
        println()

        for (s in scenarios) {
            val response = service.estimateCarbs(s.mealDescription)
            val mid = (s.expectedMin + s.expectedMax) / 2.0
            val inRange = response.estimatedCarbsG in s.expectedMin..s.expectedMax
            val err = abs(response.estimatedCarbsG - mid)
            results += Result(s, response.estimatedCarbsG, response.confidence, response.breakdown, inRange, err)

            val mark = if (inRange) "✓" else "✗"
            println("  $mark  [${s.category.padEnd(8)}]  \"${s.mealDescription}\"")
            println("       estimated=${response.estimatedCarbsG}г  range=[${s.expectedMin}–${s.expectedMax}]  conf=${response.confidence}")
            println("       ${response.breakdown}")
            println()
        }

        // ── Aggregate metrics ─────────────────────────────────────────────────

        val inRangeCount = results.count { it.inRange }
        val pctInRange = inRangeCount * 100.0 / results.size
        val mae = results.map { it.errorVsMid }.average()
        val rmse = sqrt(results.map { it.errorVsMid * it.errorVsMid }.average())

        // ── Per-category ──────────────────────────────────────────────────────

        val byCategory = results.groupBy { it.scenario.category }

        println("╔══════════════════════════════════════════════════════════════╗")
        println("║  SUMMARY                                                    ║")
        println("╠══════════════════════════════════════════════════════════════╣")
        println("║  Total meals          : ${scenarios.size.toString().padStart(3)}                               ║")
        println("║  Within expected range: ${inRangeCount.toString().padStart(3)}/${scenarios.size} (${"%.1f".format(pctInRange)}%)               ║")
        println("║  MAE  vs range midpoint: ${"%.1f".format(mae)} г                            ║")
        println("║  RMSE vs range midpoint: ${"%.1f".format(rmse)} г                            ║")
        println("╠══════════════════════════════════════════════════════════════╣")
        println("║  Per category:                                              ║")
        byCategory.forEach { (cat, rows) ->
            val catPct = rows.count { it.inRange } * 100.0 / rows.size
            val catMae = rows.map { it.errorVsMid }.average()
            println("║    %-10s  in-range=%5.1f%%  MAE=%5.1fg  (${rows.size} meals)       ║"
                .format(cat, catPct, catMae))
        }
        println("╚══════════════════════════════════════════════════════════════╝")

        // ── Assertions ────────────────────────────────────────────────────────

        assertTrue(pctInRange >= 60.0,
            "At least 60% of meals must be within expected carb range, got ${"%.1f".format(pctInRange)}%")
        assertTrue(mae <= 20.0,
            "MAE must be ≤ 20g vs range midpoint, got ${"%.1f".format(mae)}g")
    }
}
