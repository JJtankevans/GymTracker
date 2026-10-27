// shared/src/commonMain/kotlin/com/vitoroliveira/gymtracker/presentation/viewmodel/ExerciseViewModel.kt

package com.vitoroliveira.gymtracker.presentation.viewmodel

import com.vitoroliveira.gymtracker.presentation.toUserMessage
import kotlin.coroutines.cancellation.CancellationException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vitoroliveira.gymtracker.data.catalog.CatalogExercise
import com.vitoroliveira.gymtracker.data.model.Exercise
import com.vitoroliveira.gymtracker.domain.repository.ExerciseRepository
import com.vitoroliveira.gymtracker.domain.usecase.AddToGroupsResult
import com.vitoroliveira.gymtracker.domain.usecase.addToGroups
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ExerciseUiState(
    val isLoading: Boolean = false,
    val exercises: List<Exercise> = emptyList(),
    /** Nomes dos exercícios que já estão no grupo filtrado (ou de todos, sem filtro): o catálogo marca esses como "já adicionado". */
    val namesInScope: List<String> = emptyList(),
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
                val exercises = if (groupId != null) all.filter { groupId in it.groupIds } else all
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    exercises = exercises,
                    namesInScope = exercises.map { it.name }
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

    /** Se já existe um exercício com esse nome ele é reaproveitado e só ganha os [groupIds] que faltam (mesmo histórico). */
    fun createExercise(
        name: String,
        groupIds: List<String>,
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
                    defaultSets = defaultSets,
                    defaultReps = defaultReps,
                    notes = notes
                )
                val result = repository.addToGroups(newExercise, groupIds.toSet())
                // clearError = false: a recarga roda depois e apagaria o aviso "já está na sua lista" (ver armadilhas no CONTEXTO)
                loadExercises(clearError = false)
                _uiState.value = when (result) {
                    is AddToGroupsResult.Created -> _uiState.value.copy(successMessage = "Exercício '$name' criado com sucesso")
                    is AddToGroupsResult.Linked -> _uiState.value.copy(successMessage = "Exercício '$name' já existia e foi adicionado ao grupo")
                    is AddToGroupsResult.Unchanged -> _uiState.value.copy(errorMessage = "'$name' já está na sua lista")
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.toUserMessage("Erro ao criar exercício")
                )
            }
        }
    }

    /** Adiciona vários exercícios do catálogo de uma vez (séries/reps padrão de cada um); os que já existem só entram no grupo. */
    fun createMany(items: List<CatalogExercise>, muscleGroupId: String?) {
        if (items.isEmpty()) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val known = repository.getAll().toMutableList()
                val results = items.map { ex ->
                    repository.addToGroups(
                        Exercise(name = ex.name, defaultSets = ex.sets, defaultReps = ex.reps, notes = ex.hint),
                        setOfNotNull(muscleGroupId),
                        known
                    )
                }
                loadExercises(clearError = false)
                val changed = results.filter { it !is AddToGroupsResult.Unchanged }
                _uiState.value = when {
                    changed.isEmpty() -> _uiState.value.copy(errorMessage = "Esses exercícios já estão na sua lista")
                    changed.size > 1 -> _uiState.value.copy(successMessage = "${changed.size} exercícios adicionados")
                    changed[0] is AddToGroupsResult.Created ->
                        _uiState.value.copy(successMessage = "Exercício '${changed[0].exercise.name}' criado com sucesso")
                    else -> _uiState.value.copy(successMessage = "Exercício '${changed[0].exercise.name}' adicionado ao grupo")
                }
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

    /**
     * Tira o exercício só deste grupo: ele continua nos outros grupos (ou solto na lista "Todos") e o histórico fica.
     * Diferente de [deleteExercise], que apaga o exercício de todos os grupos junto com todas as séries.
     */
    fun removeFromGroup(exercise: Exercise, groupId: String) {
        if (groupId !in exercise.groupIds) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                // copy do exercício vindo do repositório: update grava os vínculos exatamente como em groupIds
                repository.update(exercise.copy(groupIds = exercise.groupIds - groupId))
                loadExercises()
                _uiState.value = _uiState.value.copy(
                    successMessage = "'${exercise.name}' saiu do grupo; o histórico continua"
                )
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.toUserMessage("Erro ao tirar o exercício do grupo")
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