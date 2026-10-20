package com.vitoroliveira.gymtracker.presentation.body

import kotlin.math.roundToInt

data class LoadSuggestion(val weightKg: Double, val reason: String)

/**
 * Sugere a carga da próxima série.
 *
 * Com histórico → dupla progressão (método usado em hipertrofia): bateu o topo da faixa de reps, sobe a carga;
 * ficou bem abaixo, reduz; no meio, repete. Sem histórico → um ponto de partida conservador a partir do peso
 * corporal e dos padrões de força (só para os 4 básicos). Sempre é só uma sugestão: o usuário edita.
 */
object LoadSuggester {
    /** Menor "pulo" de carga que costuma existir nas anilhas. */
    const val DEFAULT_STEP = 2.5

    fun roundToStep(kg: Double, step: Double = DEFAULT_STEP): Double =
        ((kg / step).roundToInt() * step).coerceAtLeast(0.0)

    /**
     * @param lastWeightKg/lastReps a última série deste exercício (da última sessão)
     * @param targetReps topo da faixa de reps planejada (ex.: 12)
     * @param setNumber 1 = primeira série do exercício neste treino
     */
    fun suggest(
        exerciseName: String,
        lastWeightKg: Double?,
        lastReps: Int?,
        targetReps: Int,
        bodyKg: Double?,
        sex: Sex?,
        age: Int?,
        step: Double = DEFAULT_STEP
    ): LoadSuggestion? {
        if (lastWeightKg != null && lastWeightKg > 0 && lastReps != null) {
            val w = lastWeightKg
            return when {
                lastReps >= targetReps -> {
                    val up = roundToStep(w * 1.025, step).let { if (it <= w) w + step else it }
                    LoadSuggestion(up, "Você fez $lastReps reps (meta $targetReps): suba um pouco.")
                }
                lastReps < targetReps - 3 -> {
                    val down = roundToStep(w * 0.95, step).let { if (it >= w) (w - step).coerceAtLeast(0.0) else it }
                    LoadSuggestion(down, "Só $lastReps reps na última vez: reduza um pouco para fechar a meta.")
                }
                else -> LoadSuggestion(w, "Mantenha e tente chegar a $targetReps reps.")
            }
        }
        // sem histórico: ponto de partida
        val lift = StrengthStandards.liftFor(exerciseName) ?: return null
        val bw = bodyKg?.takeIf { it > 0 } ?: return null
        val beginner1rm = StrengthStandards.oneRepMaxByLevel(lift, bw, sex).getValue(StrengthLevel.BEGINNER)
        // começa em ~70% da carga de um iniciante nas reps planejadas; mais cuidado acima de 50 anos
        val ageFactor = if (age != null && age >= 50) 0.9 else 1.0
        val start = roundToStep(loadForReps(beginner1rm, targetReps) * 0.7 * ageFactor, step)
        if (start <= 0) return null
        return LoadSuggestion(start, "Primeira vez: ponto de partida conservador para ${lift.label.lowercase()}. Ajuste ao sentir.")
    }
}
