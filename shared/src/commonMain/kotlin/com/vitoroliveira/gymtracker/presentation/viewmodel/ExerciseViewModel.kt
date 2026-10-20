// shared/src/commonMain/kotlin/com/vitoroliveira/gymtracker/presentation/viewmodel/ExerciseViewModel.kt

package com.vitoroliveira.gymtracker.presentation.viewmodel

import com.vitoroliveira.gymtracker.presentation.toUserMessage
import kotlin.coroutines.cancellation.CancellationException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vitoroliveira.gymtracker.data.catalog.CatalogExercise
import com.vitoroliveira.gymtracker.data.model.Exercise
import com.vitoroliveira.gymtracker.domain.repository.ExerciseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ExerciseUiState(
    val isLoading: Boolean = false,
    val exercises: List<Exercise> = emptyList(),
    /** Nomes de todos os exercícios do usuário, mesmo com filtro de grupo (o nome é único por usuário). */
    val allNames: List<String> = emptyList(),
    val selectedMuscleGroupId: String? = null,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

class ExerciseViewModel(
    private val repository: ExerciseRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExerciseUiState())
    val uiState: StateFlow<ExerciseUiState> = _uiState.asStateFlow()

    init {
        loadExercises()
    }

    /**
     * Carrega todos os exercícios, ou apenas os do grupo muscular
     * selecionado no filtro atual (se houver).
     */
    fun loadExercises(clearError: Boolean = true) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = if (clearError) null else _uiState.value.errorMessage
            )
            try {
                val groupId = _uiState.value.selectedMuscleGroupId
                val all = repository.getAll()
                val exercises = if (groupId != null) all.filter { it.muscleGroupId == groupId } else all
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    exercises = exercises,
                    allNames = all.map { it.name }
                )
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.toUserMessage("Erro ao carregar exercícios")
                )
            }
        }
    }

    /**
     * Aplica o filtro por grupo muscular e recarrega a lista.
     * Passe null para remover o filtro (lista todos os exercícios).
     */
    fun filterByMuscleGroup(muscleGroupId: String?) {
        _uiState.value = _uiState.value.copy(selectedMuscleGroupId = muscleGroupId)
        loadExercises()
    }

    fun createExercise(
        name: String,
        muscleGroupId: String?,
        defaultSets: Int = 3,
        defaultReps: Int = 12,
        notes: String? = null
    ) {
        if (name.isBlank()) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "O nome não pode ser vazio"
            )
            return
        }
        if (defaultSets <= 0 || defaultReps <= 0) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Séries e repetições devem ser maiores que zero"
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val newExercise = Exercise(
                    name = name,
                    muscleGroupId = muscleGroupId,
                    defaultSets = defaultSets,
                    defaultReps = defaultReps,
                    notes = notes
                )
                repository.insert(newExercise)
                loadExercises()
                _uiState.value = _uiState.value.copy(
                    successMessage = "Exercício '$name' criado com sucesso"
                )
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.toUserMessage("Erro ao criar exercício")
                )
            }
        }
    }

    /** Adiciona vários exercícios do catálogo de uma vez (séries/reps padrão de cada um). */
    fun createMany(items: List<CatalogExercise>, muscleGroupId: String?) {
        if (items.isEmpty()) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                items.forEach { ex ->
                    repository.insert(
                        Exercise(
                            name = ex.name, muscleGroupId = muscleGroupId,
                            defaultSets = ex.sets, defaultReps = ex.reps, notes = ex.hint
                        )
                    )
                }
                loadExercises()
                _uiState.value = _uiState.value.copy(
                    successMessage = if (items.size == 1) "Exercício '${items[0].name}' criado com sucesso"
                    else "${items.size} exercícios adicionados"
                )
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                // alguns podem ter entrado antes do erro: recarrega sem apagar a mensagem
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.toUserMessage("Erro ao adicionar os exercícios")
                )
                loadExercises(clearError = false)
            }
        }
    }

    fun updateExercise(exercise: Exercise) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                repository.update(exercise)
                loadExercises()
                _uiState.value = _uiState.value.copy(
                    successMessage = "Exercício atualizado com sucesso"
                )
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.toUserMessage("Erro ao atualizar exercício")
                )
            }
        }
    }

    fun deleteExercise(id: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                repository.delete(id)
                loadExercises()
                _uiState.value = _uiState.value.copy(
                    successMessage = "Exercício removido com sucesso"
                )
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.toUserMessage("Erro ao remover exercício")
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