package com.vitoroliveira.gymtracker.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vitoroliveira.gymtracker.data.model.Exercise
import com.vitoroliveira.gymtracker.data.model.Workout
import com.vitoroliveira.gymtracker.data.model.WorkoutSet
import com.vitoroliveira.gymtracker.domain.repository.ExerciseRepository
import com.vitoroliveira.gymtracker.domain.repository.WorkoutRepository
import com.vitoroliveira.gymtracker.presentation.TimerManager
import com.vitoroliveira.gymtracker.presentation.TimerState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class WorkoutUiState(
    val isLoading: Boolean = false,
    val activeWorkout: Workout? = null,
    val exercises: List<Exercise> = emptyList(),
    val completedSets: List<WorkoutSet> = emptyList(),
    val timerState: TimerState = TimerState(),
    val errorMessage: String? = null,
    val isWorkoutFinished: Boolean = false,
    val activeWorkoutToRecover: Workout? = null
)

class WorkoutViewModel(
    private val workoutRepository: WorkoutRepository,
    private val exerciseRepository: ExerciseRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(WorkoutUiState())
    val uiState: StateFlow<WorkoutUiState> = _uiState.asStateFlow()

    private val timerManager = TimerManager(viewModelScope)

    init {
        // Observa o timer e atualiza o estado
        viewModelScope.launch {
            timerManager.timerState.collect { timerState ->
                _uiState.value = _uiState.value.copy(timerState = timerState)
            }
        }
    }

    fun startWorkout(muscleGroupId: String?) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                // Verifica se já existe um treino ativo antes de criar um novo
                val active = workoutRepository.getActiveWorkout()
                if (active != null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        activeWorkoutToRecover = active
                    )
                    return@launch
                }

                val workout = workoutRepository.startWorkout(muscleGroupId)
                val exercises = if (muscleGroupId != null) {
                    exerciseRepository.getByMuscleGroup(muscleGroupId)
                } else {
                    exerciseRepository.getAll()
                }
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    activeWorkout = workout,
                    exercises = exercises,
                    completedSets = emptyList()
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "Erro ao iniciar treino"
                )
            }
        }
    }

    fun resumeWorkout(workout: Workout) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, activeWorkoutToRecover = null)
            try {
                val exercises = if (workout.muscleGroupId != null) {
                    exerciseRepository.getByMuscleGroup(workout.muscleGroupId)
                } else {
                    exerciseRepository.getAll()
                }
                val sets = workoutRepository.getSetsForWorkout(workout.id!!)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    activeWorkout = workout,
                    exercises = exercises,
                    completedSets = sets
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "Erro ao retomar treino"
                )
            }
        }
    }

    fun discardAndStartNew(muscleGroupId: String?) {
        val oldWorkoutId = _uiState.value.activeWorkoutToRecover?.id
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, activeWorkoutToRecover = null)
            try {
                // Deleta o antigo para que não apareça no histórico
                oldWorkoutId?.let { workoutRepository.deleteWorkout(it) }

                // Inicia o novo
                val workout = workoutRepository.startWorkout(muscleGroupId)
                val exercises = if (muscleGroupId != null) {
                    exerciseRepository.getByMuscleGroup(muscleGroupId)
                } else {
                    exerciseRepository.getAll()
                }
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    activeWorkout = workout,
                    exercises = exercises,
                    completedSets = emptyList()
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "Erro ao iniciar novo treino"
                )
            }
        }
    }

    fun addSet(exerciseId: String, reps: Int, weightKg: Double) {
        val workoutId = _uiState.value.activeWorkout?.id ?: return

        // 1. Validação de peso
        if (weightKg <= 0.0 || weightKg.isNaN()) {
            _uiState.value = _uiState.value.copy(errorMessage = "O peso deve ser maior que zero")
            return
        }

        // 2. Localiza o exercício para verificar o limite
        val exercise = _uiState.value.exercises.find { it.id == exerciseId }
        if (exercise == null) {
            _uiState.value = _uiState.value.copy(errorMessage = "Exercício não encontrado")
            return
        }

        // Calcula o número da próxima série para esse exercício
        val currentSetsCount = _uiState.value.completedSets.count { it.exerciseId == exerciseId }
        val setNumber = currentSetsCount + 1

        // 3. Validação de limite de séries (default + 2)
        val maxSetsAllowed = exercise.defaultSets + 2
        if (setNumber > maxSetsAllowed) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Limite de séries atingido para este exercício"
            )
            return
        }

        val newSet = WorkoutSet(
            workoutId = workoutId,
            exerciseId = exerciseId,
            setNumber = setNumber,
            reps = reps,
            weightKg = weightKg
        )

        viewModelScope.launch {
            try {
                val saved = workoutRepository.addSet(newSet)
                _uiState.value = _uiState.value.copy(
                    completedSets = _uiState.value.completedSets + saved,
                    errorMessage = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = e.message ?: "Erro ao registrar série"
                )
            }
        }
    }

    fun finishWorkout() {
        val workoutId = _uiState.value.activeWorkout?.id ?: return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                workoutRepository.finishWorkout(workoutId)
                timerManager.cancel()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isWorkoutFinished = true
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "Erro ao finalizar treino"
                )
            }
        }
    }

    fun toggleExerciseFinished(exerciseId: String) {
        val workout = _uiState.value.activeWorkout ?: return
        val currentFinished = workout.finishedExerciseIds.toMutableList()
        
        if (currentFinished.contains(exerciseId)) {
            currentFinished.remove(exerciseId)
        } else {
            currentFinished.add(exerciseId)
        }

        val updatedWorkout = workout.copy(finishedExerciseIds = currentFinished)
        _uiState.value = _uiState.value.copy(activeWorkout = updatedWorkout)

        viewModelScope.launch {
            try {
                workoutRepository.updateFinishedExercises(workout.id!!, currentFinished)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = e.message ?: "Erro ao atualizar status do exercício"
                )
            }
        }
    }

    fun startTimer(seconds: Int) {
        timerManager.start(seconds)
    }

    fun pauseTimer() {
        timerManager.pause()
    }

    fun resumeTimer() {
        timerManager.resume()
    }

    fun cancelTimer() {
        timerManager.cancel()
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    override fun onCleared() {
        super.onCleared()
        timerManager.cancel()
    }
}