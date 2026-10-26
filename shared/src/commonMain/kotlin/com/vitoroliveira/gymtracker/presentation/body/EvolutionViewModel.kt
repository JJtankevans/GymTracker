package com.vitoroliveira.gymtracker.presentation.body

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vitoroliveira.gymtracker.data.model.ExerciseProgression
import com.vitoroliveira.gymtracker.domain.repository.ExerciseRepository
import com.vitoroliveira.gymtracker.domain.repository.WorkoutRepository
import com.vitoroliveira.gymtracker.presentation.toUserMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days

/** Evolução de um dos 4 básicos: 1RM estimado por sessão. */
data class LiftSeries(
    val exerciseName: String,
    val lift: Lift,
    val dates: List<String>,
    val maxWeights: List<Double>,
    val oneRepMaxes: List<Double>
) {
    val bestOneRm: Double get() = oneRepMaxes.maxOrNull() ?: 0.0
}

/**
 * 1RM estimado por sessão. A view só guarda a carga máxima do dia e as reps totais, então usamos a média de
 * reps por série (limitada a 12, onde a fórmula de Epley ainda é razoável). É uma estimativa.
 */
fun buildLiftSeries(name: String, lift: Lift, progression: List<ExerciseProgression>): LiftSeries? {
    if (progression.isEmpty()) return null
    val e1 = progression.map {
        val avgReps = (it.totalReps.toDouble() / it.totalSets.coerceAtLeast(1)).toInt().coerceIn(1, 12)
        estimateOneRepMax(it.maxWeightKg, avgReps)
    }
    return LiftSeries(name, lift, progression.map { it.sessionDate }, progression.map { it.maxWeightKg }, e1)
}

data class EvolutionUiState(
    val isLoading: Boolean = false,
    val lifts: List<LiftSeries> = emptyList(),
    val errorMessage: String? = null,
    /** Séries por músculo nos últimos 7 dias (vazio se não carregou). */
    val weeklySets: List<MuscleSets> = emptyList()
)

class EvolutionViewModel(
    private val exerciseRepository: ExerciseRepository,
    private val workoutRepository: WorkoutRepository,
    private val clock: Clock = Clock.System
) : ViewModel() {
    private val _state = MutableStateFlow(EvolutionUiState())
    val state: StateFlow<EvolutionUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, errorMessage = null)
            try {
                val exercises = exerciseRepository.getAll()
                val lifts = exercises.mapNotNull { e ->
                    val id = e.id ?: return@mapNotNull null
                    val lift = StrengthStandards.liftFor(e.name) ?: return@mapNotNull null
                    buildLiftSeries(e.name, lift, workoutRepository.getProgressionForExercise(id))
                }
                // volume semanal é um extra: se falhar, o resto da tela continua
                val weekly = try {
                    val since = (clock.now() - 7.days).toString()
                    weeklySetsByMuscle(workoutRepository.getSetsSince(since), exercises.mapNotNull { e -> e.id?.let { it to e.name } }.toMap())
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    emptyList()
                }
                _state.value = EvolutionUiState(lifts = lifts, weeklySets = weekly)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _state.value = EvolutionUiState(errorMessage = e.toUserMessage("Erro ao carregar a evolução"))
            }
        }
    }
}
