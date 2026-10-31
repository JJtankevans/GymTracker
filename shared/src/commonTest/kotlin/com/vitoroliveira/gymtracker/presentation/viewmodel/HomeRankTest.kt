package com.vitoroliveira.gymtracker.presentation.viewmodel

import com.vitoroliveira.gymtracker.data.model.MuscleGroup
import com.vitoroliveira.gymtracker.data.model.Profile
import com.vitoroliveira.gymtracker.data.model.RankedBoardRow
import com.vitoroliveira.gymtracker.data.model.RankedDuel
import com.vitoroliveira.gymtracker.data.model.RankedLastResult
import com.vitoroliveira.gymtracker.data.model.RankedPlayer
import com.vitoroliveira.gymtracker.domain.repository.MuscleGroupRepository
import com.vitoroliveira.gymtracker.domain.repository.ProfileRepository
import com.vitoroliveira.gymtracker.domain.repository.RankedRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

private class RankHomeProfile : ProfileRepository {
    override suspend fun getCurrentProfile() = Profile(id = "u1", username = "t")
    override suspend fun updateProfile(profile: Profile) {}
    override suspend fun uploadAvatar(jpeg: ByteArray) = ""
    override suspend fun removeAvatar() {}
}

private class RankHomeGroups : MuscleGroupRepository {
    override suspend fun getAll(): List<MuscleGroup> = emptyList()
    override suspend fun insert(muscleGroup: MuscleGroup) = muscleGroup
    override suspend fun update(muscleGroup: MuscleGroup) {}
    override suspend fun delete(id: String) {}
}

private class RankHomePlayer(val player: RankedPlayer?, val fail: Boolean = false) : RankedRepository {
    override fun currentUserId() = "u1"
    override suspend fun player(): RankedPlayer? { if (fail) throw RuntimeException("Could not find the table 'public.ranked_players'"); return player }
    override suspend fun join() = 0
    override suspend fun board(): List<RankedBoardRow> = emptyList()
    override suspend fun lastResult(): RankedLastResult? = null
    override suspend fun settleWeek(weekStart: String) = 0
    override suspend fun settleDuels() = 0
    override suspend fun duels(): List<RankedDuel> = emptyList()
    override suspend fun createDuel(opponentId: String, ranked: Boolean) = ""
    override suspend fun respondDuel(duelId: String, accept: Boolean) {}
}

@OptIn(ExperimentalCoroutinesApi::class)
class HomeRankTest {
    @BeforeTest fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @AfterTest fun tearDown() = Dispatchers.resetMain()

    private fun vm(ranked: RankedRepository?) = HomeViewModel(FakeWorkoutRepository(), RankHomeProfile(), RankHomeGroups(), null, ranked)

    @Test fun mostraOEloDeQuemEstaNaRanqueada() {
        val rank = vm(RankHomePlayer(RankedPlayer("u1", 350))).uiState.value.rank
        assertEquals("Bronze III", rank?.label)
    }

    @Test fun semEntrarOuSemAMigracaoNaoMostraChipNemQuebraAHome() {
        assertNull(vm(RankHomePlayer(null)).uiState.value.rank)
        val s = vm(RankHomePlayer(null, fail = true)).uiState.value
        assertNull(s.rank); assertNull(s.errorMessage)
        assertNull(vm(null).uiState.value.rank)
    }
}
