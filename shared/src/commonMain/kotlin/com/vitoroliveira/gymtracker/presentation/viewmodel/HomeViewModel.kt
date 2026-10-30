// shared/src/commonMain/kotlin/com/vitoroliveira/gymtracker/presentation/viewmodel/HomeViewModel.kt

package com.vitoroliveira.gymtracker.presentation.viewmodel

import com.vitoroliveira.gymtracker.presentation.toUserMessage
import kotlin.coroutines.cancellation.CancellationException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vitoroliveira.gymtracker.data.model.MuscleGroup
import com.vitoroliveira.gymtracker.data.model.Workout
import com.vitoroliveira.gymtracker.presentation.workout.ActiveWorkoutInfo
import com.vitoroliveira.gymtracker.presentation.DashboardCalculator
import com.vitoroliveira.gymtracker.presentation.WeekBucket
import com.vitoroliveira.gymtracker.domain.repository.MuscleGroupRepository
import com.vitoroliveira.gymtracker.domain.resolveGroupName
import com.vitoroliveira.gymtracker.presentation.workout.suggestNextGroup
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
    /** Semanas seguidas batendo a meta semanal (a atual conta se já bateu). */
    val weekStreak: Int = 0,
    /** Últimas 8 semanas (mais antiga primeiro) para o gráfico de frequência. */
    val weeklyHistory: List<WeekBucket> = emptyList(),
    val totalWorkouts: Int = 0,
    /** Já formatada como "dd/MM" para exibição direta na tela. */
    val lastWorkoutDate: String? = null,
    val lastWorkoutMuscleGroupName: String? = null,
    /** Próximo grupo da rotação (o treinado há mais tempo); null se não há o que sugerir. */
    val suggestedGroupId: String? = null,
    /** Pedidos para me seguir esperando resposta (selo no ícone do Social). */
    val pendingFollowRequests: Int = 0,
    /** Minha posição na liga da semana (1 = primeiro) e quantos estão nela; null sem liga (migração 015) ou sozinho. */
    val leaguePosition: Int? = null,
    val leagueSize: Int = 0,
    /** Treino iniciado e não terminado (a Home oferece continuar ou descartar); null se não há. */
    val activeWorkout: ActiveWorkoutInfo? = null,
    val errorMessage: String? = null
)

class HomeViewModel(
    private val workoutRepository: WorkoutRepository,
    private val profileRepository: ProfileRepository,
    private val muscleGroupRepository: MuscleGroupRepository,
    private val socialRepository: com.vitoroliveira.gymtracker.domain.repository.SocialRepository? = null
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
                val weekStreak = DashboardCalculator.weekStreak(workoutDates, today, profile.weeklyGoal)
                val weeklyHistory = DashboardCalculator.weeklyBuckets(workoutDates, today, weeks = 8)

                val lastWorkout = history.firstOrNull()
                val lastWorkoutGroupName = lastWorkout?.resolveGroupName(muscleGroups.associateBy { it.id })

                val league = leagueQuietly()
                val active = activeWorkoutQuietly(muscleGroups.associateBy { it.id })
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    activeWorkout = active,
                    leaguePosition = league?.first,
                    leagueSize = league?.second ?: 0,
                    weeklyCount = weeklyCount,
                    weeklyGoal = profile.weeklyGoal,
                    streakDays = streakDays,
                    weekStreak = weekStreak,
                    weeklyHistory = weeklyHistory,
                    totalWorkouts = history.size,
                    suggestedGroupId = suggestNextGroup(muscleGroups, history),
                    pendingFollowRequests = countFollowRequestsQuietly(),
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

    /** Treino em andamento é um aviso: se a consulta falhar, a Home segue sem ele. */
    private suspend fun activeWorkoutQuietly(groups: Map<String?, MuscleGroup>): ActiveWorkoutInfo? {
        return try {
            val w = workoutRepository.getActiveWorkout() ?: return null
            val id = w.id ?: return null
            val started = w.startedAt?.let { runCatching { Instant.parse(it) }.getOrNull() }
            ActiveWorkoutInfo(
                workoutId = id,
                groupId = w.muscleGroupId,
                groupName = w.resolveGroupName(groups) ?: "Treino livre",
                minutes = started?.let { (Clock.System.now() - it).inWholeMinutes.coerceAtLeast(0) } ?: 0
            )
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            null
        }
    }

    /** Cancela o treino em andamento (apaga ele e as séries); se falhar, o aviso continua e aparece o erro. */
    fun discardActiveWorkout() {
        val id = _uiState.value.activeWorkout?.workoutId ?: return
        viewModelScope.launch {
            try {
                workoutRepository.deleteWorkout(id)
                _uiState.value = _uiState.value.copy(activeWorkout = null)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _uiState.value = _uiState.value.copy(errorMessage = e.toUserMessage("Erro ao descartar o treino"))
            }
        }
    }

    /** (minha posição, tamanho) na liga da semana; null se não há liga, se estou sozinho ou se falhar. */
    private suspend fun leagueQuietly(): Pair<Int, Int>? {
        val repo = socialRepository ?: return null
        return try {
            val rows = repo.weeklyLeague()
            val me = rows.indexOfFirst { it.isMe }
            if (rows.size < 2 || me < 0) null else (me + 1) to rows.size
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            null
        }
    }

    /** Contagem é um extra: sem rede ou sem as tabelas sociais, fica 0 e a Home segue normal. */
    private suspend fun countFollowRequestsQuietly(): Int {
        val repo = socialRepository ?: return 0
        return try {
            val me = repo.currentUserId()
            repo.follows().count { it.followedId == me && it.status == "pending" }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            0
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}