package com.vitoroliveira.gymtracker.presentation.workout

import com.vitoroliveira.gymtracker.data.model.Workout
import com.vitoroliveira.gymtracker.domain.repository.WorkoutRepository
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

/** O que fazer com um treino que ficou aberto. */
sealed interface StaleDecision {
    /** Ainda está valendo (houve atividade há pouco). */
    data object Keep : StaleDecision
    /** Esqueceram aberto e nunca teve série: apaga. */
    data object Discard : StaleDecision
    /** Esqueceram aberto: encerra no horário da última série (instante ISO). */
    data class FinishAt(val iso: String) : StaleDecision
}

/**
 * Treino esquecido: se passou mais de [IDLE_LIMIT] sem série nova (ou sem nenhuma série desde que começou),
 * quem treina provavelmente saiu sem finalizar. Aí o app encerra o treino com o horário da última série,
 * e a duração no Histórico fica real em vez de "17h 40min".
 */
object StaleWorkoutPolicy {
    val IDLE_LIMIT: Duration = 60.minutes

    fun decide(startedAt: String?, setTimes: List<String?>, now: Instant, limit: Duration = IDLE_LIMIT): StaleDecision {
        val parsed = setTimes.mapNotNull { iso -> iso?.let { runCatching { Instant.parse(it) }.getOrNull()?.let { t -> t to iso } } }
        val lastSet = parsed.maxByOrNull { it.first }
        val lastActivity = lastSet?.first
            ?: startedAt?.let { runCatching { Instant.parse(it) }.getOrNull() }
            ?: return StaleDecision.Keep
        if (now - lastActivity <= limit) return StaleDecision.Keep
        return if (lastSet == null) StaleDecision.Discard else StaleDecision.FinishAt(lastSet.second)
    }
}

/**
 * Confere o treino aberto e, se está esquecido, encerra/apaga. Devolve o treino se continua valendo,
 * ou null se foi encerrado/apagado (aí não há mais treino em andamento).
 */
suspend fun WorkoutRepository.settleIfStale(active: Workout, now: Instant): Workout? {
    val id = active.id ?: return active
    val sets = getSetsForWorkout(id)
    return when (val d = StaleWorkoutPolicy.decide(active.startedAt, sets.map { it.completedAt }, now)) {
        StaleDecision.Keep -> active
        StaleDecision.Discard -> { deleteWorkout(id); null }
        is StaleDecision.FinishAt -> { finishWorkoutAt(id, d.iso); null }
    }
}
