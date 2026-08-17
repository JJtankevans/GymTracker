// shared/src/commonMain/kotlin/com/vitoroliveira/gymtracker/domain/repository/ExerciseRepository.kt

package com.vitoroliveira.gymtracker.domain.repository

import com.vitoroliveira.gymtracker.data.model.Exercise

interface ExerciseRepository {
    suspend fun getAll(): List<Exercise>
    suspend fun getByMuscleGroup(muscleGroupId: String): List<Exercise>
    suspend fun insert(exercise: Exercise): Exercise
    suspend fun update(exercise: Exercise)
    suspend fun delete(id: String)
}