// shared/src/commonMain/kotlin/com/vitoroliveira/gymtracker/presentation/viewmodel/WorkoutDetailViewModel.kt

package com.vitoroliveira.gymtracker.presentation.viewmodel

import com.vitoroliveira.gymtracker.presentation.toUserMessage
import kotlin.coroutines.cancellation.CancellationException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vitoroliveira.gymtracker.data.model.Workout
import com.vitoroliveira.gymtracker.data.model.WorkoutSet
import com.vitoroliveira.gymtracker.presentation.workout.WorkoutSummary
import com.vitoroliveira.gymtracker.presentation.workout.summarize
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
    val errorMessage: String? = null,
    /** O treino em si (data, duração, anotação); null enquanto carrega ou se falhar (as séries ainda aparecem). */
    val workout: Workout? = null,
    val summary: WorkoutSummary? = null,
    val successMessage: String? = null,
    val isSavingNotes: Boolean = false
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

                // o treino em si é um extra: se falhar, as séries continuam aparecendo
                val workout = try { workoutRepository.getWorkout(workoutId) } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    null
                }
                _uiState.value = _uiState.value.copy(
                    isLoading = false, exerciseGroups = groups, workout = workout, summary = summarize(workout, groups)
                )
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.toUserMessage("Erro ao carregar detalhes do treino")
                )
            }
        }
    }

    /** Salva a anotação do treino; texto vazio apaga. */
    fun saveNotes(text: String) {
        val clean = text.trim().takeIf { it.isNotEmpty() }
        if (clean != null && clean.length > 1000) {
            _uiState.value = _uiState.value.copy(errorMessage = "A anotação pode ter até 1000 caracteres")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSavingNotes = true, errorMessage = null)
            try {
                workoutRepository.updateNotes(workoutId, clean)
                val w = (_uiState.value.workout ?: Workout(id = workoutId)).copy(notes = clean)
                _uiState.value = _uiState.value.copy(
                    isSavingNotes = false, workout = w,
                    successMessage = if (clean == null) "Anotação apagada" else "Anotação salva"
                )
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _uiState.value = _uiState.value.copy(isSavingNotes = false, errorMessage = e.toUserMessage("Erro ao salvar a anotação"))
            }
        }
    }

    /** Texto pronto para compartilhar (WhatsApp, Instagram...). */
    fun shareText(groupName: String?): String =
        com.vitoroliveira.gymtracker.presentation.workout.shareText(groupName, _uiState.value.workout, _uiState.value.exerciseGroups)

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null, successMessage = null)
    }
}