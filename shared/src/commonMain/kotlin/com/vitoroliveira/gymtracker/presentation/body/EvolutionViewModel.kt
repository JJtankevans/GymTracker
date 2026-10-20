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
    val errorMessage: String? = null
)

class EvolutionViewModel(
    private val exerciseRepository: ExerciseRepository,
    private val workoutRepository: WorkoutRepository
) : ViewModel() {
    private val _state = MutableStateFlow(EvolutionUiState())
    val state: StateFlow<EvolutionUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, errorMessage = null)
            try {
                val lifts = exerciseRepository.getAll().mapNotNull { e ->
                    val id = e.id ?: return@mapNotNull null
                    val lift = StrengthStandards.liftFor(e.name) ?: return@mapNotNull null
                    buildLiftSeries(e.name, lift, workoutRepository.getProgressionForExercise(id))
                }
                _state.value = EvolutionUiState(lifts = lifts)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _state.value = EvolutionUiState(errorMessage = e.toUserMessage("Erro ao carregar a evolução"))
            }
        }
    }
}
