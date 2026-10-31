package com.vitoroliveira.gymtracker.presentation.social

import com.vitoroliveira.gymtracker.data.model.GroupMemberRow
import com.vitoroliveira.gymtracker.presentation.viewmodel.FixedClock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.TimeZone
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

class GroupRulesTest {
    @Test fun quemPodeTirarQuem() {
        assertTrue(canRemoveMember("owner", "admin")); assertTrue(canRemoveMember("owner", "member")); assertFalse(canRemoveMember("owner", "owner"))
        assertTrue(canRemoveMember("admin", "member")); assertFalse(canRemoveMember("admin", "admin")); assertFalse(canRemoveMember("admin", "owner"))
        assertFalse(canRemoveMember("member", "member")); assertFalse(canRemoveMember(null, "member"))
    }

    @Test fun soODonoMudaCargo() {
        assertTrue(canChangeRole("owner", "member")); assertTrue(canChangeRole("owner", "admin"))
        assertFalse(canChangeRole("owner", "owner")); assertFalse(canChangeRole("admin", "member")); assertFalse(canChangeRole("member", "member"))
        assertEquals("member", toggledRole("admin")); assertEquals("admin", toggledRole("member"))
    }

    @Test fun quemApagaMensagem() {
        assertTrue(canDeleteMessage("member", "eu", "eu"))       // a própria
        assertFalse(canDeleteMessage("member", "outro", "eu"))   // de outro, sem cargo
        assertTrue(canDeleteMessage("admin", "outro", "eu")); assertTrue(canDeleteMessage("owner", "outro", "eu"))
    }

    @Test fun mensagemLimpaOuRecusada() {
        assertEquals("oi", cleanMessage("  oi \n"))
        assertNull(cleanMessage("   ")); assertNull(cleanMessage(""))
        assertEquals(280, cleanMessage("x".repeat(280))?.length); assertNull(cleanMessage("x".repeat(281)))
    }

    private val agora = Instant.parse("2026-11-05T15:00:00Z")

    @Test fun idadeDaMensagem() {
        fun age(iso: String) = messageAge(iso, agora, TimeZone.UTC)
        assertEquals("agora", age("2026-11-05T14:59:30Z"))
        assertEquals("5 min", age("2026-11-05T14:55:00Z"))
        assertEquals("3 h", age("2026-11-05T12:00:00Z"))
        assertEquals("ontem", age("2026-11-04T09:00:00Z"))
        assertEquals("30/10", age("2026-10-30T09:00:00Z"))
        assertEquals("", age("isso não é data"))
    }

    private fun row(name: String, w: Int, d: Int, m: Int) = GroupMemberRow("p$name", name, null, "member", w, d, m)

    @Test fun rankingDaSemanaDesempataEDeixaQuemNaoTreinouSemPosicao() {
        val r = rankMembers(listOf(row("zeca", 0, 0, 0), row("ana", 3, 3, 150), row("bia", 3, 2, 200), row("caio", 3, 3, 150), row("duda", 1, 1, 30)))
        assertEquals(listOf("ana", "caio", "bia", "duda", "zeca"), r.map { it.second.username })
        assertEquals(listOf<Int?>(1, 1, 3, 4, null), r.map { it.first })   // ana e caio empatam; bia perde nos dias ativos
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class GroupLifeViewModelTest {
    private lateinit var repo: FakeSocialRepository
    private val g = "g1"

    @BeforeTest fun setUp() { Dispatchers.setMain(UnconfinedTestDispatcher()); repo = FakeSocialRepository() }
    @AfterTest fun tearDown() = Dispatchers.resetMain()

    private fun vm() = SocialViewModel(repo, FixedClock(Instant.parse("2026-10-20T12:00:00Z")))

    @Test fun abrirOGrupoCarregaRankingSequenciaEMural() {
        repo.boardFor[g] = listOf(GroupMemberRow("me", "eu", null, "owner", 2, 2, 90, true))
        repo.streakFor[g] = 4
        val vm = vm()
        vm.postGroupMessage(g, "bora")
        vm.openGroup(g)
        val s = vm.state.value
        assertEquals(1, s.groupBoards[g]?.size); assertEquals(4, s.groupStreaks[g]); assertEquals(listOf("bora"), s.groupMessages[g]?.map { it.body })
    }

    @Test fun mensagemVaziaOuLongaDemaisNaoEnvia() {
        val vm = vm()
        vm.postGroupMessage(g, "   ")
        assertEquals("Escreva uma mensagem de até 280 caracteres.", vm.state.value.error)
        vm.clearMessages(); vm.postGroupMessage(g, "x".repeat(300))
        assertEquals("Escreva uma mensagem de até 280 caracteres.", vm.state.value.error)
        assertTrue(repo.messagesFor[g].isNullOrEmpty())
    }

    @Test fun apagarMensagemRecarregaOMural() {
        val vm = vm()
        vm.postGroupMessage(g, "uma"); vm.postGroupMessage(g, "duas")
        val id = vm.state.value.groupMessages[g]!!.first().id
        vm.deleteGroupMessage(g, id)
        assertEquals(listOf("uma"), vm.state.value.groupMessages[g]?.map { it.body })
    }

    @Test fun tirarMembroEMudarCargoChamamORepositorio() {
        val vm = vm()
        vm.removeMember(g, "joao"); assertEquals(listOf(g to "joao"), repo.removedMembers); assertEquals("Membro removido", vm.state.value.message)
        vm.setMemberRole(g, "maria", "admin"); assertEquals(listOf(Triple(g, "maria", "admin")), repo.roleChanges); assertEquals("Cargo atualizado", vm.state.value.message)
    }

    @Test fun rankingIndisponivelNaoDerrubaOGrupo() {
        repo.failBoard = true
        val vm = vm()
        vm.openGroup(g)
        assertNull(vm.state.value.groupBoards[g]); assertNull(vm.state.value.error)
    }
}
