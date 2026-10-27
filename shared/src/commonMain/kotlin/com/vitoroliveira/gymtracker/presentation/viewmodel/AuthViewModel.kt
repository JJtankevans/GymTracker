// shared/src/commonMain/kotlin/com/vitoroliveira/gymtracker/presentation/viewmodel/AuthViewModel.kt

package com.vitoroliveira.gymtracker.presentation.viewmodel

import com.vitoroliveira.gymtracker.presentation.toUserMessage
import kotlin.coroutines.cancellation.CancellationException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vitoroliveira.gymtracker.domain.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AuthUiState(
    val isLoading: Boolean = false,
    val isLoggedIn: Boolean = false,
    val errorMessage: String? = null,
    /**
     * Conta criada, mas sem sessão: o Supabase exige confirmar o e-mail (clicar no link) antes de entrar.
     * A tela de cadastro mostra o aviso com este endereço.
     */
    val confirmationEmail: String? = null,
    /** Aviso que não é erro (ex.: "E-mail reenviado"). */
    val infoMessage: String? = null
)

class AuthViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        _uiState.value = _uiState.value.copy(
            isLoggedIn = authRepository.isLoggedIn()
        )
    }

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                authRepository.signIn(email, password)
                _uiState.value = _uiState.value.copy(isLoading = false, isLoggedIn = true)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.toUserMessage("Erro ao fazer login")
                )
            }
        }
    }

    fun signUp(email: String, password: String, username: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                authRepository.signUp(email, password, username)
                // com a confirmação de e-mail ligada não vem sessão: entrar na Home faria tudo falhar com "sessão expirou"
                _uiState.value = if (authRepository.isLoggedIn()) {
                    _uiState.value.copy(isLoading = false, isLoggedIn = true)
                } else {
                    _uiState.value.copy(isLoading = false, confirmationEmail = email.trim())
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.toUserMessage("Erro ao criar conta")
                )
            }
        }
    }

    /** Manda de novo o e-mail de confirmação (o link às vezes cai no spam ou expira). */
    fun resendConfirmation() {
        val email = _uiState.value.confirmationEmail ?: return
        if (_uiState.value.isLoading) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                authRepository.resendSignUpConfirmation(email)
                _uiState.value = _uiState.value.copy(isLoading = false, infoMessage = "E-mail de confirmação reenviado para $email")
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = e.toUserMessage("Erro ao reenviar o e-mail"))
            }
        }
    }

    /** Fecha o aviso de confirmação (o usuário vai para o login). */
    fun dismissConfirmation() {
        _uiState.value = _uiState.value.copy(confirmationEmail = null)
    }

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
            _uiState.value = AuthUiState(isLoggedIn = false)
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null, infoMessage = null)
    }
}