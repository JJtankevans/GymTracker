package com.vitoroliveira.gymtracker.presentation.social

import com.vitoroliveira.gymtracker.data.model.Standing
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ChallengeMessagesTest {
    private fun s(id: String, score: Double) = Standing(profileId = id, username = id, score = score)

    private val ativo = ChallengeStatus.ACTIVE

    @Test fun asFrasesDoThiagoEstaoTodasNaBiblioteca() {
        val todas = ChallengeMessages.behind + ChallengeMessages.failed
        listOf(
            "Você foi moggado!", "Faltou suor, sobrou desculpa.", "Faltou foco, sobrou mico.",
            "Ficar pra trás dói, né? Amanhã tem mais.", "Catabolizou só de olhar o gráfico dos seus amigos.",
            "-10000 de Aura. A meta te amassou.", "Sem aura, sem meta, sem respeito no ranking.",
            "Alpha de verdade bate a meta. Betinha arruma desculpa.", "Perdeu aura, levou mogg e virou o betinha do grupo."
        ).forEach { assertTrue(it in todas, "faltou: $it") }
        assertEquals(9, todas.size)
    }

    @Test fun quemEstaNaFrenteLideraSeTemPontos() {
        assertEquals(ChallengeMood.LEADING, challengeMood(ativo, null, listOf(s("eu", 5.0), s("b", 3.0), s("c", 1.0)), "eu"))
    }

    @Test fun empateNoTopoContaComoLideranca() {
        assertEquals(ChallengeMood.LEADING, challengeMood(ativo, null, listOf(s("eu", 5.0), s("b", 5.0)), "eu"))
    }

    @Test fun ninguemTreinouNaoZoaNemCelebra() {
        assertNull(challengeMood(ativo, null, listOf(s("eu", 0.0), s("b", 0.0)), "eu"))
    }

    @Test fun sozinhoNoDesafioSemMetaNaoTemMensagem() {
        assertNull(challengeMood(ativo, null, listOf(s("eu", 4.0)), "eu"))
    }

    @Test fun meioDoPlacarEstaFicandoParaTras() {
        assertEquals(ChallengeMood.BEHIND, challengeMood(ativo, null, listOf(s("a", 9.0), s("eu", 5.0), s("c", 1.0)), "eu"))
    }

    @Test fun ultimoColocadoFalhou() {
        assertEquals(ChallengeMood.FAILED, challengeMood(ativo, null, listOf(s("a", 9.0), s("b", 5.0), s("eu", 1.0)), "eu"))
    }

    @Test fun bateuAMetaVenceTudo() {
        assertEquals(ChallengeMood.GOAL_REACHED, challengeMood(ativo, 10.0, listOf(s("a", 30.0), s("eu", 12.0)), "eu"))
    }

    @Test fun desafioEncerradoSemBaterAMetaFalhou() {
        assertEquals(ChallengeMood.FAILED, challengeMood(ChallengeStatus.ENDED, 10.0, listOf(s("eu", 8.0), s("b", 2.0)), "eu"))
    }

    @Test fun metaNaoBatidaAindaNoPrazoUsaOPlacar() {
        assertEquals(ChallengeMood.LEADING, challengeMood(ativo, 100.0, listOf(s("eu", 8.0), s("b", 2.0)), "eu"))
    }

    @Test fun naoMostraAntesDeComecarNemParaQuemNaoParticipa() {
        assertNull(challengeMood(ChallengeStatus.UPCOMING, null, listOf(s("eu", 1.0), s("b", 2.0)), "eu"))
        assertNull(challengeMood(ativo, null, listOf(s("a", 1.0), s("b", 2.0)), "eu"))
        assertNull(challengeMood(ativo, null, null, "eu"))
    }

    @Test fun mesmaSementeDaMesmaFraseEOLayoutCombinaComOClima() {
        val a = ChallengeMessages.pick(ChallengeMood.BEHIND, 123)
        assertEquals(a, ChallengeMessages.pick(ChallengeMood.BEHIND, 123))
        assertEquals(MessageLayout.ALERT, a.layout)
        assertEquals(MessageLayout.STAMP, ChallengeMessages.pick(ChallengeMood.FAILED, 1).layout)
        assertEquals(MessageLayout.BANNER, ChallengeMessages.pick(ChallengeMood.LEADING, 1).layout)
    }

    @Test fun sementeNegativaOuGrandeNaoEstouraOIndice() {
        listOf(Int.MIN_VALUE, -1, 0, Int.MAX_VALUE).forEach { seed ->
            ChallengeMood.entries.forEach { mood ->
                assertTrue(ChallengeMessages.pick(mood, seed).text in ChallengeMessages.pool(mood))
            }
        }
    }

    @Test fun diasDiferentesVariamAFrase() {
        val frases = (0 until 5).map { ChallengeMessages.pick(ChallengeMood.BEHIND, it).text }.toSet()
        assertEquals(ChallengeMessages.behind.size, frases.size)
    }
}

class ProfileTitlesTest {
    private fun honor(level: Int) = com.vitoroliveira.gymtracker.data.model.ProfileTitle("p1", "honor", level = level, challengeId = "c1")
    private fun fail() = com.vitoroliveira.gymtracker.data.model.ProfileTitle("p1", "fail", challengeId = "c1", failures = 3)

    @Test fun escadaDeHonraTemOsQuatroNomesEOMaximoEMrOlimpia() {
        assertEquals(listOf("Alpha", "Chad", "Giga Chad", "Mr. Olimpia"), (1..4).map { ProfileTitles.text(honor(it)) })
        assertTrue(ProfileTitles.isMax(honor(4)))
        assertTrue(!ProfileTitles.isMax(honor(3)))
    }

    @Test fun nivelForaDaFaixaNaoEstoura() {
        assertEquals("Alpha", ProfileTitles.text(honor(0)))
        assertEquals("Mr. Olimpia", ProfileTitles.text(honor(9)))
    }

    @Test fun tituloDeFalhaUsaUmaFraseDoThiagoEFicaFixo() {
        val frase = ProfileTitles.text(fail())
        assertTrue(frase in ChallengeMessages.behind + ChallengeMessages.failed)
        assertEquals(frase, ProfileTitles.text(fail()))
    }

    @Test fun escadaParaOCriadorMostraOsMultiplos() {
        assertEquals("Alpha 7 dias · Chad 14 · Giga Chad 21 · Mr. Olimpia 28", ProfileTitles.ladder(7))
    }

    private val agora = kotlin.time.Instant.parse("2026-10-21T12:00:00Z")

    @Test fun formularioAceitaTitulosEPassaParaOBanco() {
        val n = ChallengeForm(title = "Mês de ferro", metric = ChallengeMetric.WORKOUTS, days = 30, minMinutesPerWorkout = 20, failThreshold = 3, streakGoal = 7).toNew("me", agora)
        assertEquals(3, n.failThreshold)
        assertEquals(7, n.streakGoal)
    }

    @Test fun semTitulosOsCamposFicamNulos() {
        val n = ChallengeForm(title = "Mês de ferro", metric = ChallengeMetric.WORKOUTS, days = 30).toNew("me", agora)
        assertNull(n.failThreshold)
        assertNull(n.streakGoal)
    }

    @Test fun validaRegrasDosTitulos() {
        fun erro(f: ChallengeForm) = runCatching { f.toNew("me", agora) }.exceptionOrNull()?.message
        val base = ChallengeForm(title = "Mês de ferro", metric = ChallengeMetric.WORKOUTS, days = 10, minMinutesPerWorkout = 20)
        assertTrue(erro(base.copy(failThreshold = 0))!!.contains("entre 1 e 100"))
        assertTrue(erro(base.copy(failThreshold = 2, minMinutesPerWorkout = 0))!!.contains("mínimo de minutos"))
        assertTrue(erro(base.copy(streakGoal = 0))!!.contains("entre 1 e 365"))
        assertTrue(erro(base.copy(streakGoal = 11))!!.contains("duração"))
        assertNull(erro(base.copy(streakGoal = 10)))
    }
}
