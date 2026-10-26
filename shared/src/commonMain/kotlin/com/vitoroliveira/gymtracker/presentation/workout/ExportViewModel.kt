package com.vitoroliveira.gymtracker.presentation.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vitoroliveira.gymtracker.domain.repository.ExerciseRepository
import com.vitoroliveira.gymtracker.domain.repository.MuscleGroupRepository
import com.vitoroliveira.gymtracker.domain.repository.WorkoutRepository
import com.vitoroliveira.gymtracker.presentation.toUserMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

data class ExportUiState(
    val isExporting: Boolean = false,
    /** CSV pronto para a tela salvar e compartilhar; a tela chama [ExportViewModel.consumed] depois. */
    val csv: String? = null,
    val rows: Int = 0,
    val errorMessage: String? = null
)

/** Gera o CSV do histórico inteiro (Perfil > Exportar meus treinos). */
class ExportViewModel(
    private val workouts: WorkoutRepository,
    private val exercises: ExerciseRepository,
    private val groups: MuscleGroupRepository
) : ViewModel() {
    private val _state = MutableStateFlow(ExportUiState())
    val state: StateFlow<ExportUiState> = _state.asStateFlow()

    fun export() {
        if (_state.value.isExporting) return
        viewModelScope.launch {
            _state.value = ExportUiState(isExporting = true)
            try {
                val sets = workouts.getAllSetsForExport()
                if (sets.isEmpty()) {
                    _state.value = ExportUiState(errorMessage = "Ainda não há séries registradas para exportar")
                    return@launch
                }
                val names = exercises.getAll().mapNotNull { e -> e.id?.let { it to e.name } }.toMap()
                val groupNames = groups.getAll().mapNotNull { g -> g.id?.let { it to g.name } }.toMap()
                _state.value = ExportUiState(csv = historyCsv(sets, names, groupNames), rows = sets.size)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _state.value = ExportUiState(errorMessage = e.toUserMessage("Erro ao exportar os treinos"))
            }
        }
    }

    fun consumed() { _state.value = ExportUiState() }
}
