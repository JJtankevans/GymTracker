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
    /** Backup completo em JSON pronto para a tela salvar e compartilhar (#364). */
    val backupJson: String? = null,
    /** Resultado de "Restaurar catálogo" (#364), para a tela mostrar e limpar. */
    val restoredMessage: String? = null,
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

    /** Backup completo: grupos, exercícios, treinos finalizados e séries, num arquivo só. */
    fun backup() {
        if (_state.value.isExporting) return
        viewModelScope.launch {
            _state.value = ExportUiState(isExporting = true)
            try {
                val sets = workouts.getAllSetsForExport()
                val history = workouts.getHistory()
                val json = buildBackupJson(
                    createdAt = kotlin.time.Clock.System.now().toString(),
                    groups = groups.getAll(),
                    exercises = exercises.getAll(),
                    workouts = history,
                    sets = sets
                )
                _state.value = ExportUiState(backupJson = json, rows = sets.size)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _state.value = ExportUiState(errorMessage = e.toUserMessage("Erro ao gerar o backup"))
            }
        }
    }

    /**
     * Restaura o catálogo (grupos e exercícios) de um backup JSON: só cria o que ainda não existe na conta, nunca apaga.
     * O histórico não é restaurado (ver BackupRestore.kt).
     */
    fun restoreCatalog(json: String) {
        if (_state.value.isExporting) return
        viewModelScope.launch {
            _state.value = ExportUiState(isExporting = true)
            try {
                val backup = try {
                    parseBackupJson(json)
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    _state.value = ExportUiState(errorMessage = "Esse arquivo não parece um backup do GymTracker")
                    return@launch
                }
                val existingGroups = groups.getAll()
                val plan = planCatalogRestore(backup, existingGroups, exercises.getAll())
                val idMap = mutableMapOf<String, String>()
                // grupos que já existiam (mesmo nome): o id antigo aponta para o da conta
                backup.groups.forEach { g ->
                    val mine = existingGroups.firstOrNull { it.name.trim().equals(g.name.trim(), ignoreCase = true) }?.id
                    if (mine != null && g.id != null) idMap[g.id] = mine
                }
                plan.newGroups.forEach { g ->
                    val created = groups.insert(g.copy(id = null, profileId = null, createdAt = null))
                    if (g.id != null && created.id != null) idMap[g.id] = created.id
                }
                plan.newExercises.forEach { e ->
                    exercises.insert(
                        e.copy(id = null, profileId = null, createdAt = null, updatedAt = null, groupIds = remapExerciseGroups(e.id, backup, idMap))
                    )
                }
                _state.value = ExportUiState(
                    restoredMessage = "Catálogo restaurado: ${plan.newGroups.size} grupos e ${plan.newExercises.size} exercícios novos (${plan.skipped} já existiam)"
                )
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _state.value = ExportUiState(errorMessage = e.toUserMessage("Erro ao restaurar o backup"))
            }
        }
    }

    fun consumed() { _state.value = ExportUiState() }
}
