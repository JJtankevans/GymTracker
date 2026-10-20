// shared/src/commonMain/kotlin/com/vitoroliveira/gymtracker/domain/repository/AuthRepository.kt

package com.vitoroliveira.gymtracker.domain.repository

interface AuthRepository {
    suspend fun signUp(email: String, password: String, username: String)
    suspend fun signIn(email: String, password: String)
    suspend fun signOut()
    /** Espera o supabase-kt carregar a sessão salva e diz se há usuário logado. */
    suspend fun awaitSession(): Boolean
    fun isLoggedIn(): Boolean
    fun currentUserId(): String?
}