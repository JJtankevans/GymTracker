package com.vitoroliveira.gymtracker.presentation.workout

import kotlin.time.Instant

/** Acima disso a duração gravada é suspeita (treino que ficou aberto). */
const val MAX_PLAUSIBLE_WORKOUT_MINUTES = 240L

/**
 * Duração real de um treino. Se a gravada está entre 1 min e 4 h, vale ela. Senão (treino que ficou aberto por horas,
 * às vezes com as últimas séries lançadas bem depois) o treino termina na última série **antes de uma pausa maior que
 * 1 h** (mesma regra do treino esquecido, [StaleWorkoutPolicy]). Sem séries ou sem resultado plausível, devolve [recorded].
 * Só serve para exibir: o banco não é alterado.
 */
fun realDurationMinutes(recorded: Long?, start: Instant?, setTimes: List<String?>): Long? {
    if (recorded != null && recorded in 1..MAX_PLAUSIBLE_WORKOUT_MINUTES) return recorded
    if (start == null) return recorded
    val times = setTimes.mapNotNull { iso -> iso?.let { runCatching { Instant.parse(it) }.getOrNull() } }.sorted()
    if (times.isEmpty()) return recorded
    var last = times.first()
    for (t in times.drop(1)) {
        if (t - last > StaleWorkoutPolicy.IDLE_LIMIT) break
        last = t
    }
    val fromSets = (last - start).inWholeMinutes
    return if (fromSets in 1..MAX_PLAUSIBLE_WORKOUT_MINUTES) fromSets else recorded
}
