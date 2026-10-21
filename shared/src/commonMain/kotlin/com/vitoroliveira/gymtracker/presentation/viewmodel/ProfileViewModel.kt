// shared/src/commonMain/kotlin/com/vitoroliveira/gymtracker/presentation/viewmodel/ProfileViewModel.kt

package com.vitoroliveira.gymtracker.presentation.viewmodel

import com.vitoroliveira.gymtracker.presentation.toUserMessage
import kotlin.coroutines.cancellation.CancellationException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vitoroliveira.gymtracker.data.model.Profile
import com.vitoroliveira.gymtracker.domain.AvatarRules
import com.vitoroliveira.gymtracker.domain.repository.AuthRepository
import com.vitoroliveira.gymtracker.domain.repository.ProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ProfileUiState(
    val isLoading: Boolean = false,
    val profile: Profile? = null,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val isSignedOut: Boolean = false,
    /** Enviando ou removendo a foto: a tela mostra um indicador sobre o avatar e bloqueia novo toque. */
    val isUpdatingAvatar: Boolean = false
)

class ProfileViewModel(
    private val profileRepository: ProfileRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadProfile()
    }

    fun loadProfile() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val profile = profileRepository.getCurrentProfile()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    profile = profile
                )
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.toUserMessage("Erro ao carregar perfil")
                )
            }
        }
    }

    /**
     * Atualiza a meta semanal de treinos (1 a 7 dias), validando
     * o mesmo intervalo do CHECK constraint da tabela `profiles`.
     */
    fun updateWeeklyGoal(weeklyGoal: Int) {
        val currentProfile = _uiState.value.profile
        if (currentProfile == null) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Perfil ainda não carregado"
            )
            return
        }
        if (weeklyGoal !in 1..7) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "A meta semanal deve ser entre 1 e 7 dias"
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                profileRepository.updateProfile(currentProfile.copy(weeklyGoal = weeklyGoal))
                loadProfile()
                _uiState.value = _uiState.value.copy(
                    successMessage = "Meta semanal atualizada"
                )
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.toUserMessage("Erro ao atualizar meta semanal")
                )
            }
        }
    }

    /** Recebe a foto já recortada em JPEG (o app prepara; ver AvatarRules), envia ao Storage e recarrega o perfil. */
    fun uploadAvatar(jpeg: ByteArray) {
        if (_uiState.value.isUpdatingAvatar) return
        if (jpeg.isEmpty()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Não foi possível ler essa imagem")
            return
        }
        if (jpeg.size > AvatarRules.MAX_BYTES) {
            _uiState.value = _uiState.value.copy(errorMessage = "A foto é grande demais (máximo 2 MB)")
            return
        }
        changeAvatar("Foto de perfil atualizada", "Erro ao enviar a foto") { profileRepository.uploadAvatar(jpeg) }
    }

    fun removeAvatar() {
        if (_uiState.value.isUpdatingAvatar || _uiState.value.profile?.avatarUrl == null) return
        changeAvatar("Foto de perfil removida", "Erro ao remover a foto") { profileRepository.removeAvatar() }
    }

    /** O app não conseguiu abrir/decodificar a imagem escolhida. */
    fun onAvatarReadFailed() {
        _uiState.value = _uiState.value.copy(errorMessage = "Não foi possível ler essa imagem")
    }

    private fun changeAvatar(success: String, failure: String, action: suspend () -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isUpdatingAvatar = true, errorMessage = null)
            try {
                action()
                // o perfil recarregado traz a nova avatar_url; sem isLoading para a tela não virar spinner
                val profile = profileRepository.getCurrentProfile()
                _uiState.value = _uiState.value.copy(
                    isUpdatingAvatar = false,
                    profile = profile,
                    successMessage = success
                )
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _uiState.value = _uiState.value.copy(
                    isUpdatingAvatar = false,
                    errorMessage = e.toUserMessage(failure)
                )
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
            _uiState.value = _uiState.value.copy(isSignedOut = true)
        }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(
            errorMessage = null,
            successMessage = null
        )
    }
}