package com.vitoroliveira.gymtracker.presentation.workout

import kotlin.math.roundToLong

/** Como a série que está sendo preparada se compara com a última vez e com o recorde. */
enum class ComparisonTone { UP, SAME, DOWN, RECORD }

data class SetComparison(val tone: ComparisonTone, val text: String)

/**
 * Frase curta para mostrar enquanto a pessoa ajusta carga e repetições, antes de registrar:
 * passa o recorde de carga? sobe, iguala ou cai em relação à última vez? Null sem histórico para comparar.
 * Carga zero (peso do corpo) compara só as repetições.
 */
fun compareSet(kg: Double, reps: Int, lastKg: Double?, lastReps: Int?, recordKg: Double?): SetComparison? {
    if (recordKg != null && kg > 0 && kg > recordKg) {
        return SetComparison(ComparisonTone.RECORD, "Recorde! +${formatKgBr(round2(kg - recordKg))} kg sobre ${formatKgBr(recordKg)} kg")
    }
    if (lastKg == null) return null
    val dKg = round2(kg - lastKg)
    if (dKg > 0) return SetComparison(ComparisonTone.UP, "+${formatKgBr(dKg)} kg vs. última")
    if (dKg < 0) return SetComparison(ComparisonTone.DOWN, "-${formatKgBr(-dKg)} kg vs. última")
    if (lastReps != null && reps != lastReps) {
        val d = reps - lastReps
        val word = if (kotlin.math.abs(d) == 1) "rep" else "reps"
        return if (d > 0) SetComparison(ComparisonTone.UP, "+$d $word vs. última")
        else SetComparison(ComparisonTone.DOWN, "$d $word vs. última")
    }
    return SetComparison(ComparisonTone.SAME, "Igual à última vez")
}

private fun round2(v: Double): Double = (v * 100).roundToLong() / 100.0
