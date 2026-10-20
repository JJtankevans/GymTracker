package com.vitoroliveira.gymtracker.presentation.records

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vitoroliveira.gymtracker.domain.repository.ExerciseRepository
import com.vitoroliveira.gymtracker.domain.repository.WorkoutRepository
import com.vitoroliveira.gymtracker.presentation.toUserMessage
import kotlinx.coroutines.CancellationException
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
                val rows = exerciseRepository.getAll().mapNotNull { e ->
                    val id = e.id ?: return@mapNotNull null
                    buildRecordRow(id, e.name, workoutRepository.getProgressionForExercise(id))
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
}
