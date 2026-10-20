package com.vitoroliveira.gymtracker.presentation.viewmodel

import com.vitoroliveira.gymtracker.presentation.toUserMessage
import kotlin.coroutines.cancellation.CancellationException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vitoroliveira.gymtracker.data.model.MuscleGroup
import com.vitoroliveira.gymtracker.domain.repository.MuscleGroupRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class MuscleGroupUiState(
    val isLoading: Boolean = false,
    val muscleGroups: List<MuscleGroup> = emptyList(),
    val errorMessage: String? = null,
    val successMessage: String? = null
)

class MuscleGroupViewModel(
    private val repository: MuscleGroupRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MuscleGroupUiState())
    val uiState: StateFlow<MuscleGroupUiState> = _uiState.asStateFlow()

    init {
        loadMuscleGroups()
    }

    fun loadMuscleGroups() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val groups = repository.getAll()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    muscleGroups = groups
                )
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.toUserMessage("Erro ao carregar grupos musculares")
                )
            }
        }
    }

    fun createMuscleGroup(name: String, colorHex: String) {
        if (name.isBlank()) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "O nome não pode ser vazio"
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val newGroup = MuscleGroup(name = name, colorHex = colorHex)
                repository.insert(newGroup)
                loadMuscleGroups()
                _uiState.value = _uiState.value.copy(
                    successMessage = "Grupo '$name' criado com sucesso"
                )
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.toUserMessage("Erro ao criar grupo muscular")
                )
            }
        }
    }

    fun updateMuscleGroup(muscleGroup: MuscleGroup) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                repository.update(muscleGroup)
                loadMuscleGroups()
                _uiState.value = _uiState.value.copy(
                    successMessage = "Grupo atualizado com sucesso"
                )
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.toUserMessage("Erro ao atualizar grupo muscular")
                )
            }
        }
    }

    fun deleteMuscleGroup(id: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                repository.delete(id)
                loadMuscleGroups()
                _uiState.value = _uiState.value.copy(
                    successMessage = "Grupo removido com sucesso"
                )
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.toUserMessage("Erro ao remover grupo muscular")
                )
            }
        }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(
            errorMessage = null,
            successMessage = null
        )
    }
}