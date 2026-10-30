// shared/src/commonMain/kotlin/com/vitoroliveira/gymtracker/presentation/viewmodel/ProfileViewModel.kt

package com.vitoroliveira.gymtracker.presentation.viewmodel

import com.vitoroliveira.gymtracker.presentation.toUserMessage
import kotlin.coroutines.cancellation.CancellationException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vitoroliveira.gymtracker.data.model.Profile
import com.vitoroliveira.gymtracker.data.model.ProfileTitle
import com.vitoroliveira.gymtracker.domain.repository.SocialRepository
import com.vitoroliveira.gymtracker.domain.AvatarRules
import com.vitoroliveira.gymtracker.domain.repository.AuthRepository
import com.vitoroliveira.gymtracker.domain.repository.LocalUserData
import com.vitoroliveira.gymtracker.domain.repository.ProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

data class ProfileUiState(
    val isLoading: Boolean = false,
    val profile: Profile? = null,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val isSignedOut: Boolean = false,
    /** Enviando ou removendo a foto: a tela mostra um indicador sobre o avatar e bloqueia novo toque. */
    val isUpdatingAvatar: Boolean = false,
    /** Meu título de desafio (honra ou falha), se houver. */
    val title: ProfileTitle? = null,
    /** Saindo da conta (enviando o que falta, encerrando a sessão e apagando os dados do aparelho). */
    val isSigningOut: Boolean = false,
    /** Excluindo a conta de vez (foto, dados no servidor e no aparelho). */
    val isDeletingAccount: Boolean = false
)

class ProfileViewModel(
    private val profileRepository: ProfileRepository,
    private val authRepository: AuthRepository,
    private val socialRepository: SocialRepository? = null,
    private val localData: LocalUserData? = null
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
                loadTitleQuietly(profile)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.toUserMessage("Erro ao carregar perfil")
                )
            }
        }
    }

    /** O título é um enfeite: se falhar (ex.: migração 010 ainda não rodou), o perfil segue sem ele e sem erro. */
    private suspend fun loadTitleQuietly(profile: Profile) {
        val id = profile.id ?: return
        val repo = socialRepository ?: return
        try {
            _uiState.value = _uiState.value.copy(title = repo.titles(listOf(id)).firstOrNull())
        } catch (e: Exception) {
            if (e is CancellationException) throw e
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

    /** Erro vindo de fora do ViewModel (ex.: exportação), mostrado no mesmo lugar das outras mensagens. */
    fun onExternalError(message: String) {
        _uiState.value = _uiState.value.copy(errorMessage = message)
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

    /**
     * Sai da conta: (1) envia para a nuvem o que ainda só está no aparelho, enquanto a sessão vale (no máximo
     * [FLUSH_TIMEOUT_MS], para não travar sem internet); (2) encerra a sessão; (3) apaga os dados do usuário no aparelho,
     * para a próxima conta que entrar neste celular não herdá-los. Falha de rede não impede a saída.
     */
    fun signOut() {
        if (_uiState.value.isSigningOut || _uiState.value.isSignedOut) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSigningOut = true)
            localData?.let { data -> quietly { withTimeoutOrNull(FLUSH_TIMEOUT_MS) { data.flush() } } }
            quietly { authRepository.signOut() }
            quietly { localData?.clear() }
            _uiState.value = _uiState.value.copy(isSigningOut = false, isSignedOut = true)
        }
    }

    /**
     * Exclui a conta de vez: remove a foto do Storage (o banco não apaga arquivos), depois a conta e os dados no servidor
     * (migração 016) e por fim os dados do aparelho. Se o servidor recusar (sem internet, migração 016 não rodada),
     * nada além da foto é apagado, a pessoa continua logada e vê o motivo. A tela só chama isto depois de uma confirmação digitada.
     */
    fun deleteAccount() {
        val s = _uiState.value
        if (s.isDeletingAccount || s.isSigningOut || s.isSignedOut) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isDeletingAccount = true, errorMessage = null)
            try {
                if (_uiState.value.profile?.avatarUrl != null) quietly { profileRepository.removeAvatar() }
                authRepository.deleteAccount()
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                val missing = "delete_my_account" in e.message.orEmpty() || "could not find the function" in e.message.orEmpty().lowercase()
                _uiState.value = _uiState.value.copy(
                    isDeletingAccount = false,
                    errorMessage = if (missing) "A exclusão de conta ainda não está ativada no servidor (falta rodar a migração 016)."
                    else e.toUserMessage("Não foi possível excluir a conta. Seus dados continuam como estavam; confira a internet e tente de novo.")
                )
                return@launch
            }
            quietly { localData?.clear() }
            _uiState.value = _uiState.value.copy(isDeletingAccount = false, isSignedOut = true)
        }
    }

    private suspend fun quietly(block: suspend () -> Unit) {
        try { block() } catch (e: Exception) { if (e is CancellationException) throw e }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(
            errorMessage = null,
            successMessage = null
        )
    }

    private companion object {
        const val FLUSH_TIMEOUT_MS = 5_000L
    }
}
