// shared/src/commonMain/kotlin/com/vitoroliveira/gymtracker/domain/repository/MuscleGroupRepository.kt

package com.vitoroliveira.gymtracker.domain.repository

import com.vitoroliveira.gymtracker.data.model.MuscleGroup

interface MuscleGroupRepository {
    suspend fun getAll(): List<MuscleGroup>
    suspend fun insert(muscleGroup: MuscleGroup): MuscleGroup
    suspend fun update(muscleGroup: MuscleGroup)
    suspend fun delete(id: String)
}