package ua.nure.smartinsulinbackend

import org.junit.jupiter.api.Test
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt
import kotlin.test.assertTrue

data class DatasetScenario(
    val scenarioId: Int,
    val scenarioGroup: String,
    val description: String,
    val carbsG: Double,
    val currentGlucose: Double,
    val icr: Double,
    val isf: Double,
    val targetGlucoseMin: Double,
    val targetGlucoseMax: Double,
    val iob: Double,
    val expectedBc: Double,
    val expectedBcSafe: Double,
)

/**
 * Validates the bolus calculation formula against a curated dataset of 50 clinical scenarios.
 *
 * Formula under test:
 *   BC = C/ICR + (BG - T_mid)/ISF - IOB
 *   where T_mid = (targetMin + targetMax) / 2
 *   BC_safe = 0 if BG < 4.0 mmol/L (hypoglycemia), otherwise BC
 *
 * Metrics reported:
 *   - MAE  (Mean Absolute Error, units of insulin)
 *   - RMSE (Root Mean Squared Error)
 *   - % of scenarios within ±0.5 U of expected
 */
class BolusDatasetTest {

    // ── Pure formula (mirrors BolusCalculationService logic) ──────────────────

    private fun round1(v: Double): Double = Math.round(v * 10.0) / 10.0

    private fun calculateBolus(s: DatasetScenario): Pair<Double, Double> {
        val bolusForCarbs = if (s.icr > 0) s.carbsG / s.icr else 0.0
        val targetMid = (s.targetGlucoseMin + s.targetGlucoseMax) / 2.0
        val correction = if (s.isf > 0) (s.currentGlucose - targetMid) / s.isf else 0.0
        val total = round1(max(0.0, bolusForCarbs + correction - s.iob))
        val safe = if (s.currentGlucose < 4.0) 0.0 else total
        return total to safe
    }

    // ── Dataset loader ────────────────────────────────────────────────────────

    private fun loadDataset(): List<DatasetScenario> {
        val stream = javaClass.getResourceAsStream("/insulin_dose_dataset.csv")
            ?: error("insulin_dose_dataset.csv not found in test resources")
        return stream.bufferedReader().use { reader ->
            reader.readLines()
                .drop(1)              // skip header
                .filter { it.isNotBlank() }
                .map { line ->
                    val c = line.split(",")
                    DatasetScenario(
                        scenarioId       = c[0].trim().toInt(),
                        scenarioGroup    = c[1].trim(),
                        description      = c[2].trim(),
                        carbsG           = c[3].trim().toDouble(),
                        currentGlucose   = c[4].trim().toDouble(),
                        icr              = c[5].trim().toDouble(),
                        isf              = c[6].trim().toDouble(),
                        targetGlucoseMin = c[7].trim().toDouble(),
                        targetGlucoseMax = c[8].trim().toDouble(),
                        iob              = c[9].trim().toDouble(),
                        expectedBc       = c[10].trim().toDouble(),
                        expectedBcSafe   = c[11].trim().toDouble(),
                    )
                }
        }
    }

    // ── Main test ─────────────────────────────────────────────────────────────

    @Test
    fun `bolus dataset - formula accuracy and safety logic`() {
        val scenarios = loadDataset()
        assertTrue(scenarios.size == 50, "Expected 50 scenarios, got ${scenarios.size}")

        data class Result(
            val scenario: DatasetScenario,
            val calculated: Double,
            val calculatedSafe: Double,
            val error: Double,
            val safeError: Double,
        )

        val results = scenarios.map { s ->
            val (calc, safe) = calculateBolus(s)
            Result(s, calc, safe, abs(calc - s.expectedBc), abs(safe - s.expectedBcSafe))
        }

        // ── Aggregate metrics ─────────────────────────────────────────────────

        val mae  = results.map { it.error }.average()
        val rmse = sqrt(results.map { it.error * it.error }.average())
        val pctWithin = results.count { it.error <= 0.5 } * 100.0 / scenarios.size

        val safeMae  = results.map { it.safeError }.average()
        val safeRmse = sqrt(results.map { it.safeError * it.safeError }.average())
        val safePct  = results.count { it.safeError <= 0.5 } * 100.0 / scenarios.size

        // ── Per-group metrics ─────────────────────────────────────────────────

        val byGroup = results.groupBy { it.scenario.scenarioGroup }

        // ── Console report ────────────────────────────────────────────────────

        println("\n╔══════════════════════════════════════════════════════════╗")
        println("║          BOLUS FORMULA DATASET VALIDATION REPORT        ║")
        println("╚══════════════════════════════════════════════════════════╝")
        println("Dataset : insulin_dose_dataset.csv")
        println("Scenarios: ${scenarios.size}  (6 groups)")
        println()

        println("┌─────────────────────────────────────────────────────────┐")
        println("│  FORMULA ACCURACY  (BC = C/ICR + (BG-T)/ISF - IOB)     │")
        println("├──────────────────────┬──────────────────────────────────┤")
        println("│ MAE                  │ ${"%.4f".format(mae)} U                         │")
        println("│ RMSE                 │ ${"%.4f".format(rmse)} U                         │")
        println("│ Within ±0.5 U        │ ${results.count{it.error<=0.5}}/${scenarios.size} (${"%.1f".format(pctWithin)}%)                   │")
        println("└──────────────────────┴──────────────────────────────────┘")
        println()

        println("┌─────────────────────────────────────────────────────────┐")
        println("│  SAFETY LOGIC ACCURACY  (BC_safe = 0 when BG < 4.0)    │")
        println("├──────────────────────┬──────────────────────────────────┤")
        println("│ MAE                  │ ${"%.4f".format(safeMae)} U                         │")
        println("│ RMSE                 │ ${"%.4f".format(safeRmse)} U                         │")
        println("│ Within ±0.5 U        │ ${results.count{it.safeError<=0.5}}/${scenarios.size} (${"%.1f".format(safePct)}%)                   │")
        println("└──────────────────────┴──────────────────────────────────┘")
        println()

        println("┌──────────────────────────────────┬────────┬────────┬────────────┐")
        println("│ Group                            │ Count  │  MAE   │ ±0.5 U     │")
        println("├──────────────────────────────────┼────────┼────────┼────────────┤")
        byGroup.entries.sortedBy { it.key }.forEach { (group, rows) ->
            val gMae = rows.map { it.error }.average()
            val gPct = rows.count { it.error <= 0.5 } * 100.0 / rows.size
            println("│ %-32s │  %3d   │ %6.4f │  %5.1f%%    │".format(group, rows.size, gMae, gPct))
        }
        println("└──────────────────────────────────┴────────┴────────┴────────────┘")
        println()

        println("┌─────────────────────────────────────────────────────────┐")
        println("│  HYPOGLYCEMIA SAFETY CHECK  (BG < 4.0 → dose must = 0) │")
        println("└─────────────────────────────────────────────────────────┘")
        val hypo = results.filter { it.scenario.scenarioGroup == "hypoglycemia_no_insulin" }
        hypo.forEach { r ->
            val blocked = r.calculatedSafe == 0.0
            val mark = if (blocked) "✓" else "✗"
            println("  $mark  #${r.scenario.scenarioId} BG=${r.scenario.currentGlucose} mmol/L" +
                    "  raw=${r.calculated}  safe=${r.calculatedSafe}")
        }
        val hypoBlocked = hypo.count { it.calculatedSafe == 0.0 }
        println("  Blocked: $hypoBlocked/${hypo.size}")
        println()

        if (results.any { it.error > 0.05 }) {
            println("⚠ Scenarios with error > 0.05 U:")
            results.filter { it.error > 0.05 }.forEach { r ->
                println("  #${r.scenario.scenarioId} [${r.scenario.scenarioGroup}] " +
                        "calc=${r.calculated}  expected=${r.scenario.expectedBc}  Δ=${"%.3f".format(r.error)}")
            }
            println()
        }

        println("═══════════════════════════════════════════════════════════")

        // ── Assertions ────────────────────────────────────────────────────────

        assertTrue(mae < 0.1,
            "MAE must be < 0.1 U — got ${"%.4f".format(mae)}")
        assertTrue(rmse < 0.15,
            "RMSE must be < 0.15 U — got ${"%.4f".format(rmse)}")
        assertTrue(pctWithin >= 95.0,
            "≥ 95% must be within ±0.5 U — got ${"%.1f".format(pctWithin)}%")
        assertTrue(safePct >= 95.0,
            "Safety ≥ 95% within ±0.5 U — got ${"%.1f".format(safePct)}%")
        assertTrue(hypoBlocked == hypo.size,
            "All hypoglycemia cases must be blocked (dose = 0) — $hypoBlocked/${hypo.size}")
    }
}
