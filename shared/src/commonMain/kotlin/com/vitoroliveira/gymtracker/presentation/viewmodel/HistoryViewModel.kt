// shared/src/commonMain/kotlin/com/vitoroliveira/gymtracker/presentation/viewmodel/HistoryViewModel.kt

package com.vitoroliveira.gymtracker.presentation.viewmodel

import com.vitoroliveira.gymtracker.presentation.toUserMessage
import kotlin.coroutines.cancellation.CancellationException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vitoroliveira.gymtracker.domain.repository.MuscleGroupRepository
import com.vitoroliveira.gymtracker.domain.resolveGroupName
import com.vitoroliveira.gymtracker.domain.repository.WorkoutRepository
import kotlinx.coroutines.NonCancellable
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

/** Treino que saiu da lista mas ainda não foi apagado de verdade: dá tempo de desfazer. */
data class PendingDelete(val item: WorkoutHistoryItem, val index: Int)

data class HistoryUiState(
    val isLoading: Boolean = false,
    val workouts: List<WorkoutHistoryItem> = emptyList(),
    val pendingUndo: PendingDelete? = null,
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

    /**
     * Tira o treino da lista na hora e deixa a exclusão **pendente**: a tela mostra "Desfazer" e só depois
     * ([commitPendingDelete]) o treino e as séries dele são apagados de verdade. Apagar outro antes
     * fecha o pendente primeiro.
     */
    fun deleteWorkout(workoutId: String) {
        val index = _uiState.value.workouts.indexOfFirst { it.workoutId == workoutId }
        if (index < 0) return
        commitPendingDelete()
        val state = _uiState.value
        val item = state.workouts[index]
        _uiState.value = state.copy(
            workouts = state.workouts.filterNot { it.workoutId == workoutId },
            pendingUndo = PendingDelete(item, index)
        )
    }

    /** Desfaz a exclusão pendente: o treino volta para o mesmo lugar da lista. */
    fun undoDelete() {
        val pending = _uiState.value.pendingUndo ?: return
        val list = _uiState.value.workouts.toMutableList()
        list.add(pending.index.coerceAtMost(list.size), pending.item)
        _uiState.value = _uiState.value.copy(workouts = list, pendingUndo = null)
    }

    /**
     * Apaga de verdade o treino pendente. Roda fora do escopo da tela (NonCancellable) para não se perder
     * se a pessoa sair do Histórico antes do aviso acabar; se falhar, o treino volta para a lista.
     */
    fun commitPendingDelete() {
        val pending = _uiState.value.pendingUndo ?: return
        _uiState.value = _uiState.value.copy(pendingUndo = null)
        viewModelScope.launch(NonCancellable) {
            try {
                workoutRepository.deleteWorkout(pending.item.workoutId)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                val list = _uiState.value.workouts.toMutableList()
                list.add(pending.index.coerceAtMost(list.size), pending.item)
                _uiState.value = _uiState.value.copy(
                    workouts = list,
                    errorMessage = e.toUserMessage("Erro ao apagar o treino")
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}