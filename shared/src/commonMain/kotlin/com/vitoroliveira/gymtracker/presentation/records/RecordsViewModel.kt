package com.vitoroliveira.gymtracker.presentation.records

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vitoroliveira.gymtracker.domain.repository.ExerciseRepository
import com.vitoroliveira.gymtracker.domain.repository.WorkoutRepository
import com.vitoroliveira.gymtracker.presentation.toUserMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.time.Clock

data class RecordsUiState(
    val isLoading: Boolean = false,
    val records: List<RecordRow> = emptyList(),
    val today: LocalDate? = null,
    val errorMessage: String? = null
) {
    /** Recordes batidos nos últimos 30 dias. */
    val recentCount: Int get() = today?.let { t -> records.count { it.isRecent(t) } } ?: 0
}

class RecordsViewModel(
    private val exerciseRepository: ExerciseRepository,
    private val workoutRepository: WorkoutRepository,
    private val clock: Clock = Clock.System
) : ViewModel() {

    private val _state = MutableStateFlow(RecordsUiState())
    val state: StateFlow<RecordsUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, errorMessage = null)
            try {
                val rows = try {
                    // caminho rápido: 1 chamada, o servidor calcula tudo (migração 008)
                    workoutRepository.getRecordsSummary().map { it.toRecordRow() }
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    // banco sem a função `records_summary()` (008 ainda não rodada): caminho antigo, mais lento
                    loadPerExercise()
                }
                _state.value = RecordsUiState(
                    records = rows.sortedForDisplay(),
                    today = clock.todayIn(TimeZone.currentSystemDefault())
                )
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _state.value = RecordsUiState(errorMessage = e.toUserMessage("Erro ao carregar os recordes"))
            }
        }
    }

    /** Uma consulta por exercício, mas em lotes paralelos de 8 (antes eram todas em sequência). */
    private suspend fun loadPerExercise(): List<RecordRow> = coroutineScope {
        exerciseRepository.getAll()
            .mapNotNull { e -> e.id?.let { id -> id to e.name } }
            .chunked(8)
            .flatMap { lote ->
                lote.map { (id, name) ->
                    async { buildRecordRow(id, name, workoutRepository.getProgressionForExercise(id)) }
                }.awaitAll()
            }
            .filterNotNull()
    }
}
