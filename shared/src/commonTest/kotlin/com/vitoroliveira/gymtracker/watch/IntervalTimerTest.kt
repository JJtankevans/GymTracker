package com.vitoroliveira.gymtracker.watch

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IntervalTimerTest {

    private val tabata = IntervalPlan(workSec = 20, restSec = 10, rounds = 8, prepSec = 5)

    @Test
    fun totalSemPausaDepoisDaUltimaRodada() {
        // 5 + 8×20 + 7×10 = 235 s
        assertEquals(235_000L, tabata.totalMs)
        assertEquals(5_000L + 10 * 60_000L, IntervalPlan(workSec = 60, restSec = 0, rounds = 10).totalMs)
    }

    @Test
    fun fasesAoLongoDoTempo() {
        assertEquals(IntervalState(IntervalPhase.PREP, 1, 5_000, 5_000), tabata.stateAt(0))
        assertEquals(IntervalState(IntervalPhase.WORK, 1, 20_000, 20_000), tabata.stateAt(5_000))
        assertEquals(IntervalState(IntervalPhase.REST, 1, 10_000, 10_000), tabata.stateAt(25_000))
        assertEquals(IntervalState(IntervalPhase.WORK, 2, 15_000, 20_000), tabata.stateAt(40_000))
        // última rodada: começa em 5 + 7×30 = 215 s e vai até 235 s
        assertEquals(IntervalState(IntervalPhase.WORK, 8, 1_000, 20_000), tabata.stateAt(234_000))
        assertEquals(IntervalPhase.DONE, tabata.stateAt(235_000).phase)
        assertEquals(IntervalPhase.DONE, tabata.stateAt(999_999).phase)
    }

    @Test
    fun semPausaPulaDeEsforcoParaEsforco() {
        val emom = IntervalPlan(workSec = 60, restSec = 0, rounds = 3, prepSec = 0)
        assertEquals(IntervalState(IntervalPhase.WORK, 1, 60_000, 60_000), emom.stateAt(0))
        assertEquals(IntervalState(IntervalPhase.WORK, 2, 60_000, 60_000), emom.stateAt(60_000))
        assertEquals(IntervalPhase.DONE, emom.stateAt(180_000).phase)
    }

    @Test
    fun normalizaLimitesERotulo() {
        val p = IntervalPlan(workSec = 1, restSec = -5, rounds = 0, prepSec = 99).normalized()
        assertEquals(IntervalPlan(workSec = 5, restSec = 0, rounds = 1, prepSec = 60), p)
        assertEquals("5s × 1", p.label)
        assertEquals("20s / 10s × 8", tabata.label)
    }

    @Test
    fun trocaDeFase() {
        val a = tabata.stateAt(6_000)
        assertTrue(a.samePhaseAs(tabata.stateAt(24_999)))
        assertFalse(a.samePhaseAs(tabata.stateAt(25_000)))
        assertFalse(a.samePhaseAs(null))
        assertEquals(0.5f, tabata.stateAt(15_000).fraction)
    }
}
