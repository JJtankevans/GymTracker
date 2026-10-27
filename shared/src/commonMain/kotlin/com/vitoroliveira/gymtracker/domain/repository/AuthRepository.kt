// shared/src/commonMain/kotlin/com/vitoroliveira/gymtracker/domain/repository/AuthRepository.kt

package com.vitoroliveira.gymtracker.domain.repository

interface AuthRepository {
    /**
     * Cria a conta. Com "Confirm email" ligado no Supabase a conta nasce sem sessão (o usuário precisa clicar no link do
     * e-mail antes de entrar): depois de chamar, confira [isLoggedIn].
     */
    suspend fun signUp(email: String, password: String, username: String)
    /** Manda de novo o e-mail de confirmação do cadastro. */
    suspend fun resendSignUpConfirmation(email: String)
    suspend fun signIn(email: String, password: String)
    suspend fun signOut()
    /** Espera o supabase-kt carregar a sessão salva e diz se há usuário logado. */
    suspend fun awaitSession(): Boolean
    fun isLoggedIn(): Boolean
    fun currentUserId(): String?
}