// shared/src/commonMain/kotlin/com/vitoroliveira/gymtracker/data/repository/AuthRepositoryImpl.kt

package com.vitoroliveira.gymtracker.data.repository

import com.vitoroliveira.gymtracker.domain.repository.AuthRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.OtpType
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.postgrest
import kotlin.coroutines.cancellation.CancellationException
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

    override suspend fun resendSignUpConfirmation(email: String) {
        supabase.auth.resendEmail(OtpType.Email.SIGNUP, email)
    }

    override suspend fun signIn(email: String, password: String) {
        supabase.auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
    }

    /** Sem internet o pedido de logout ao servidor falha; a sessão salva no aparelho é apagada mesmo assim. */
    override suspend fun signOut() {
        try {
            supabase.auth.signOut()
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            supabase.auth.clearSession()
        }
    }

    /** O servidor apaga o usuário (e tudo em cascata); depois só resta esquecer a sessão salva neste aparelho. */
    override suspend fun deleteAccount() {
        supabase.postgrest.rpc("delete_my_account")
        supabase.auth.clearSession()
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