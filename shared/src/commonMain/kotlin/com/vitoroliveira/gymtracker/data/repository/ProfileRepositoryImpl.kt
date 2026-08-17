// shared/src/commonMain/kotlin/com/vitoroliveira/gymtracker/data/repository/ProfileRepositoryImpl.kt

package com.vitoroliveira.gymtracker.data.repository

import com.vitoroliveira.gymtracker.data.model.Profile
import com.vitoroliveira.gymtracker.domain.repository.ProfileRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest

class ProfileRepositoryImpl(
    private val supabase: SupabaseClient
) : ProfileRepository {

    override suspend fun getCurrentProfile(): Profile {
        val userId = supabase.auth.currentUserOrNull()?.id
            ?: throw IllegalStateException("Usuário não autenticado")

        return supabase.postgrest["profiles"]
            .select {
                filter { eq("id", userId) }
            }
            .decodeSingle()
    }

    override suspend fun updateProfile(profile: Profile) {
        val id = profile.id
            ?: throw IllegalStateException("Perfil sem id — carregue o perfil antes de atualizar")

        supabase.postgrest["profiles"]
            .update(profile) {
                filter { eq("id", id) }
            }
    }
}