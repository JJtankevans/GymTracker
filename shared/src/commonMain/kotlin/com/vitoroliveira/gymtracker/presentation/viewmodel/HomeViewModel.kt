// shared/src/commonMain/kotlin/com/vitoroliveira/gymtracker/presentation/viewmodel/HomeViewModel.kt

package com.vitoroliveira.gymtracker.presentation.viewmodel

import com.vitoroliveira.gymtracker.presentation.toUserMessage
import kotlin.coroutines.cancellation.CancellationException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vitoroliveira.gymtracker.data.model.Workout
import com.vitoroliveira.gymtracker.presentation.DashboardCalculator
import com.vitoroliveira.gymtracker.presentation.WeekBucket
import com.vitoroliveira.gymtracker.domain.repository.MuscleGroupRepository
import com.vitoroliveira.gymtracker.domain.repository.ProfileRepository
import com.vitoroliveira.gymtracker.domain.repository.WorkoutRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlinx.datetime.toLocalDateTime

data class HomeUiState(
    val isLoading: Boolean = false,
    val weeklyCount: Int = 0,
    val weeklyGoal: Int = 3,
    val streakDays: Int = 0,
    /** Últimas 8 semanas (mais antiga primeiro) para o gráfico de frequência. */
    val weeklyHistory: List<WeekBucket> = emptyList(),
    val totalWorkouts: Int = 0,
    /** Já formatada como "dd/MM" para exibição direta na tela. */
    val lastWorkoutDate: String? = null,
    val lastWorkoutMuscleGroupName: String? = null,
    val errorMessage: String? = null
)

class HomeViewModel(
    private val workoutRepository: WorkoutRepository,
    private val profileRepository: ProfileRepository,
    private val muscleGroupRepository: MuscleGroupRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val timeZone = TimeZone.currentSystemDefault()

    init {
        loadDashboard()
    }

    /**
     * Carrega perfil (meta semanal), histórico de treinos finalizados
     * e grupos musculares (para exibir o nome do grupo do último treino),
     * e deriva os indicadores do dashboard a partir daí.
     */
    fun loadDashboard() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val profile = profileRepository.getCurrentProfile()
                val history = workoutRepository.getHistory()
                val muscleGroups = muscleGroupRepository.getAll()

                val workoutDates = history.mapNotNull { parseLocalDate(it.startedAt) }
                val today = Clock.System.todayIn(timeZone)
                val weeklyCount = DashboardCalculator.weeklyCount(workoutDates, today)
                val streakDays = DashboardCalculator.streak(workoutDates, today)
                val weeklyHistory = DashboardCalculator.weeklyBuckets(workoutDates, today, weeks = 8)

                val lastWorkout = history.firstOrNull()
                val lastWorkoutGroupName = lastWorkout?.muscleGroupId?.let { groupId ->
                    muscleGroups.find { it.id == groupId }?.name
                }

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    weeklyCount = weeklyCount,
                    weeklyGoal = profile.weeklyGoal,
                    streakDays = streakDays,
                    weeklyHistory = weeklyHistory,
                    totalWorkouts = history.size,
                    lastWorkoutDate = lastWorkout?.let { formatDate(it.startedAt) },
                    lastWorkoutMuscleGroupName = lastWorkout?.let {
                        lastWorkoutGroupName ?: "Treino livre"
                    }
                )
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.toUserMessage("Erro ao carregar o dashboard")
                )
            }
        }
    }

    private fun parseLocalDate(isoString: String?): LocalDate? {
        if (isoString == null) return null
        return runCatching {
            Instant.parse(isoString).toLocalDateTime(timeZone).date
        }.getOrNull()
    }

    private fun formatDate(isoString: String?): String? {
        val date = parseLocalDate(isoString) ?: return null
        val day = date.dayOfMonth.toString().padStart(2, '0')
        val month = date.monthNumber.toString().padStart(2, '0')
        return "$day/$month"
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}