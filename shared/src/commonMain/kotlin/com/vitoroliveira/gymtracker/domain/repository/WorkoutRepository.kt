// shared/src/commonMain/kotlin/com/vitoroliveira/gymtracker/domain/repository/WorkoutRepository.kt

package com.vitoroliveira.gymtracker.domain.repository

import com.vitoroliveira.gymtracker.data.model.ExerciseProgression
import com.vitoroliveira.gymtracker.data.model.Workout
import com.vitoroliveira.gymtracker.data.model.WorkoutSet

interface WorkoutRepository {
    suspend fun startWorkout(muscleGroupId: String?): Workout
    suspend fun finishWorkout(workoutId: String)
    suspend fun getHistory(): List<Workout>
    suspend fun addSet(workoutSet: WorkoutSet): WorkoutSet
    suspend fun getSetsForWorkout(workoutId: String): List<WorkoutSet>
    suspend fun getProgressionForExercise(exerciseId: String): List<ExerciseProgression>
}