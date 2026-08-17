// shared/src/commonMain/kotlin/com/vitoroliveira/gymtracker/data/repository/ExerciseRepositoryImpl.kt

package com.vitoroliveira.gymtracker.data.repository

import com.vitoroliveira.gymtracker.data.model.Exercise
import com.vitoroliveira.gymtracker.domain.repository.ExerciseRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest

class ExerciseRepositoryImpl(
    private val supabase: SupabaseClient
) : ExerciseRepository {

    override suspend fun getAll(): List<Exercise> {
        return supabase.postgrest["exercises"]
            .select()
            .decodeList()
    }

    override suspend fun getByMuscleGroup(muscleGroupId: String): List<Exercise> {
        return supabase.postgrest["exercises"]
            .select {
                filter { eq("muscle_group_id", muscleGroupId) }
            }
            .decodeList()
    }

    override suspend fun insert(exercise: Exercise): Exercise {
        val userId = supabase.auth.currentUserOrNull()?.id
            ?: throw IllegalStateException("Usuário não autenticado")

        return supabase.postgrest["exercises"]
            .insert(exercise.copy(profileId = userId))
            .decodeSingle()
    }

    override suspend fun update(exercise: Exercise) {
        supabase.postgrest["exercises"]
            .update(exercise) {
                filter { eq("id", exercise.id!!) }
            }
    }

    override suspend fun delete(id: String) {
        supabase.postgrest["exercises"]
            .delete {
                filter { eq("id", id) }
            }
    }
}