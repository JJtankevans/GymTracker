package com.vitoroliveira.gymtracker.presentation.circuit

/** Faixas de referência (ver docs/REFERENCIAS_TEMPOS.md). Servem de comparação, não de regra. */
enum class TrainingGoal(val label: String, val restRange: IntRange, val defaultRest: Int, val repsHint: String) {
    STRENGTH("Força", 120..300, 150, "1–6 reps"),
    HYPERTROPHY("Hipertrofia", 60..180, 90, "6–12 reps"),
    ENDURANCE("Resistência", 30..60, 45, "12–20 reps")
}

enum class RestVerdict { SHORT, IN_RANGE, LONG }

object ReferenceTimes {
    /** Ritmo inicial por repetição, antes de o app aprender o do usuário. */
    const val DEFAULT_SEC_PER_REP = 3.0
    /** Faixa de segundos por repetição considerada comum (2–4 s). */
    val SEC_PER_REP_RANGE = 2.0..4.0

    fun restVerdict(goal: TrainingGoal, restSeconds: Int): RestVerdict = when {
        restSeconds < goal.restRange.first -> RestVerdict.SHORT
        restSeconds > goal.restRange.last -> RestVerdict.LONG
        else -> RestVerdict.IN_RANGE
    }
}
