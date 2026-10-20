// shared/src/commonMain/kotlin/com/vitoroliveira/gymtracker/data/repository/AuthRepositoryImpl.kt

package com.vitoroliveira.gymtracker.data.repository

import com.vitoroliveira.gymtracker.domain.repository.AuthRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject

class AuthRepositoryImpl(
    private val supabase: SupabaseClient
) : AuthRepository {

    override suspend fun signUp(email: String, password: String, username: String) {
        supabase.auth.signUpWith(Email) {
            this.email = email
            this.password = password
            data = buildJsonObject {
                put("username", JsonPrimitive(username))
            }
        }
    }

    override suspend fun signIn(email: String, password: String) {
        supabase.auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
    }

    override suspend fun signOut() {
        supabase.auth.signOut()
    }

    override suspend fun awaitSession(): Boolean {
        supabase.auth.awaitInitialization()
        return isLoggedIn()
    }

    override fun isLoggedIn(): Boolean {
        return supabase.auth.currentSessionOrNull() != null
    }

    override fun currentUserId(): String? {
        return supabase.auth.currentUserOrNull()?.id
    }
}