package com.vitoroliveira.gymtracker.presentation.ranked

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RankedRulesTest {
    private fun d(day: Int) = LocalDate(2026, 11, day)

    // ---- elos ----
    @Test fun elosEDivisoes() {
        assertEquals("Ferro III", rankFor(0).label)
        assertEquals("Ferro III", rankFor(99).label)
        assertEquals("Ferro II", rankFor(100).label)
        assertEquals("Ferro I", rankFor(299).label)
        assertEquals("Bronze III", rankFor(300).label)
        assertEquals("Ouro II", rankFor(3 * 300 + 150).label)
        assertEquals("Diamante I", rankFor(6 * 300 - 1).label)
        assertEquals("Lenda", rankFor(1800).label)
        assertEquals("Lenda", rankFor(5000).label)
        assertEquals("Ferro III", rankFor(-50).label)
    }

    @Test fun progressoDentroDaDivisao() {
        val r = rankFor(3 * 300 + 150) // Ouro II com 50 de 100
        assertEquals(50, r.rpInDivision); assertEquals(0.5f, r.progress); assertEquals(50, r.rpToNext)
        val top = rankFor(1900)
        assertEquals(100, top.rpInDivision); assertEquals(1f, top.progress); assertEquals(0, top.rpToNext)
    }

    // ---- pontos ----
    @Test fun pontosDeUmTreino() {
        assertEquals(0, workoutPoints(19.9, 10))                     // curto demais
        assertEquals(0, workoutPoints(241.0, 10))                    // esquecido aberto
        assertEquals(100 + 0 + 2 * 10, workoutPoints(20.0, 10))
        assertEquals(100 + 15 + 2 * 12, workoutPoints(50.0, 12))     // (50-20)/2 = 15
        assertEquals(100 + 30 + 2 * 20, workoutPoints(80.0, 25))     // esforço e séries no teto
        assertEquals(100 + 30 + 2 * 20, workoutPoints(200.0, 99))
        assertEquals(100, workoutPoints(20.0, 0))
        assertEquals(100 + 0, workoutPoints(21.9, 0))                // meio ponto de esforço arredonda para baixo
    }

    @Test fun semanaSemTreinoValeZero() = assertEquals(0, weekPoints(emptyList(), 3))

    @Test fun soOMelhorTreinoDoDiaConta() {
        val w = listOf(RankedWorkout(d(2), 30.0, 10), RankedWorkout(d(2), 60.0, 15))
        assertEquals(workoutPoints(60.0, 15), weekPoints(w, 0))
    }

    @Test fun bonusDeConstanciaComTresDias() {
        val ws = (2..4).map { RankedWorkout(d(it), 20.0, 0) } // 3 dias de 100
        assertEquals(300 + CONSISTENCY_BONUS, weekPoints(ws, 0))
        assertEquals(200, weekPoints(ws.take(2), 0))
    }

    @Test fun bonusDaMetaSemanal() {
        val ws = (2..3).map { RankedWorkout(d(it), 20.0, 0) }
        assertEquals(200 + GOAL_BONUS, weekPoints(ws, 2))
        assertEquals(200, weekPoints(ws, 3))
        assertEquals(200, weekPoints(ws, 0)) // sem meta não ganha
    }

    @Test fun maximoDeSeisDias() {
        val ws = (2..8).map { RankedWorkout(d(it), 20.0, 0) } // 7 dias
        assertEquals(6 * 100 + CONSISTENCY_BONUS + GOAL_BONUS, weekPoints(ws, 3))
    }

    @Test fun treinosInvalidosNaoContamNemComoDia() {
        val ws = listOf(RankedWorkout(d(2), 10.0, 20), RankedWorkout(d(3), 20.0, 0), RankedWorkout(d(4), 300.0, 20))
        assertEquals(100, weekPoints(ws, 0))
    }

    // ---- fechamento da semana ----
    @Test fun faixasNumaTurmaDeTrinta() {
        assertEquals(6, promoteCount(30)); assertEquals(6, demoteCount(30))
        assertEquals(Zone.PROMOTE, zoneFor(6, 30, 500)); assertEquals(Zone.SAFE, zoneFor(7, 30, 500))
        assertEquals(Zone.SAFE, zoneFor(24, 30, 500)); assertEquals(Zone.DEMOTE, zoneFor(25, 30, 500))
        assertEquals(Zone.DEMOTE, zoneFor(1, 30, 0)) // zero pontos nunca sobe
    }

    @Test fun variacaoDeRpNumaTurmaDeTrinta() {
        assertEquals(40, settleDelta(1, 30, 600))
        (2..6).forEach { assertEquals(30, settleDelta(it, 30, 400), "posição $it") }
        (7..15).forEach { assertEquals(10, settleDelta(it, 30, 300), "posição $it") }
        (16..24).forEach { assertEquals(-10, settleDelta(it, 30, 200), "posição $it") }
        (25..30).forEach { assertEquals(-25, settleDelta(it, 30, 100), "posição $it") }
        assertEquals(-35, settleDelta(3, 30, 0))
    }

    @Test fun turmaPequenaSoOlhaSeTreinou() {
        assertEquals(10, settleDelta(1, 4, 250)); assertEquals(10, settleDelta(4, 4, 100))
        assertEquals(-20, settleDelta(2, 4, 60)); assertEquals(-35, settleDelta(2, 4, 0))
        assertEquals(0, promoteCount(4)); assertEquals(Zone.SAFE, zoneFor(1, 4, 100))
    }

    @Test fun turmaDeCinco() {
        assertEquals(listOf(40, 10, 10, -10, -25), (1..5).map { settleDelta(it, 5, 100) })
    }

    @Test fun posicoesComEmpate() {
        assertEquals(listOf(1, 1, 3, 4), positions(listOf(300, 300, 200, 100)))
        assertEquals(listOf(4, 1, 2, 2), positions(listOf(10, 90, 50, 50)))
    }

    // ---- x1 ----
    @Test fun duelaEntreIguaisVale12() {
        assertEquals(12, duelDelta(500, 500, 300, 200)); assertEquals(-12, duelDelta(500, 500, 200, 300))
        assertEquals(0, duelDelta(500, 500, 250, 250))
    }

    @Test fun zebraRendeMaisQueFavorito() {
        val zebra = duelDelta(300, 700, 300, 200)   // quem tem menos RP vence
        val favorito = duelDelta(700, 300, 300, 200)
        assertTrue(zebra > favorito, "zebra=$zebra favorito=$favorito")
        assertEquals(22, zebra); assertEquals(2, favorito)
        assertEquals(-22, duelDelta(700, 300, 200, 300))   // o favorito que perde cai o mesmo tanto
    }

    @Test fun deltaDoDuelFicaNoTeto() {
        assertTrue(duelDelta(0, 5000, 300, 200) <= DUEL_MAX_DELTA)
        assertTrue(duelDelta(5000, 0, 200, 300) >= -DUEL_MAX_DELTA)
    }

    @Test fun amistosoEDuelVazioNaoMexemEmRp() {
        assertEquals(0, duelDelta(500, 500, 300, 200, ranked = false))
        assertEquals(0, duelDelta(500, 500, 0, 0))
    }

    @Test fun soDuelaNoMesmoElo() {
        assertTrue(canDuel(310, 590)); assertFalse(canDuel(290, 310)); assertTrue(canDuel(1800, 4000))
    }

    // ---- temporadas ----
    @Test fun temporadasDeOitoSemanas() {
        assertEquals(SeasonInfo(1, 1), seasonOf(LocalDate(2026, 10, 26)))
        assertEquals(SeasonInfo(1, 8), seasonOf(LocalDate(2026, 12, 14)))
        assertTrue(seasonOf(LocalDate(2026, 12, 14)).isLastWeek)
        assertEquals(SeasonInfo(2, 1), seasonOf(LocalDate(2026, 12, 21)))
        assertEquals(SeasonInfo(1, 1), seasonOf(LocalDate(2026, 9, 1))) // antes do início: trava na primeira
    }

    @Test fun resetSuaveDeFimDeTemporada() {
        assertEquals(0, softReset(0)); assertEquals(60, softReset(100)); assertEquals(1080, softReset(1800)); assertEquals(0, softReset(-5))
    }

    @Test fun segundaDaSemana() {
        assertEquals(LocalDate(2026, 10, 26), mondayOf(LocalDate(2026, 11, 1))) // domingo
        assertEquals(LocalDate(2026, 11, 2), mondayOf(LocalDate(2026, 11, 2)))
        assertEquals(LocalDate(2026, 11, 2), mondayOf(LocalDate(2026, 11, 5)))
    }
}
