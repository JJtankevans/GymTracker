package com.vitoroliveira.gymtracker.presentation.social

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.time.Instant

class IdentityTest {
    private val now = Instant.parse("2026-10-30T12:00:00Z")

    @Test fun limpaOQueNaoEhValido() {
        val l = Identity.clean(Look(cover = "tema_que_nao_existe", accentHex = "verde", icon = "emoji"))
        assertEquals(Look(), l)
    }

    @Test fun mantemOQueEhValidoECorfica() {
        val l = Identity.clean(Look(cover = " radar ", accentHex = " #c6f432 ", icon = "trophy"))
        assertEquals(Look("radar", "#C6F432", "trophy"), l)
    }

    @Test fun todasAsOpcoesDaListaSaoValidas() {
        Identity.covers.forEach { assertEquals(it, Identity.clean(Look(cover = it)).cover) }
        Identity.accents.forEach { assertEquals(it, Identity.clean(Look(accentHex = it)).accentHex) }
        Identity.icons.forEach { assertEquals(it, Identity.clean(Look(icon = it)).icon) }
    }

    @Test fun grupoNovoLevaIdentidadeEMetaSoQuandoDefinidas() {
        val g = GroupForm("Time do Peito", look = Look("sakura", "#ff4d8d", "fire"), weeklyGoal = 20).toNew("me")
        assertEquals("sakura", g.cover); assertEquals("#FF4D8D", g.accentHex); assertEquals("fire", g.icon); assertEquals(20, g.weeklyGoal)
        val plain = GroupForm("Time do Peito").toNew("me")
        assertNull(plain.cover); assertNull(plain.accentHex); assertNull(plain.icon); assertNull(plain.weeklyGoal)
    }

    @Test fun metaDoGrupoPrecisaFicarEntre1e500() {
        assertFailsWith<IllegalArgumentException> { GroupForm("Time do Peito", weeklyGoal = 0).toNew("me") }
        assertFailsWith<IllegalArgumentException> { GroupForm("Time do Peito", weeklyGoal = 501).toNew("me") }
    }

    @Test fun edicaoDoGrupoLimpaCamposVaziosEValida() {
        val e = GroupEditForm(description = "  ", look = Look("radar"), pinnedMessage = "  Treino às 6h!  ", weeklyGoal = 12).toEdit()
        assertNull(e.description); assertEquals("radar", e.cover); assertEquals("Treino às 6h!", e.pinnedMessage); assertEquals(12, e.weeklyGoal)
        assertFailsWith<IllegalArgumentException> { GroupEditForm(pinnedMessage = "x".repeat(301)).toEdit() }
        assertFailsWith<IllegalArgumentException> { GroupEditForm(weeklyGoal = 0).toEdit() }
        // tudo vazio limpa os campos
        val clear = GroupEditForm().toEdit()
        assertNull(clear.cover); assertNull(clear.pinnedMessage); assertNull(clear.weeklyGoal)
    }

    @Test fun desafioLevaIdentidadeEPremio() {
        val c = ChallengeForm("Mês do supino", ChallengeMetric.WORKOUTS, look = Look("vulcao", "#ff3b30", "bolt"), prize = " Quem perder paga o açaí ").toNew("me", now)
        assertEquals("vulcao", c.cover); assertEquals("#FF3B30", c.accentHex); assertEquals("bolt", c.icon); assertEquals("Quem perder paga o açaí", c.prize)
        val plain = ChallengeForm("Mês do supino", ChallengeMetric.WORKOUTS).toNew("me", now)
        assertNull(plain.cover); assertNull(plain.prize)
    }

    @Test fun premioNaoPassaDe120Caracteres() {
        assertFailsWith<IllegalArgumentException> { ChallengeForm("Mês do supino", ChallengeMetric.WORKOUTS, prize = "x".repeat(121)).toNew("me", now) }
    }

    @Test fun progressoDaSemanaDoGrupo() {
        val p = GroupWeekProgress(workouts = 12, members = 8, activeMembers = 5)
        assertEquals(0.6f, p.fraction(20))
        assertNull(p.fraction(null)); assertNull(p.fraction(0))
        assertEquals(1.5f, GroupWeekProgress(workouts = 30).fraction(20))
        assertEquals("12 de 20 treinos · 5 de 8 membros ativos", p.summary(20))
        assertEquals("12 treinos · 5 de 8 membros ativos", p.summary(null))
        assertEquals("1 treino · 1 de 1 membro ativo", GroupWeekProgress(1, 1, 1).summary(null))
    }
}
