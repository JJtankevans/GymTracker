package com.vitoroliveira.gymtracker.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vitoroliveira.gymtracker.domain.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

enum class SessionState { Loading, SignedIn, SignedOut }

/** Decide, na abertura do app, se vai pra Home (sessão salva) ou Login. */
class SessionViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow(SessionState.Loading)
    val state: StateFlow<SessionState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            _state.value = try {
                if (authRepository.awaitSession()) SessionState.SignedIn else SessionState.SignedOut
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                SessionState.SignedOut
            }
        }
    }
}
