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
    val isWorkoutFinished: Boolean = false
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

    fun addSet(exerciseId: String, reps: Int, weightKg: Double) {
        val workoutId = _uiState.value.activeWorkout?.id ?: return

        // Calcula o número da próxima série para esse exercício
        val setNumber = _uiState.value.completedSets
            .count { it.exerciseId == exerciseId } + 1

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
                    completedSets = _uiState.value.completedSets + saved
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