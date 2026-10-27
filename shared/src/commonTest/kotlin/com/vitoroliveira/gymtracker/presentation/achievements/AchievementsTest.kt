package com.vitoroliveira.gymtracker.presentation.achievements

import com.vitoroliveira.gymtracker.data.model.RecordSummary
import com.vitoroliveira.gymtracker.data.model.Workout
import com.vitoroliveira.gymtracker.domain.repository.ProfileRepository
import com.vitoroliveira.gymtracker.data.model.Profile
import com.vitoroliveira.gymtracker.presentation.viewmodel.FakeWorkoutRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private class GoalProfiles(val goal: Int) : ProfileRepository {
    override suspend fun getCurrentProfile() = Profile(id = "u1", username = "t", weeklyGoal = goal)
    override suspend fun updateProfile(profile: Profile) {}
    override suspend fun uploadAvatar(jpeg: ByteArray) = ""
    override suspend fun removeAvatar() {}
}

@OptIn(ExperimentalCoroutinesApi::class)
class AchievementsTest {
    @BeforeTest fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @AfterTest fun tearDown() = Dispatchers.resetMain()

    private fun d(day: Int) = LocalDate(2026, 10, day)

    @Test fun maiorSequenciaEmTodaAHistoria() {
        assertEquals(0, Achievements.longestStreak(emptyList()))
        assertEquals(3, Achievements.longestStreak(listOf(d(1), d(2), d(3), d(10), d(11))))
        assertEquals(2, Achievements.longestStreak(listOf(d(5), d(5), d(6)))) // dois treinos no mesmo dia contam 1 dia
    }

    @Test fun semanasComAMetaContamTreinosDeSegundaADomingo() {
        // 26/10/2026 é segunda. Semana 26-01: 3 treinos; semana 19-25: 2 treinos
        val dates = listOf(d(19), d(21), d(26), d(27), d(28))
        assertEquals(1, Achievements.weeksMeetingGoal(dates, 3))
        assertEquals(2, Achievements.weeksMeetingGoal(dates, 2))
    }

    @Test fun niveisDesbloqueiamPeloProgresso() {
        val dates = (1..12).map { d(it) }
        val all = Achievements.compute(AchievementInput(dates, weeklyGoal = 3, totalVolumeKg = 12_500.0, exercisesWithRecord = 1))
        fun a(id: String) = all.first { it.id == id }
        assertTrue(a("WORKOUTS_10").unlocked)
        assertTrue(!a("WORKOUTS_50").unlocked)
        assertEquals(12, a("WORKOUTS_50").progress)
        assertTrue(a("STREAK_7").unlocked)
        assertTrue(!a("STREAK_14").unlocked)
        assertTrue(a("VOLUME_10").unlocked)
        assertEquals(12, a("VOLUME_100").progress)
        assertTrue(a("RECORDS_1").unlocked)
    }

    @Test fun semVolumeOuRecordesAFamiliaNaoAparece() {
        val all = Achievements.compute(AchievementInput(listOf(d(1)), weeklyGoal = 3))
        assertTrue(all.none { it.family == AchievementFamily.VOLUME || it.family == AchievementFamily.RECORDS })
        assertTrue(all.first { it.id == "WORKOUTS_1" }.unlocked)
    }

    @Test fun viewModelJuntaHistoricoMetaERecordes() {
        val repo = FakeWorkoutRepository().apply {
            history += (1..4).map { Workout(id = "w$it", startedAt = "2026-10-0${it}T10:00:00Z", finishedAt = "2026-10-0${it}T11:00:00Z") }
            recordsSummary = listOf(RecordSummary("e1", "Supino", 80.0, "2026-10-01", 100.0, null, 3))
        }
        val vm = AchievementsViewModel(repo, GoalProfiles(goal = 2), social = null, zone = TimeZone.UTC)
        val s = vm.state.value
        assertTrue(!s.isLoading)
        assertTrue(s.achievements.first { it.id == "STREAK_3" }.unlocked)
        assertTrue(s.achievements.first { it.id == "RECORDS_1" }.unlocked)
        assertTrue(s.achievements.none { it.family == AchievementFamily.VOLUME }) // sem social = sem volume
        assertTrue(s.unlockedCount >= 3)
    }

    @Test fun medalhasEspeciaisPorHorarioDuracaoEDia() {
        fun at(day: Int, hour: Int, minutes: Long? = 60) = SessionInfo(kotlinx.datetime.LocalDateTime(2026, 10, day, hour, 0), minutes)
        // 26/10/2026 é segunda
        val sessions = listOf(at(26, 6), at(26, 22), at(24, 10, 95), at(25, 23, 120), at(27, 6, null))
        val all = Achievements.specials(sessions).associateBy { it.id }
        assertEquals(2, all.getValue("SPECIAL_EARLY").progress)
        assertEquals(2, all.getValue("SPECIAL_NIGHT").progress)
        assertEquals(2, all.getValue("SPECIAL_LONG").progress)
        assertEquals(2, all.getValue("SPECIAL_MONDAY").progress)
        assertEquals(2, all.getValue("SPECIAL_WEEKEND").progress)
        assertTrue(Achievements.compute(AchievementInput(emptyList(), 3)).none { it.family == AchievementFamily.SPECIAL })
    }
}
