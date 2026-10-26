// shared/src/commonMain/kotlin/com/vitoroliveira/gymtracker/presentation/viewmodel/HistoryViewModel.kt

package com.vitoroliveira.gymtracker.presentation.viewmodel

import com.vitoroliveira.gymtracker.presentation.toUserMessage
import kotlin.coroutines.cancellation.CancellationException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vitoroliveira.gymtracker.domain.repository.MuscleGroupRepository
import com.vitoroliveira.gymtracker.domain.resolveGroupName
import com.vitoroliveira.gymtracker.domain.repository.WorkoutRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.time.Instant

data class WorkoutHistoryItem(
    val workoutId: String,
    val muscleGroupName: String,
    val muscleGroupColorHex: String,
    val startedAt: Instant,
    val finishedAt: Instant?,
    val durationMinutes: Long?
)

data class HistoryUiState(
    val isLoading: Boolean = false,
    val workouts: List<WorkoutHistoryItem> = emptyList(),
    val errorMessage: String? = null
)

class HistoryViewModel(
    private val workoutRepository: WorkoutRepository,
    private val muscleGroupRepository: MuscleGroupRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        loadHistory()
    }

    fun loadHistory() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val workouts = workoutRepository.getHistory()
                val muscleGroups = muscleGroupRepository.getAll().associateBy { it.id }

                val items = workouts.mapNotNull { workout ->
                    val startedAtRaw = workout.startedAt ?: return@mapNotNull null
                    val workoutId = workout.id ?: return@mapNotNull null

                    val startedAt = Instant.parse(startedAtRaw)
                    val finishedAt = workout.finishedAt?.let { Instant.parse(it) }
                    val durationMinutes = finishedAt?.let { (it - startedAt).inWholeMinutes }

                    val group = workout.muscleGroupId?.let { muscleGroups[it] }

                    WorkoutHistoryItem(
                        workoutId = workoutId,
                        muscleGroupName = workout.resolveGroupName(muscleGroups) ?: "Sem grupo",
                        muscleGroupColorHex = group?.colorHex ?: "#9E9E9E",
                        startedAt = startedAt,
                        finishedAt = finishedAt,
                        durationMinutes = durationMinutes
                    )
                }

                _uiState.value = _uiState.value.copy(isLoading = false, workouts = items)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.toUserMessage("Erro ao carregar histórico")
                )
            }
        }
    }

    /** Apaga o treino (e as séries dele) e tira da lista; se falhar, a lista fica como estava. */
    fun deleteWorkout(workoutId: String) {
        viewModelScope.launch {
            try {
                workoutRepository.deleteWorkout(workoutId)
                _uiState.value = _uiState.value.copy(workouts = _uiState.value.workouts.filterNot { it.workoutId == workoutId })
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _uiState.value = _uiState.value.copy(errorMessage = e.toUserMessage("Erro ao apagar o treino"))
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}