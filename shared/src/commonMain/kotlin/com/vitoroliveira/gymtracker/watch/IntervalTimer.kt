package com.vitoroliveira.gymtracker.watch

import kotlinx.serialization.Serializable

/**
 * Timer de intervalos do relógio (HIIT, Tabata, EMOM): [prepSec] de preparo, depois [rounds] rodadas de
 * [workSec] de esforço seguidas de [restSec] de pausa (a última rodada não tem pausa; pausa 0 = sem pausa).
 * Tudo é calculado a partir do tempo decorrido, então pausar é só guardar quanto já passou.
 */
@Serializable
data class IntervalPlan(
    val workSec: Int = 40,
    val restSec: Int = 20,
    val rounds: Int = 8,
    val prepSec: Int = 5
) {
    val totalMs: Long
        get() = (prepSec + rounds * workSec + (rounds - 1).coerceAtLeast(0) * restSec) * 1000L

    fun normalized(): IntervalPlan = copy(
        workSec = workSec.coerceIn(5, 600),
        restSec = restSec.coerceIn(0, 600),
        rounds = rounds.coerceIn(1, 99),
        prepSec = prepSec.coerceIn(0, 60)
    )

    /** "40s / 20s × 8" ou "60s × 10" quando não há pausa. */
    val label: String
        get() = if (restSec > 0) "${workSec}s / ${restSec}s × $rounds" else "${workSec}s × $rounds"

    fun stateAt(elapsedMs: Long): IntervalState {
        val t = elapsedMs.coerceAtLeast(0)
        val prep = prepSec * 1000L
        if (t < prep) return IntervalState(IntervalPhase.PREP, 1, prep - t, prep)
        val work = workSec * 1000L
        val rest = restSec * 1000L
        var left = t - prep
        for (round in 1..rounds) {
            if (left < work) return IntervalState(IntervalPhase.WORK, round, work - left, work)
            left -= work
            if (round == rounds) break
            if (rest > 0) {
                if (left < rest) return IntervalState(IntervalPhase.REST, round, rest - left, rest)
                left -= rest
            }
        }
        return IntervalState(IntervalPhase.DONE, rounds, 0, 0)
    }
}

enum class IntervalPhase { PREP, WORK, REST, DONE }

/** Onde o timer está: fase, rodada (1-based), quanto falta da fase e o tamanho dela. */
data class IntervalState(val phase: IntervalPhase, val round: Int, val remainingMs: Long, val phaseMs: Long) {
    /** 1 no começo da fase, 0 no fim. */
    val fraction: Float get() = if (phaseMs <= 0) 0f else (remainingMs.toFloat() / phaseMs).coerceIn(0f, 1f)

    /** Mesma fase e rodada: serve para saber quando vibrar a troca. */
    fun samePhaseAs(other: IntervalState?): Boolean = other != null && other.phase == phase && other.round == round
}

/** Treinos prontos do timer. */
val IntervalPresets: List<Pair<String, IntervalPlan>> = listOf(
    "Tabata" to IntervalPlan(workSec = 20, restSec = 10, rounds = 8),
    "HIIT 40/20" to IntervalPlan(workSec = 40, restSec = 20, rounds = 10),
    "HIIT 30/30" to IntervalPlan(workSec = 30, restSec = 30, rounds = 10),
    "EMOM 10 min" to IntervalPlan(workSec = 60, restSec = 0, rounds = 10),
    "Prancha 45/15" to IntervalPlan(workSec = 45, restSec = 15, rounds = 4)
)
