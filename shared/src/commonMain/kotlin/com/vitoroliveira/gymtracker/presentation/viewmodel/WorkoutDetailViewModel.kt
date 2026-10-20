// shared/src/commonMain/kotlin/com/vitoroliveira/gymtracker/presentation/viewmodel/WorkoutDetailViewModel.kt

package com.vitoroliveira.gymtracker.presentation.viewmodel

import com.vitoroliveira.gymtracker.presentation.toUserMessage
import kotlin.coroutines.cancellation.CancellationException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vitoroliveira.gymtracker.data.model.WorkoutSet
import com.vitoroliveira.gymtracker.domain.repository.ExerciseRepository
import com.vitoroliveira.gymtracker.domain.repository.WorkoutRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ExerciseSetGroup(
    val exerciseId: String,
    val exerciseName: String,
    val sets: List<WorkoutSet>
)

data class WorkoutDetailUiState(
    val isLoading: Boolean = false,
    val exerciseGroups: List<ExerciseSetGroup> = emptyList(),
    val errorMessage: String? = null
)

class WorkoutDetailViewModel(
    private val workoutId: String,
    private val workoutRepository: WorkoutRepository,
    private val exerciseRepository: ExerciseRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(WorkoutDetailUiState())
    val uiState: StateFlow<WorkoutDetailUiState> = _uiState.asStateFlow()

    init {
        loadDetail()
    }

    fun loadDetail() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val sets = workoutRepository.getSetsForWorkout(workoutId)
                val exercises = exerciseRepository.getAll().associateBy { it.id }

                // Preserva a ordem de primeira aparição do exercício na sessão
                val orderedExerciseIds = sets.map { it.exerciseId }.distinct()

                val groups = orderedExerciseIds.map { exerciseId ->
                    ExerciseSetGroup(
                        exerciseId = exerciseId,
                        exerciseName = exercises[exerciseId]?.name ?: "Exercício removido",
                        sets = sets
                            .filter { it.exerciseId == exerciseId }
                            .sortedBy { it.setNumber }
                    )
                }

                _uiState.value = _uiState.value.copy(isLoading = false, exerciseGroups = groups)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.toUserMessage("Erro ao carregar detalhes do treino")
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}