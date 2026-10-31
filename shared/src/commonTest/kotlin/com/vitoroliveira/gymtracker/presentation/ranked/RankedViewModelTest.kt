package com.vitoroliveira.gymtracker.presentation.ranked

import com.vitoroliveira.gymtracker.data.model.RankedBoardRow
import com.vitoroliveira.gymtracker.data.model.RankedDuel
import com.vitoroliveira.gymtracker.data.model.RankedLastResult
import com.vitoroliveira.gymtracker.data.model.RankedPlayer
import com.vitoroliveira.gymtracker.domain.repository.RankedRepository
import com.vitoroliveira.gymtracker.presentation.viewmodel.FixedClock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

private class FakeRanked : RankedRepository {
    var player: RankedPlayer? = null
    var board: List<RankedBoardRow> = emptyList()
    var last: RankedLastResult? = null
    var duelList: List<RankedDuel> = emptyList()
    var failPlayer: Throwable? = null
    var failCreate: Throwable? = null
    val settledWeeks = mutableListOf<String>()
    var settledDuels = 0
    val created = mutableListOf<Pair<String, Boolean>>()
    val responses = mutableListOf<Pair<String, Boolean>>()
    var joins = 0

    override fun currentUserId() = "me"
    override suspend fun player(): RankedPlayer? { failPlayer?.let { throw it }; return player }
    override suspend fun join(): Int { joins++; player = RankedPlayer("me", 0); return 0 }
    override suspend fun board() = board
    override suspend fun lastResult() = last
    override suspend fun settleWeek(weekStart: String): Int { settledWeeks += weekStart; return 0 }
    override suspend fun settleDuels(): Int { settledDuels++; return 0 }
    override suspend fun duels() = duelList
    override suspend fun createDuel(opponentId: String, ranked: Boolean): String { failCreate?.let { throw it }; created += opponentId to ranked; return "d1" }
    override suspend fun respondDuel(duelId: String, accept: Boolean) { responses += duelId to accept }
}

@OptIn(ExperimentalCoroutinesApi::class)
class RankedViewModelTest {
    // quinta-feira 05/11/2026 (a semana começou na segunda 02/11)
    private val clock = FixedClock(Instant.parse("2026-11-05T15:00:00Z"))
    private lateinit var repo: FakeRanked

    @BeforeTest fun setUp() { Dispatchers.setMain(UnconfinedTestDispatcher()); repo = FakeRanked() }
    @AfterTest fun tearDown() = Dispatchers.resetMain()

    private fun vm() = RankedViewModel(repo, clock)
    private fun row(pos: Int, id: String, pts: Int, me: Boolean = false, rp: Int = 100) = RankedBoardRow(pos, id, "u$id", null, rp, pts, me)

    @Test fun semEntrarNaRanqueadaNaoCarregaMais() {
        val s = vm().state.value
        assertNull(s.player); assertNull(s.rank); assertTrue(s.board.isEmpty()); assertNull(s.loadError)
        assertTrue(repo.settledWeeks.isEmpty())
    }

    @Test fun entrarMostraOElo() {
        val vm = vm()
        vm.join()
        val s = vm.state.value
        assertEquals(1, repo.joins)
        assertEquals("Ferro III", s.rank?.label)
        assertEquals("Você entrou na ranqueada! Bora subir de elo.", s.message)
    }

    @Test fun carregaOTudoEFechaASemanaPassada() {
        repo.player = RankedPlayer("me", rp = 350, peakRp = 400, weeksPlayed = 3)
        repo.board = listOf(row(1, "a", 300), row(2, "me", 200, me = true), row(3, "b", 100))
        repo.last = RankedLastResult("2026-10-26", 300, 2, 12, 10, 350)
        val s = vm().state.value
        assertEquals(listOf("2026-10-26"), repo.settledWeeks) // segunda anterior à semana de 02/11
        assertEquals(1, repo.settledDuels)
        assertEquals("Bronze III", s.rank?.label)
        assertEquals(2, s.season?.weekInSeason)                 // 02/11 é a semana 2 da temporada 1
        assertEquals(4, s.daysLeft)                            // quinta: quinta, sexta, sábado, domingo
        assertEquals(2, s.analysis?.position)
        assertNotNull(s.lastResult)
    }

    @Test fun falhaNoPlayerMostraOMotivo() {
        repo.failPlayer = RuntimeException("Could not find the table 'public.ranked_players' in the schema cache")
        val s = vm().state.value
        assertEquals("A ranqueada ainda não está ativada no servidor (falta rodar a migração 018).", s.loadError)
        assertFalse(s.isLoading)
    }

    @Test fun semInternetEhErroDeRede() {
        repo.failPlayer = RuntimeException("Unable to resolve host")
        assertEquals("Sem conexão com a internet. Verifique sua rede e tente de novo.", vm().state.value.loadError)
    }

    @Test fun desafiarEnviaOPedidoELimpaOOcupado() {
        repo.player = RankedPlayer("me", 500)
        val vm = vm()
        vm.challenge("a", ranked = true)
        assertEquals(listOf("a" to true), repo.created)
        assertFalse(vm.state.value.busy)
        assertEquals("x1 valendo RP enviado", vm.state.value.message)
        vm.challenge("b", ranked = false)
        assertEquals("x1 amistoso enviado", vm.state.value.message)
    }

    @Test fun regraDoBancoApareceComoVeio() {
        repo.player = RankedPlayer("me", 500)
        repo.failCreate = RuntimeException("x1 valendo RP é só entre pessoas do mesmo elo")
        val vm = vm()
        vm.challenge("a", true)
        assertEquals("x1 valendo RP é só entre pessoas do mesmo elo", vm.state.value.error)
        assertFalse(vm.state.value.busy)
    }

    @Test fun responderAceitaORecusa() {
        repo.player = RankedPlayer("me", 500)
        val vm = vm()
        vm.respond("d1", true); assertEquals("x1 aceito: valem 7 dias!", vm.state.value.message)
        vm.respond("d2", false); assertEquals("x1 recusado", vm.state.value.message)
        assertEquals(listOf("d1" to true, "d2" to false), repo.responses)
    }

    @Test fun regrasDeMensagemDoBanco() {
        assertEquals("Já existe um x1 em aberto entre vocês", ruleMessage("ERROR: Já existe um x1 em aberto entre vocês (P0001)"))
        assertNull(ruleMessage("qualquer outro erro")); assertNull(ruleMessage(null))
    }
}
