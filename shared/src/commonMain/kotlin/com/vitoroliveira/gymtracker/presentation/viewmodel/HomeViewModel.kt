// shared/src/commonMain/kotlin/com/vitoroliveira/gymtracker/presentation/viewmodel/HomeViewModel.kt

package com.vitoroliveira.gymtracker.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vitoroliveira.gymtracker.data.model.Workout
import com.vitoroliveira.gymtracker.domain.repository.MuscleGroupRepository
import com.vitoroliveira.gymtracker.domain.repository.ProfileRepository
import com.vitoroliveira.gymtracker.domain.repository.WorkoutRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlinx.datetime.DateTimeUnit
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import kotlinx.datetime.toLocalDateTime

data class HomeUiState(
    val isLoading: Boolean = false,
    val weeklyCount: Int = 0,
    val weeklyGoal: Int = 3,
    val streakDays: Int = 0,
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

                val weeklyCount = calculateWeeklyCount(history)
                val streakDays = calculateStreak(
                    history.mapNotNull { parseLocalDate(it.startedAt) }
                )

                val lastWorkout = history.firstOrNull()
                val lastWorkoutGroupName = lastWorkout?.muscleGroupId?.let { groupId ->
                    muscleGroups.find { it.id == groupId }?.name
                }

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    weeklyCount = weeklyCount,
                    weeklyGoal = profile.weeklyGoal,
                    streakDays = streakDays,
                    lastWorkoutDate = lastWorkout?.let { formatDate(it.startedAt) },
                    lastWorkoutMuscleGroupName = lastWorkout?.let {
                        lastWorkoutGroupName ?: "Treino livre"
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "Erro ao carregar o dashboard"
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

    /**
     * Conta treinos cuja data cai na semana atual (segunda a domingo),
     * mesmo critério do DATE_TRUNC('week', ...) usado na view
     * `vw_weekly_frequency` do Supabase.
     */
    private fun calculateWeeklyCount(workouts: List<Workout>): Int {
        val today = Clock.System.todayIn(timeZone)
        val daysFromMonday = today.dayOfWeek.isoDayNumber - 1
        val weekStart = today.minus(daysFromMonday, DateTimeUnit.DAY)
        val weekEnd = weekStart.plus(6, DateTimeUnit.DAY)

        return workouts.count { workout ->
            val date = parseLocalDate(workout.startedAt) ?: return@count false
            date in weekStart..weekEnd
        }
    }

    /**
     * Conta dias consecutivos com pelo menos um treino, terminando hoje
     * ou ontem (se o usuário ainda não treinou hoje, a streak continua
     * valendo até o fim do dia). Quebra assim que encontra um "buraco".
     */
    private fun calculateStreak(workoutDates: List<LocalDate>): Int {
        if (workoutDates.isEmpty()) return 0

        val distinctDates = workoutDates.distinct().sortedDescending()
        val today = Clock.System.todayIn(timeZone)
        val mostRecent = distinctDates.first()

        // Se o treino mais recente foi antes de ontem, a streak já quebrou
        if (mostRecent < today.minus(1, DateTimeUnit.DAY)) return 0

        var streak = 1
        var expected = mostRecent.minus(1, DateTimeUnit.DAY)
        for (date in distinctDates.drop(1)) {
            if (date == expected) {
                streak++
                expected = expected.minus(1, DateTimeUnit.DAY)
            } else if (date < expected) {
                break
            }
        }
        return streak
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}