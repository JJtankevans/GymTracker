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
import com.vitoroliveira.gymtracker.domain.resolveGroupName
import com.vitoroliveira.gymtracker.presentation.workout.settleIfStale
import com.vitoroliveira.gymtracker.presentation.workout.suggestNextGroup
import com.vitoroliveira.gymtracker.domain.repository.ProfileRepository
import com.vitoroliveira.gymtracker.domain.repository.WorkoutRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.todayIn
import kotlin.time.Duration.Companion.days
import kotlinx.datetime.toLocalDateTime

/** Resultado das consultas paralelas da Home. */
private data class DashboardData(
    val profile: com.vitoroliveira.gymtracker.data.model.Profile,
    val history: List<com.vitoroliveira.gymtracker.data.model.Workout>,
    val muscleGroups: List<com.vitoroliveira.gymtracker.data.model.MuscleGroup>,
    val active: com.vitoroliveira.gymtracker.data.model.Workout?,
    val pendingFollows: Int
)

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
    /** Próximo grupo da rotação (o treinado há mais tempo); null se não há o que sugerir. */
    val suggestedGroupId: String? = null,
    /** Nome do grupo sugerido (para o cartão "Próximo treino" da Home). */
    val suggestedGroupName: String? = null,
    /** true quando o grupo sugerido vem da agenda semanal de hoje (e não da rotação). */
    val suggestedFromSchedule: Boolean = false,
    /** id → nome de todos os grupos (a tela cruza com a agenda de hoje). */
    val groupNames: Map<String, String> = emptyMap(),
    /** Treino iniciado e ainda não finalizado (para o aviso "Retomar" da Home). */
    val activeWorkoutStartedAt: String? = null,
    val activeWorkoutGroupId: String? = null,
    val activeWorkoutGroupName: String? = null,
    val hasActiveWorkout: Boolean = false,
    /** Pedidos para me seguir esperando resposta (selo no ícone do Social). */
    val pendingFollowRequests: Int = 0,
    /** Painel "Hoje": volume (kg) da semana atual e da anterior; null se não carregou. */
    val weekVolumeKg: Double? = null,
    val prevWeekVolumeKg: Double = 0.0,
    val daysSinceLastWorkout: Int? = null,
    val suggestDeload: Boolean = false,
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
                // As cinco consultas são independentes: rodam juntas (antes eram em fila, uma espera por rede cada).
                val (profile, history, muscleGroups, active, pendingFollows) = coroutineScope {
                    val profileD = async { profileRepository.getCurrentProfile() }
                    val historyD = async { workoutRepository.getHistory() }
                    val groupsD = async { muscleGroupRepository.getAll() }
                    // Treino aberto e pedidos para seguir são extras: se falharem, a Home segue sem eles.
                    val activeD = async {
                        try {
                            workoutRepository.getActiveWorkout()?.let { workoutRepository.settleIfStale(it, Clock.System.now()) }
                        } catch (e: Exception) {
                            if (e is CancellationException) throw e
                            null
                        }
                    }
                    val followsD = async { countFollowRequestsQuietly() }
                    DashboardData(profileD.await(), historyD.await(), groupsD.await(), activeD.await(), followsD.await())
                }

                val workoutDates = history.mapNotNull { parseLocalDate(it.startedAt) }
                val today = Clock.System.todayIn(timeZone)
                val weeklyCount = DashboardCalculator.weeklyCount(workoutDates, today)
                val streakDays = DashboardCalculator.streak(workoutDates, today)
                val weeklyHistory = DashboardCalculator.weeklyBuckets(workoutDates, today, weeks = 8)

                val lastWorkout = history.firstOrNull()
                // painel "Hoje" é um extra: se a consulta das séries falhar, a Home segue sem ele
                val volumes = try {
                    val since = (Clock.System.now() - 84.days).toString()
                    val raw = workoutRepository.getSetsSince(since)
                    val pts = raw.mapNotNull { s -> parseLocalDate(s.workouts?.startedAt)?.let { it to s.reps * s.weightKg } }
                    val v = com.vitoroliveira.gymtracker.presentation.body.weeklyVolumes(pts, today)
                    v
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    null
                }
                val lastDate = lastWorkout?.let { parseLocalDate(it.startedAt) }
                val lastWorkoutGroupName = lastWorkout?.resolveGroupName(muscleGroups.associateBy { it.id })

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    weeklyCount = weeklyCount,
                    weeklyGoal = profile.weeklyGoal,
                    streakDays = streakDays,
                    weeklyHistory = weeklyHistory,
                    totalWorkouts = history.size,
                    suggestedGroupId = suggestNextGroup(muscleGroups, history),
                    groupNames = muscleGroups.mapNotNull { g -> g.id?.let { it to g.name } }.toMap(),
                    suggestedGroupName = suggestNextGroup(muscleGroups, history)?.let { id -> muscleGroups.firstOrNull { it.id == id }?.name },
                    pendingFollowRequests = pendingFollows,
                    weekVolumeKg = volumes?.last(),
                    prevWeekVolumeKg = volumes?.getOrNull(volumes.size - 2) ?: 0.0,
                    daysSinceLastWorkout = lastDate?.let { it.daysUntil(today) },
                    suggestDeload = volumes?.let { com.vitoroliveira.gymtracker.presentation.body.needsDeload(it) } ?: false,
                    hasActiveWorkout = active != null,
                    activeWorkoutStartedAt = active?.startedAt,
                    activeWorkoutGroupId = active?.muscleGroupId,
                    activeWorkoutGroupName = active?.resolveGroupName(muscleGroups.associateBy { it.id }),
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