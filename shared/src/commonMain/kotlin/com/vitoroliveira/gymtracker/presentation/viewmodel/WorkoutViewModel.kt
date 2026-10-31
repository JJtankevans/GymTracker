package com.vitoroliveira.gymtracker.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vitoroliveira.gymtracker.data.model.Exercise
import com.vitoroliveira.gymtracker.data.model.ExerciseBaseline
import com.vitoroliveira.gymtracker.data.model.Workout
import com.vitoroliveira.gymtracker.data.model.WorkoutSet
import com.vitoroliveira.gymtracker.domain.repository.ExerciseRepository
import com.vitoroliveira.gymtracker.domain.usecase.addToGroups
import com.vitoroliveira.gymtracker.domain.repository.WorkoutRepository
import com.vitoroliveira.gymtracker.presentation.TimerManager
import com.vitoroliveira.gymtracker.presentation.TimerState
import com.vitoroliveira.gymtracker.presentation.toUserMessage
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Duration.Companion.minutes
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Clock
import kotlin.time.Instant

/** Séries a mais que dá para registrar além das séries do plano do exercício. */
const val EXTRA_SETS_ALLOWED = 2

/** Máximo de séries do exercício num treino: as do plano + [EXTRA_SETS_ALLOWED]. */
fun maxSetsFor(exercise: Exercise): Int = exercise.defaultSets + EXTRA_SETS_ALLOWED

/** Aviso mostrado quando o exercício chega ao máximo de séries (no treino ativo e no circuito). */
fun setLimitMessage(exercise: Exercise): String =
    "Você chegou ao limite de ${maxSetsFor(exercise)} séries em ${exercise.name} " +
        "(${exercise.defaultSets} do plano + $EXTRA_SETS_ALLOWED extras)."

/** Resumo mostrado ao terminar o treino. */
data class FinishedWorkoutSummary(
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
    /** Exercícios em que o usuário já bateu recorde neste treino (recalculado a cada série registrada, editada ou apagada). */
    val newRecordExerciseIds: Set<String> = emptySet(),
    val summary: FinishedWorkoutSummary? = null,
    /** Exercícios com série sendo gravada agora: o botão "Registrar série" fica desligado (evita série duplicada no toque duplo). */
    val savingExerciseIds: Set<String> = emptySet(),
    /** Sobe 1 cada vez que uma série registrada ou editada AGORA bate um recorde novo: dispara o confete (retomar o treino não dispara). */
    val recordBursts: Int = 0
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
    /** Séries ainda sendo gravadas: o fim do treino espera por elas (senão a última série do circuito ficava de fora). */
    private val pendingSaves = mutableListOf<Job>()
    /** Gravações em andamento por exercício (o circuito pode enfileirar mais de uma do mesmo exercício). */
    private val savingCount = mutableMapOf<String, Int>()
    /** Uma série gravada por vez: o número da série é calculado já com as anteriores salvas. */
    private val saveMutex = Mutex()
    /** Recorde de carga de cada exercício ANTES deste treino (do histórico): base para recalcular os recordes do treino. */
    private var recordsBefore: Map<String, Double?> = emptyMap()

    init {
        viewModelScope.launch {
            timerManager.timerState.collect { timerState ->
                _uiState.value = _uiState.value.copy(timerState = timerState)
            }
        }
    }

    // ───────────────────────── Início / retomada ─────────────────────────

    /**
     * [resumeIfActive]: se já existe um treino em andamento, entra nele direto em vez de oferecer "continuar ou
     * descartar" (vindo do aviso da Home, a pessoa já escolheu continuar).
     */
    fun startWorkout(muscleGroupId: String?, resumeIfActive: Boolean = false) {
        val s = _uiState.value
        // Idempotente: girar a tela ou recompor não deve iniciar (nem oferecer retomar) de novo.
        if (s.activeWorkout != null || s.isLoading || s.activeWorkoutToRecover != null) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val active = workoutRepository.getActiveWorkout()
                if (active != null) {
                    val activeId = active.id
                    if (resumeIfActive && activeId != null) {
                        openWorkout(active, active.muscleGroupId, workoutRepository.getSetsForWorkout(activeId))
                        return@launch
                    }
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
        val inGroup = if (muscleGroupId != null) {
            exerciseRepository.getByMuscleGroup(muscleGroupId)
        } else {
            exerciseRepository.getAll()
        }
        // treino retomado: exercícios "só desta sessão" (fora do grupo) voltam porque já têm séries registradas
        val groupIds = inGroup.mapNotNull { it.id }.toSet()
        val strayIds = sets.map { it.exerciseId }.filter { it !in groupIds }.distinct()
        val exercises = if (muscleGroupId == null || strayIds.isEmpty()) inGroup
        else inGroup + exerciseRepository.getAll().filter { it.id in strayIds }
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
        recordsBefore = emptyMap()
        startTicker(workout.startedAt)
        // Histórico é um "extra": carrega depois, sem travar a tela.
        val workoutId = workout.id ?: return
        viewModelScope.launch {
            val baselines = loadBaselines(workoutId, exercises)
            recordsBefore = baselines.mapValues { it.value.recordWeightKg }
            // treino retomado: séries de antes que já eram recorde aparecem como recorde (sem confete)
            _uiState.value = withRecords(_uiState.value.copy(baselines = baselines))
        }
    }

    /**
     * Exercícios só desta sessão ("decidi fazer um pump agora"): entram na lista do treino e aceitam séries, mas NÃO
     * entram no grupo. Se o nome já existe nos exercícios da pessoa (ignorando caixa e acento) ele é reaproveitado, com
     * o mesmo histórico e recorde; se não, é criado sem grupo. Ao retomar o treino eles reaparecem pelas séries.
     */
    fun addTemporaryExercises(drafts: List<Exercise>) {
        val workoutId = _uiState.value.activeWorkout?.id ?: return
        val clean = drafts.filter { it.name.isNotBlank() }.map { it.copy(id = null, name = it.name.trim(), groupIds = emptyList()) }
        if (clean.isEmpty()) return
        viewModelScope.launch {
            try {
                val known = exerciseRepository.getAll().toMutableList()
                val added = mutableListOf<Exercise>()
                for (draft in clean) {
                    val exercise = exerciseRepository.addToGroups(draft, emptySet(), known).exercise
                    val already = _uiState.value.exercises + added
                    if (already.none { it.id == exercise.id }) added += exercise
                }
                if (added.isEmpty()) return@launch
                _uiState.value = _uiState.value.copy(exercises = _uiState.value.exercises + added)
                val baselines = loadBaselines(workoutId, added)
                recordsBefore = recordsBefore + baselines.mapValues { it.value.recordWeightKg }
                _uiState.value = withRecords(_uiState.value.copy(baselines = _uiState.value.baselines + baselines))
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _uiState.value = _uiState.value.copy(errorMessage = e.toUserMessage("Erro ao adicionar o exercício"))
            }
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

    /**
     * Registra uma série. Enquanto a série do mesmo exercício está sendo gravada, um novo toque é ignorado (toque duplo
     * gravava a série duas vezes); o circuito passa [queueIfSaving] = true porque as séries dele chegam em sequência e
     * nenhuma pode se perder.
     */
    fun addSet(exerciseId: String, reps: Int, weightKg: Double, queueIfSaving: Boolean = false) {
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

        if (!queueIfSaving && exerciseId in _uiState.value.savingExerciseIds) return

        // conta também as que ainda estão sendo gravadas
        val done = _uiState.value.completedSets.count { it.exerciseId == exerciseId } + (savingCount[exerciseId] ?: 0)
        if (done >= maxSetsFor(exercise)) {
            _uiState.value = _uiState.value.copy(errorMessage = setLimitMessage(exercise))
            return
        }

        markSaving(exerciseId, +1)
        pendingSaves.removeAll { it.isCompleted }
        pendingSaves += viewModelScope.launch {
            try {
                saveMutex.withLock {
                    val existing = _uiState.value.completedSets.filter { it.exerciseId == exerciseId }
                    // maior número + 1 (e não "quantidade + 1"): continua certo depois de apagar uma série do meio
                    val setNumber = (existing.maxOfOrNull { it.setNumber } ?: 0) + 1
                    val saved = workoutRepository.addSet(
                        WorkoutSet(workoutId = workoutId, exerciseId = exerciseId, setNumber = setNumber, reps = reps, weightKg = weightKg)
                    )
                    val before = _uiState.value
                    publishWithRecords(before, before.copy(completedSets = before.completedSets + saved, errorMessage = null))
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _uiState.value = _uiState.value.copy(errorMessage = e.toUserMessage("Erro ao registrar série"))
            } finally {
                markSaving(exerciseId, -1)
            }
        }
    }

    private fun markSaving(exerciseId: String, delta: Int) {
        val n = (savingCount[exerciseId] ?: 0) + delta
        if (n > 0) savingCount[exerciseId] = n else savingCount.remove(exerciseId)
        _uiState.value = _uiState.value.copy(savingExerciseIds = savingCount.keys.toSet())
    }

    fun updateSet(setId: String, reps: Int, weightKg: Double) {
        validateSet(reps, weightKg)?.let {
            _uiState.value = _uiState.value.copy(errorMessage = it)
            return
        }
        viewModelScope.launch {
            try {
                val saved = workoutRepository.updateSet(setId, reps, weightKg)
                val before = _uiState.value
                // editar a série que bateu o recorde (ex.: 200 kg digitado por engano) desfaz o recorde; editar para cima detecta
                publishWithRecords(before, before.copy(
                    completedSets = before.completedSets.map { if (it.id == setId) saved else it },
                    errorMessage = null
                ))
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
                val state = _uiState.value
                _uiState.value = withRecords(state.copy(
                    completedSets = state.completedSets.filterNot { it.id == setId },
                    errorMessage = null
                ))
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _uiState.value = _uiState.value.copy(errorMessage = e.toUserMessage("Erro ao apagar série"))
            }
        }
    }

    /** Publica [next] com os recordes recalculados; se apareceu recorde em exercício novo, conta mais um confete. */
    private fun publishWithRecords(before: WorkoutUiState, next: WorkoutUiState) {
        val after = withRecords(next)
        val gained = (after.newRecordExerciseIds - before.newRecordExerciseIds).isNotEmpty()
        _uiState.value = if (gained) after.copy(recordBursts = before.recordBursts + 1) else after
    }

    /**
     * Recordes do treino a partir das séries atuais: é recorde quando a maior carga do exercício no treino passa do recorde
     * de antes do treino (só se já havia histórico; senão toda primeira vez seria recorde). O "Recorde: X kg" mostrado
     * (baselines) acompanha: a carga do treino se for recorde, senão o recorde de antes.
     */
    private fun withRecords(s: WorkoutUiState): WorkoutUiState {
        val best = s.completedSets.groupBy { it.exerciseId }.mapValues { (_, sets) -> sets.maxOf { it.weightKg } }
        val ids = best.filter { (id, kg) -> recordsBefore[id]?.let { it > 0.0 && kg > it } == true }.keys
        val baselines = s.baselines.mapValues { (id, b) ->
            b.copy(recordWeightKg = if (id in ids) best.getValue(id) else recordsBefore[id] ?: b.recordWeightKg)
        }
        return s.copy(newRecordExerciseIds = ids, baselines = baselines)
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
                // séries que ainda estão indo para o banco entram no resumo e no post do feed (o gatilho calcula na hora do fim)
                pendingSaves.toList().joinAll()
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

    private fun buildSummary(state: WorkoutUiState): FinishedWorkoutSummary {
        val sets = state.completedSets
        val names = state.exercises.associate { it.id to it.name }
        return FinishedWorkoutSummary(
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
    /** Recalcula o descanso na hora (a tela chama ao voltar do bloqueio e enquanto o descanso corre). */
    fun refreshTimer() = timerManager.refresh()

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    override fun onCleared() {
        super.onCleared()
        timerManager.cancel()
        tickerJob?.cancel()
    }
}
