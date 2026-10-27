package com.vitoroliveira.gymtracker.domain.usecase

import com.vitoroliveira.gymtracker.data.model.WorkoutSet
import com.vitoroliveira.gymtracker.domain.repository.WorkoutRepository
import com.vitoroliveira.gymtracker.watch.WatchWorkout
import com.vitoroliveira.gymtracker.watch.epochMsToIso

/**
 * Grava no Supabase um treino feito no relógio. Pode ser chamado de novo com o mesmo treino
 * (a Data Layer entrega de novo se o celular cair no meio): o id do treino vem do relógio,
 * então um treino já fechado não é duplicado e as séries só entram se ainda não entraram.
 *
 * Ordem: cria o treino aberto → grava as séries (uma chamada) → fecha com a hora do relógio.
 * Fechar por último faz o gatilho do feed (`trg_workout_to_post`, AFTER UPDATE OF finished_at) contar as séries.
 */
class ImportWatchWorkout(private val workouts: WorkoutRepository) {

    enum class Result { SAVED, ALREADY_SAVED, NOTHING_TO_SAVE }

    /**
     * [knownGroupIds]/[knownExerciseIds]: o que ainda existe na conta. Grupo apagado vira treino sem grupo;
     * séries de exercício apagado são descartadas (senão a chave estrangeira derruba o envio inteiro).
     */
    suspend operator fun invoke(w: WatchWorkout, knownGroupIds: Set<String>, knownExerciseIds: Set<String>): Result {
        val sets = w.sets.filter { it.exerciseId in knownExerciseIds }
        val existing = workouts.getWorkout(w.id)
        if (existing?.finishedAt != null) return Result.ALREADY_SAVED
        if (sets.isEmpty() && existing == null) return Result.NOTHING_TO_SAVE

        if (existing == null) {
            workouts.createWorkout(w.id, w.groupId?.takeIf { it in knownGroupIds }, epochMsToIso(w.startedAt))
        }
        if (workouts.getSetsForWorkout(w.id).isEmpty()) {
            workouts.addSets(toRows(w.id, sets))
        }
        workouts.finishWorkoutAt(w.id, epochMsToIso(w.finishedAt))
        return Result.SAVED
    }

    /** Numera de novo por exercício (descartar um exercício apagado não deixa buraco na numeração). */
    internal fun toRows(workoutId: String, sets: List<com.vitoroliveira.gymtracker.watch.WatchSet>): List<WorkoutSet> {
        val counter = mutableMapOf<String, Int>()
        return sets.sortedBy { it.completedAt }.map { s ->
            val n = (counter[s.exerciseId] ?: 0) + 1
            counter[s.exerciseId] = n
            WorkoutSet(
                workoutId = workoutId, exerciseId = s.exerciseId, setNumber = n,
                reps = s.reps, weightKg = s.weightKg, completedAt = epochMsToIso(s.completedAt)
            )
        }
    }
}
