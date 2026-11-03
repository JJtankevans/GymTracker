package com.vitoroliveira.gymtracker.presentation.records

import kotlin.math.roundToInt

/** Meta de carga de um exercício comparada com o recorde atual. */
data class GoalProgress(val targetKg: Double, val bestKg: Double) {
    val reached: Boolean get() = bestKg >= targetKg
    val remainingKg: Double get() = (targetKg - bestKg).coerceAtLeast(0.0)
    /** 0..1 do recorde até a meta. */
    val fraction: Float get() = if (targetKg <= 0) 0f else (bestKg / targetKg).toFloat().coerceIn(0f, 1f)
    val percent: Int get() = (fraction * 100).roundToInt()
}

/** Lê o que a pessoa digitou ("100", "102,5", "102.5 kg"); arredonda a 0,25 kg. Fora de 1..1000 = null. */
fun parseGoalKg(text: String): Double? {
    val clean = text.lowercase().replace("kg", "").trim().replace(',', '.')
    val v = clean.toDoubleOrNull() ?: return null
    if (v < 1 || v > 1000) return null
    return (v * 4).roundToInt() / 4.0
}

/** Formato guardado no aparelho: "id=kg;id=kg" (ids do Supabase são UUIDs, sem ';' nem '='). */
fun encodeGoals(goals: Map<String, Double>): String =
    goals.entries.joinToString(";") { "${it.key}=${it.value}" }

fun decodeGoals(raw: String?): Map<String, Double> =
    raw.orEmpty().split(';').mapNotNull { part ->
        val i = part.indexOf('=')
        if (i <= 0) null else part.substring(i + 1).toDoubleOrNull()?.let { part.substring(0, i) to it }
    }.toMap()
