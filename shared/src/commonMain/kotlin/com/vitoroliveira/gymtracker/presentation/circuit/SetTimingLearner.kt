package com.vitoroliveira.gymtracker.presentation.circuit

import kotlinx.serialization.Serializable

/** Uma série medida: quanto durou e quantas reps teve. */
@Serializable
data class TimingSample(val durationSec: Int, val reps: Int)

/** Onde os tempos aprendidos ficam guardados (SharedPreferences no Android). */
interface TimingStore {
    fun load(): Map<String, List<TimingSample>>
    fun save(data: Map<String, List<TimingSample>>)
}

class InMemoryTimingStore(private var data: Map<String, List<TimingSample>> = emptyMap()) : TimingStore {
    override fun load() = data
    override fun save(data: Map<String, List<TimingSample>>) { this.data = data }
}

/**
 * Aprende o ritmo do usuário em cada exercício.
 *
 * - Guarda as últimas [window] séries de cada exercício.
 * - Prevê a duração por **segundos por repetição** (mediana), então 8 e 12 reps
 *   têm previsões diferentes.
 * - Mediana + descarte de absurdos: uma pausa longa não estraga a previsão.
 * - Poucos dados (< [minSamples]): mistura com o ritmo padrão.
 */
class SetTimingLearner(
    initial: Map<String, List<TimingSample>> = emptyMap(),
    private val window: Int = 10,
    private val defaultSecPerRep: Double = 3.0,
    private val minSamples: Int = 3
) {
    private val data: MutableMap<String, List<TimingSample>> = initial.toMutableMap()

    fun snapshot(): Map<String, List<TimingSample>> = data.toMap()

    fun sampleCount(exerciseId: String): Int = valid(data[exerciseId].orEmpty()).size

    fun record(exerciseId: String, durationSec: Int, reps: Int) {
        if (reps <= 0 || durationSec !in MIN_SEC..MAX_SEC) return
        data[exerciseId] = (data[exerciseId].orEmpty() + TimingSample(durationSec, reps)).takeLast(window)
    }

    /** Segundos por repetição aprendidos (ou padrão, se ainda não há dados). */
    fun secPerRep(exerciseId: String): Double {
        val s = valid(data[exerciseId].orEmpty())
        if (s.isEmpty()) return defaultSecPerRep
        val learned = median(s.map { it.durationSec.toDouble() / it.reps })
        val w = (s.size.toDouble() / minSamples).coerceAtMost(1.0)
        return learned * w + defaultSecPerRep * (1 - w)
    }

    /** Duração prevista da série, em segundos. */
    fun predictSeconds(exerciseId: String, reps: Int): Int =
        (secPerRep(exerciseId) * reps).toInt().coerceIn(PRED_MIN, PRED_MAX)

    /** Mediana das durações medidas (para comparar com as faixas de referência). */
    fun typicalSeconds(exerciseId: String): Int? =
        valid(data[exerciseId].orEmpty()).takeIf { it.isNotEmpty() }?.let { median(it.map { s -> s.durationSec.toDouble() }).toInt() }

    /** Tira amostras absurdas: > 3x a mediana (com dados suficientes). */
    private fun valid(list: List<TimingSample>): List<TimingSample> {
        if (list.size < minSamples) return list
        val m = median(list.map { it.durationSec.toDouble() / it.reps })
        return list.filter { it.durationSec.toDouble() / it.reps <= m * 3 }
    }

    private fun median(v: List<Double>): Double {
        val s = v.sorted()
        return if (s.size % 2 == 1) s[s.size / 2] else (s[s.size / 2 - 1] + s[s.size / 2]) / 2
    }

    companion object {
        const val MIN_SEC = 5
        const val MAX_SEC = 300
        const val PRED_MIN = 10
        const val PRED_MAX = 180
    }
}
