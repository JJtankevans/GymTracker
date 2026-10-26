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
    /** Recordes de carga de todos os exercícios em UMA chamada (função `records_summary()`, migração 008). */
    suspend fun getRecordsSummary(): List<RecordSummary>
    suspend fun updateSet(setId: String, reps: Int, weightKg: Double): WorkoutSet
    suspend fun deleteSet(setId: String)

    /** Última vez e recorde do exercício, ignorando o treino [excludeWorkoutId]. */
    suspend fun getExerciseBaseline(exerciseId: String, excludeWorkoutId: String): ExerciseBaseline
}
