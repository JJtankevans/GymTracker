// shared/src/commonMain/kotlin/com/vitoroliveira/gymtracker/data/repository/MuscleGroupRepositoryImpl.kt

package com.vitoroliveira.gymtracker.data.repository

import com.vitoroliveira.gymtracker.data.model.MuscleGroup
import com.vitoroliveira.gymtracker.domain.repository.MuscleGroupRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest

class MuscleGroupRepositoryImpl(
    private val supabase: SupabaseClient
) : MuscleGroupRepository {

    override suspend fun getAll(): List<MuscleGroup> {
        return supabase.postgrest["muscle_groups"]
            .select()
            .decodeList()
    }

    override suspend fun insert(muscleGroup: MuscleGroup): MuscleGroup {
        val userId = supabase.auth.currentUserOrNull()?.id
            ?: throw IllegalStateException("Usuário não autenticado")

        return supabase.postgrest["muscle_groups"]
            .insert(muscleGroup.copy(profileId = userId)){
                select()
            }
            .decodeSingle()
    }

    override suspend fun update(muscleGroup: MuscleGroup) {
        supabase.postgrest["muscle_groups"]
            .update(muscleGroup) {
                filter { eq("id", muscleGroup.id!!) }
            }
    }

    override suspend fun delete(id: String) {
        supabase.postgrest["muscle_groups"]
            .delete {
                filter { eq("id", id) }
            }
    }
}