package com.vitoroliveira.gymtracker.watch

import kotlin.math.roundToInt

/** FC máxima sem idade conhecida (≈ fórmula de Tanaka para 26 anos). */
const val DEFAULT_MAX_HR = 190

/** FC máxima estimada pela fórmula de Tanaka (208 − 0,7 × idade). É uma estimativa: varia ±10 bpm entre pessoas. */
fun estimatedMaxHr(age: Int?): Int =
    if (age == null || age !in 10..110) DEFAULT_MAX_HR else (208 - 0.7 * age).roundToInt()

/** Zonas de 1 a 5 em % da FC máxima (50-60, 60-70, 70-80, 80-90, 90+). Abaixo de 50% = 0 (repouso). */
enum class HrZone(val label: String, val fromPct: Int) {
    REST("Repouso", 0),
    Z1("Leve", 50),
    Z2("Queima", 60),
    Z3("Aeróbica", 70),
    Z4("Limiar", 80),
    Z5("Máxima", 90);

    val number: Int get() = ordinal

    companion object {
        fun of(bpm: Int, maxHr: Int): HrZone {
            if (maxHr <= 0) return REST
            val pct = bpm * 100 / maxHr
            return entries.last { pct >= it.fromPct }
        }
    }
}

/** Amostras por zona (índice = [HrZone.ordinal]). Chegam ~1 por segundo, então valem como segundos. */
fun List<Int>.addZoneSample(zone: HrZone): List<Int> {
    val base = if (size == HrZone.entries.size) this else List(HrZone.entries.size) { getOrElse(it) { 0 } }
    return base.mapIndexed { i, v -> if (i == zone.ordinal) v + 1 else v }
}

/** Zona onde ficou mais tempo (sem contar repouso); null sem amostras de esforço. */
fun List<Int>.dominantZone(): HrZone? =
    withIndex().filter { it.index > 0 && it.value > 0 }.maxByOrNull { it.value }?.let { HrZone.entries[it.index] }
