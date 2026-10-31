// shared/src/commonMain/kotlin/com/vitoroliveira/gymtracker/domain/repository/WorkoutRepository.kt

package com.vitoroliveira.gymtracker.domain.repository

import com.vitoroliveira.gymtracker.data.model.ExerciseBaseline
import com.vitoroliveira.gymtracker.data.model.ExerciseProgression
import com.vitoroliveira.gymtracker.data.model.RecordSummary
import com.vitoroliveira.gymtracker.data.model.RecentSet
import com.vitoroliveira.gymtracker.data.model.Workout
import com.vitoroliveira.gymtracker.data.model.WorkoutSet

interface WorkoutRepository {
    suspend fun startWorkout(muscleGroupId: String?): Workout
    suspend fun finishWorkout(workoutId: String)
    /** Cria um treino com id e início dados (treino feito no relógio); ainda sem fim. */
    suspend fun createWorkout(workoutId: String, muscleGroupId: String?, startedAtIso: String)
    /** Fecha o treino com a hora dada (treino do relógio). Dispara o post do feed, como [finishWorkout]. */
    suspend fun finishWorkoutAt(workoutId: String, finishedAtIso: String)
    /** Grava várias séries em uma chamada (uma transação só no PostgREST). */
    suspend fun addSets(sets: List<WorkoutSet>)
    suspend fun deleteWorkout(workoutId: String)
    suspend fun getActiveWorkout(): Workout?
    suspend fun updateFinishedExercises(workoutId: String, finishedExerciseIds: List<String>)
    suspend fun getHistory(): List<Workout>
    /** Um treino pelo id (null se não existe ou não é meu). */
    suspend fun getWorkout(workoutId: String): Workout?
    /** Grava a anotação do treino (null ou vazio apaga). */
    suspend fun updateNotes(workoutId: String, notes: String?)
    suspend fun addSet(workoutSet: WorkoutSet): WorkoutSet
    suspend fun getSetsForWorkout(workoutId: String): List<WorkoutSet>
    suspend fun getProgressionForExercise(exerciseId: String): List<ExerciseProgression>
    /** Todas as séries de treinos iniciados a partir de [sinceIso] (instante ISO), em uma chamada. */
    suspend fun getSetsSince(sinceIso: String): List<RecentSet>
    /** Todas as minhas séries (treinos terminados), em páginas, para exportar. */
    suspend fun getAllSetsForExport(): List<com.vitoroliveira.gymtracker.data.model.ExportSet>
    /** Recordes de carga de todos os exercícios em UMA chamada (função `records_summary()`, migração 008). */
    suspend fun getRecordsSummary(): List<RecordSummary>
    suspend fun updateSet(setId: String, reps: Int, weightKg: Double): WorkoutSet
    suspend fun deleteSet(setId: String)

    /** Última vez e recorde do exercício, ignorando o treino [excludeWorkoutId]. */
    suspend fun getExerciseBaseline(exerciseId: String, excludeWorkoutId: String): ExerciseBaseline
}
