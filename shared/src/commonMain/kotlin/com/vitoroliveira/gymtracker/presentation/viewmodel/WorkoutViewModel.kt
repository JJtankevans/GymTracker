package com.vitoroliveira.gymtracker.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vitoroliveira.gymtracker.data.model.Exercise
import com.vitoroliveira.gymtracker.data.model.ExerciseBaseline
import com.vitoroliveira.gymtracker.data.model.Workout
import com.vitoroliveira.gymtracker.data.model.WorkoutSet
import com.vitoroliveira.gymtracker.domain.repository.ExerciseRepository
import com.vitoroliveira.gymtracker.domain.repository.WorkoutRepository
import com.vitoroliveira.gymtracker.presentation.TimerManager
import com.vitoroliveira.gymtracker.presentation.TimerState
import com.vitoroliveira.gymtracker.presentation.toUserMessage
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.minutes
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Clock
import kotlin.time.Instant

/** Resumo mostrado ao terminar o treino. */
data class WorkoutSummary(
    val durationMinutes: Long,
    val totalSets: Int,
    val totalReps: Int,
    val totalVolumeKg: Double,
    val exercisesWorked: Int,
    /** Nomes dos exercícios em que bateu recorde de carga. */
    val newRecords: List<String>
)

data class WorkoutUiState(
    val isLoading: Boolean = false,
    val activeWorkout: Workout? = null,
    val exercises: List<Exercise> = emptyList(),
    val completedSets: List<WorkoutSet> = emptyList(),
    val timerState: TimerState = TimerState(),
    val errorMessage: String? = null,
    val isWorkoutFinished: Boolean = false,
    val activeWorkoutToRecover: Workout? = null,
    /** Última vez / recorde por exercício (id → baseline). */
    val baselines: Map<String, ExerciseBaseline> = emptyMap(),
    /** Exercícios em que o usuário já bateu recorde neste treino. */
    val newRecordExerciseIds: Set<String> = emptySet(),
    val summary: WorkoutSummary? = null
)

class WorkoutViewModel(
    private val workoutRepository: WorkoutRepository,
    private val exerciseRepository: ExerciseRepository,
    private val clock: Clock = Clock.System
) : ViewModel() {

    private val _uiState = MutableStateFlow(WorkoutUiState())
    val uiState: StateFlow<WorkoutUiState> = _uiState.asStateFlow()

    /** Tempo decorrido do treino, em segundos. Separado do uiState pra não recompor a tela toda a cada segundo. */
    private val _elapsedSeconds = MutableStateFlow(0L)
    val elapsedSeconds: StateFlow<Long> = _elapsedSeconds.asStateFlow()

    private val timerManager = TimerManager(viewModelScope)
    private var tickerJob: Job? = null

    init {
        viewModelScope.launch {
            timerManager.timerState.collect { timerState ->
                _uiState.value = _uiState.value.copy(timerState = timerState)
            }
        }
    }

    // ───────────────────────── Início / retomada ─────────────────────────

    fun startWorkout(muscleGroupId: String?) {
        val s = _uiState.value
        // Idempotente: girar a tela ou recompor não deve iniciar (nem oferecer retomar) de novo.
        if (s.activeWorkout != null || s.isLoading || s.activeWorkoutToRecover != null) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val active = workoutRepository.getActiveWorkout()
                if (active != null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        activeWorkoutToRecover = active
                    )
                    return@launch
                }
                openWorkout(workoutRepository.startWorkout(muscleGroupId), muscleGroupId, emptyList())
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.toUserMessage("Erro ao iniciar treino")
                )
            }
        }
    }

    fun resumeWorkout(workout: Workout) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, activeWorkoutToRecover = null)
            try {
                val sets = workoutRepository.getSetsForWorkout(workout.id!!)
                openWorkout(workout, workout.muscleGroupId, sets)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.toUserMessage("Erro ao retomar treino")
                )
            }
        }
    }

    fun discardAndStartNew(muscleGroupId: String?) {
        val oldWorkoutId = _uiState.value.activeWorkoutToRecover?.id
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, activeWorkoutToRecover = null)
            try {
                oldWorkoutId?.let { workoutRepository.deleteWorkout(it) }
                openWorkout(workoutRepository.startWorkout(muscleGroupId), muscleGroupId, emptyList())
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.toUserMessage("Erro ao iniciar novo treino")
                )
            }
        }
    }

    private suspend fun openWorkout(workout: Workout, muscleGroupId: String?, sets: List<WorkoutSet>) {
        val exercises = if (muscleGroupId != null) {
            exerciseRepository.getByMuscleGroup(muscleGroupId)
        } else {
            exerciseRepository.getAll()
        }
        _uiState.value = _uiState.value.copy(
            isLoading = false,
            activeWorkout = workout,
            exercises = exercises,
            completedSets = sets,
            baselines = emptyMap(),
            newRecordExerciseIds = emptySet(),
            summary = null,
            isWorkoutFinished = false
        )
        startTicker(workout.startedAt)
        // Histórico é um "extra": carrega depois, sem travar a tela.
        val workoutId = workout.id ?: return
        viewModelScope.launch {
            val baselines = loadBaselines(workoutId, exercises)
            _uiState.value = _uiState.value.copy(baselines = baselines)
        }
    }

    private suspend fun loadBaselines(
        workoutId: String,
        exercises: List<Exercise>
    ): Map<String, ExerciseBaseline> = coroutineScope {
        exercises.mapNotNull { ex ->
            val id = ex.id ?: return@mapNotNull null
            async {
                try {
                    id to workoutRepository.getExerciseBaseline(id, workoutId)
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    null // sem histórico não é erro: só não pré-preenche
                }
            }
        }.awaitAll().filterNotNull().toMap()
    }

    private fun startTicker(startedAtRaw: String?) {
        tickerJob?.cancel()
        val startedAt = startedAtRaw?.let { runCatching { Instant.parse(it) }.getOrNull() } ?: run {
            _elapsedSeconds.value = 0
            return
        }
        tickerJob = viewModelScope.launch {
            while (true) {
                _elapsedSeconds.value = (clock.now() - startedAt).inWholeSeconds.coerceAtLeast(0)
                delay(1000)
            }
        }
    }

    // ───────────────────────── Séries ─────────────────────────

    fun addSet(exerciseId: String, reps: Int, weightKg: Double) {
        val workoutId = _uiState.value.activeWorkout?.id ?: return

        validateSet(reps, weightKg)?.let {
            _uiState.value = _uiState.value.copy(errorMessage = it)
            return
        }

        val exercise = _uiState.value.exercises.find { it.id == exerciseId }
        if (exercise == null) {
            _uiState.value = _uiState.value.copy(errorMessage = "Exercício não encontrado")
            return
        }

        val existing = _uiState.value.completedSets.filter { it.exerciseId == exerciseId }
        // maior número + 1 (e não "quantidade + 1"): continua certo depois de apagar uma série do meio
        val setNumber = (existing.maxOfOrNull { it.setNumber } ?: 0) + 1

        if (existing.size + 1 > exercise.defaultSets + 2) {
            _uiState.value = _uiState.value.copy(errorMessage = "Limite de séries atingido para este exercício")
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
                val state = _uiState.value
                val record = state.baselines[exerciseId]?.recordWeightKg
                // Só é "recorde" se já havia histórico (senão toda primeira vez seria recorde).
                val isRecord = record != null && record > 0.0 && weightKg > record
                _uiState.value = state.copy(
                    completedSets = state.completedSets + saved,
                    errorMessage = null,
                    newRecordExerciseIds = if (isRecord) state.newRecordExerciseIds + exerciseId else state.newRecordExerciseIds,
                    baselines = if (isRecord) state.baselines + (exerciseId to state.baselines.getValue(exerciseId).copy(recordWeightKg = weightKg)) else state.baselines
                )
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _uiState.value = _uiState.value.copy(errorMessage = e.toUserMessage("Erro ao registrar série"))
            }
        }
    }

    fun updateSet(setId: String, reps: Int, weightKg: Double) {
        validateSet(reps, weightKg)?.let {
            _uiState.value = _uiState.value.copy(errorMessage = it)
            return
        }
        viewModelScope.launch {
            try {
                val saved = workoutRepository.updateSet(setId, reps, weightKg)
                _uiState.value = _uiState.value.copy(
                    completedSets = _uiState.value.completedSets.map { if (it.id == setId) saved else it },
                    errorMessage = null
                )
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _uiState.value = _uiState.value.copy(errorMessage = e.toUserMessage("Erro ao editar série"))
            }
        }
    }

    fun deleteSet(setId: String) {
        viewModelScope.launch {
            try {
                workoutRepository.deleteSet(setId)
                _uiState.value = _uiState.value.copy(
                    completedSets = _uiState.value.completedSets.filterNot { it.id == setId },
                    errorMessage = null
                )
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _uiState.value = _uiState.value.copy(errorMessage = e.toUserMessage("Erro ao apagar série"))
            }
        }
    }

    /** Peso 0 é válido (exercício com o peso do corpo, ex.: barra fixa). */
    private fun validateSet(reps: Int, weightKg: Double): String? = when {
        reps <= 0 -> "Informe pelo menos 1 repetição"
        reps > 999 -> "Repetições demais. Confira o valor"
        weightKg.isNaN() || weightKg < 0.0 -> "O peso não pode ser negativo"
        weightKg > 2000.0 -> "Peso muito alto. Confira o valor"
        else -> null
    }

    // ───────────────────────── Finalização ─────────────────────────

    /** Texto para compartilhar o treino que acabou de terminar (usa o resumo final para a duração). */
    fun shareText(groupName: String?): String {
        val s = _uiState.value
        val names = s.exercises.associate { it.id to it.name }
        val groups = s.completedSets.map { it.exerciseId }.distinct().map { id ->
            ExerciseSetGroup(id, names[id] ?: "Exercício", s.completedSets.filter { it.exerciseId == id }.sortedBy { it.setNumber })
        }
        val started = s.activeWorkout?.startedAt?.let { runCatching { kotlin.time.Instant.parse(it) }.getOrNull() }
        val finished = started?.let { st -> s.summary?.let { st + it.durationMinutes.minutes } }
        val workout = s.activeWorkout?.copy(finishedAt = finished?.toString())
        return com.vitoroliveira.gymtracker.presentation.workout.shareText(
            groupName, workout, groups, newRecords = s.summary?.newRecords.orEmpty()
        )
    }

    fun finishWorkout() {
        val state = _uiState.value
        val workoutId = state.activeWorkout?.id ?: return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                workoutRepository.finishWorkout(workoutId)
                timerManager.cancel()
                tickerJob?.cancel()
                val finalState = _uiState.value
                _uiState.value = finalState.copy(
                    isLoading = false,
                    isWorkoutFinished = true,
                    summary = buildSummary(finalState)
                )
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.toUserMessage("Erro ao finalizar treino")
                )
            }
        }
    }

    /** Apaga o treino aberto (ex.: circuito encerrado sem nenhuma série). Chama [onDone] quando terminar. */
    fun discardActiveWorkout(onDone: () -> Unit) {
        val workoutId = _uiState.value.activeWorkout?.id ?: run { onDone(); return }
        viewModelScope.launch {
            try {
                workoutRepository.deleteWorkout(workoutId)
                timerManager.cancel()
                tickerJob?.cancel()
                _uiState.value = _uiState.value.copy(activeWorkout = null)
                onDone()
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _uiState.value = _uiState.value.copy(errorMessage = e.toUserMessage("Erro ao descartar treino"))
            }
        }
    }

    private fun buildSummary(state: WorkoutUiState): WorkoutSummary {
        val sets = state.completedSets
        val names = state.exercises.associate { it.id to it.name }
        return WorkoutSummary(
            durationMinutes = _elapsedSeconds.value / 60,
            totalSets = sets.size,
            totalReps = sets.sumOf { it.reps },
            totalVolumeKg = sets.sumOf { it.reps * it.weightKg },
            exercisesWorked = sets.map { it.exerciseId }.distinct().size,
            newRecords = state.newRecordExerciseIds.mapNotNull { names[it] }
        )
    }

    fun toggleExerciseFinished(exerciseId: String) {
        val workout = _uiState.value.activeWorkout ?: return
        val currentFinished = workout.finishedExerciseIds.toMutableList()

        if (currentFinished.contains(exerciseId)) {
            currentFinished.remove(exerciseId)
        } else {
            currentFinished.add(exerciseId)
        }

        _uiState.value = _uiState.value.copy(activeWorkout = workout.copy(finishedExerciseIds = currentFinished))

        viewModelScope.launch {
            try {
                workoutRepository.updateFinishedExercises(workout.id!!, currentFinished)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _uiState.value = _uiState.value.copy(
                    errorMessage = e.toUserMessage("Erro ao atualizar status do exercício")
                )
            }
        }
    }

    // ───────────────────────── Timer ─────────────────────────

    fun startTimer(seconds: Int) = timerManager.start(seconds)
    fun pauseTimer() = timerManager.pause()
    fun resumeTimer() = timerManager.resume()
    fun cancelTimer() = timerManager.cancel()

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    override fun onCleared() {
        super.onCleared()
        timerManager.cancel()
        tickerJob?.cancel()
    }
}
