// shared/src/commonMain/kotlin/com/vitoroliveira/gymtracker/domain/repository/ExerciseRepository.kt

package com.vitoroliveira.gymtracker.domain.repository

import com.vitoroliveira.gymtracker.data.model.Exercise

/** Um exercício pode estar em vários grupos: [Exercise.groupIds] vem preenchido e `insert`/`update` gravam esses vínculos. */
interface ExerciseRepository {
    suspend fun getAll(): List<Exercise>
    suspend fun getByMuscleGroup(muscleGroupId: String): List<Exercise>
    suspend fun insert(exercise: Exercise): Exercise
    /** Atualiza o exercício e deixa os vínculos exatamente em [Exercise.groupIds]. */
    suspend fun update(exercise: Exercise)
    suspend fun delete(id: String)
}
