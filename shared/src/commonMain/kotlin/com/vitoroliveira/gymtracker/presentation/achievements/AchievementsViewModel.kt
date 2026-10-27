package com.vitoroliveira.gymtracker.presentation.achievements

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vitoroliveira.gymtracker.domain.repository.ProfileRepository
import com.vitoroliveira.gymtracker.domain.repository.SocialRepository
import com.vitoroliveira.gymtracker.domain.repository.WorkoutRepository
import com.vitoroliveira.gymtracker.presentation.toUserMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Instant

data class AchievementsUiState(
    val isLoading: Boolean = true,
    val achievements: List<Achievement> = emptyList(),
    val errorMessage: String? = null
) {
    val unlockedCount: Int get() = achievements.count { it.unlocked }
}

class AchievementsViewModel(
    private val workouts: WorkoutRepository,
    private val profiles: ProfileRepository,
    private val social: SocialRepository? = null,
    private val zone: TimeZone = TimeZone.currentSystemDefault()
) : ViewModel() {
    private val _state = MutableStateFlow(AchievementsUiState())
    val state: StateFlow<AchievementsUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, errorMessage = null)
            try {
                val sessions = workouts.getHistory().mapNotNull { w ->
                    val start = w.startedAt?.let { runCatching { Instant.parse(it) }.getOrNull() } ?: return@mapNotNull null
                    val end = w.finishedAt?.let { runCatching { Instant.parse(it) }.getOrNull() }
                    SessionInfo(start.toLocalDateTime(zone), end?.let { (it - start).inWholeMinutes })
                }
                val dates = sessions.map { it.start.date }
                // os extras abaixo são opcionais: se falharem, a família correspondente só não aparece
                val goal = optional { profiles.getCurrentProfile().weeklyGoal } ?: 3
                val volume = optional { social?.let { s -> s.profileStats(s.currentUserId())?.volumeKg } }
                val records = optional { workouts.getRecordsSummary().size }
                _state.value = AchievementsUiState(
                    isLoading = false,
                    achievements = Achievements.compute(AchievementInput(dates, goal, volume, records, sessions))
                )
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _state.value = AchievementsUiState(isLoading = false, errorMessage = e.toUserMessage("Erro ao carregar as conquistas"))
            }
        }
    }

    private suspend fun <T> optional(block: suspend () -> T?): T? =
        try { block() } catch (e: Exception) { if (e is CancellationException) throw e; null }
}
