package com.vitoroliveira.gymtracker.presentation.workout

import com.vitoroliveira.gymtracker.presentation.body.loadForReps
import kotlin.math.roundToLong

/** Anilhas de cada lado da barra para chegar a uma carga total. */
data class PlateResult(
    /** Anilhas de UM lado, da mais pesada para a mais leve. */
    val perSide: List<Double>,
    /** Carga total que dá para montar (barra + 2 × lado). */
    val achievedKg: Double,
    /** Quanto faltou para o alvo (0 quando fecha exato; > 0 quando as anilhas não permitem). */
    val missingKg: Double,
    /** O alvo é menor que a própria barra. */
    val belowBar: Boolean = false
)

object PlateCalculator {
    /** Anilhas comuns de academia (kg). */
    val defaultPlates = listOf(25.0, 20.0, 15.0, 10.0, 5.0, 2.5, 1.25)
    /** Barras mais comuns: olímpica 20 kg, feminina/curta 15 kg, W ou curta 10 kg. */
    val bars = listOf(20.0, 15.0, 10.0)

    /**
     * Monta cada lado com as maiores anilhas primeiro (com esses pesos isso sempre dá o menor número de anilhas).
     * As contas são em gramas para não errar com 1,25 kg.
     */
    fun plates(targetKg: Double, barKg: Double = 20.0, available: List<Double> = defaultPlates): PlateResult {
        val target = (targetKg * 1000).roundToLong()
        val bar = (barKg * 1000).roundToLong()
        if (target < bar) return PlateResult(emptyList(), barKg, 0.0, belowBar = true)
        var side = (target - bar) / 2
        val out = mutableListOf<Double>()
        for (p in available.map { (it * 1000).roundToLong() }.filter { it > 0 }.sortedDescending()) {
            while (side >= p) { out += p / 1000.0; side -= p }
        }
        val achieved = bar + 2 * out.sumOf { (it * 1000).roundToLong() }
        return PlateResult(out, achieved / 1000.0, (target - achieved) / 1000.0)
    }
}

/** Uma linha da tabela de cargas: quantas repetições, quanto peso e que % do 1RM isso é. */
data class RepMaxRow(val reps: Int, val weightKg: Double, val percent: Int)

/**
 * Cargas estimadas para cada número de repetições a partir do 1RM (Epley invertido), arredondadas para o múltiplo
 * de [stepKg] mais próximo (2,5 kg = menor salto com anilhas de 1,25 kg de cada lado). É estimativa, não prescrição.
 */
fun repMaxTable(oneRmKg: Double, reps: List<Int> = listOf(1, 2, 3, 4, 5, 6, 8, 10, 12, 15), stepKg: Double = 2.5): List<RepMaxRow> {
    if (oneRmKg <= 0) return emptyList()
    return reps.map { r ->
        val raw = loadForReps(oneRmKg, r)
        val rounded = (raw / stepKg).roundToLong() * stepKg
        RepMaxRow(r, rounded, ((raw / oneRmKg) * 100).roundToLong().toInt())
    }
}

/** Uma série de aquecimento sugerida. */
data class WarmupSet(val weightKg: Double, val reps: Int)

/**
 * Aquecimento clássico até a carga de trabalho: barra vazia ×10, ~40% ×8, ~60% ×5, ~80% ×3.
 * Cargas arredondadas para [stepKg], nunca abaixo da barra e sempre abaixo da carga de trabalho;
 * repetidas são puladas. Carga perto da barra (ou máquina sem barra e carga leve) = sem aquecimento.
 */
fun warmupSets(workKg: Double, barKg: Double = 20.0, stepKg: Double = 2.5): List<WarmupSet> {
    if (workKg <= barKg + stepKg || workKg <= 0) return emptyList()
    val out = mutableListOf<WarmupSet>()
    if (barKg > 0) out += WarmupSet(barKg, 10)
    for ((pct, reps) in listOf(0.4 to 8, 0.6 to 5, 0.8 to 3)) {
        val w = maxOf(barKg, (workKg * pct / stepKg).roundToLong() * stepKg)
        if (w <= 0 || w >= workKg || out.any { it.weightKg == w }) continue
        out += WarmupSet(w, reps)
    }
    return out
}
