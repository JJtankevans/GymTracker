// shared/src/commonMain/kotlin/com/vitoroliveira/gymtracker/presentation/viewmodel/ExerciseDetailViewModel.kt

package com.vitoroliveira.gymtracker.presentation.viewmodel

import com.vitoroliveira.gymtracker.presentation.toUserMessage
import kotlin.coroutines.cancellation.CancellationException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vitoroliveira.gymtracker.data.model.ExerciseProgression
import com.vitoroliveira.gymtracker.domain.repository.WorkoutRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ExerciseDetailUiState(
    val isLoading: Boolean = false,
    val progression: List<ExerciseProgression> = emptyList(),
    val maxWeightKg: Double? = null,
    val lastWeightKg: Double? = null,
    val totalSessions: Int = 0,
    val errorMessage: String? = null
)

class ExerciseDetailViewModel(
    private val exerciseId: String,
    private val workoutRepository: WorkoutRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExerciseDetailUiState())
    val uiState: StateFlow<ExerciseDetailUiState> = _uiState.asStateFlow()

    init {
        loadProgression()
    }

    fun loadProgression() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                // já vem ordenado por session_date ASC (ver getProgressionForExercise)
                val progression = workoutRepository.getProgressionForExercise(exerciseId)

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    progression = progression,
                    maxWeightKg = progression.maxOfOrNull { it.maxWeightKg },
                    lastWeightKg = progression.lastOrNull()?.maxWeightKg,
                    totalSessions = progression.size
                )
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.toUserMessage("Erro ao carregar progressão do exercício")
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}