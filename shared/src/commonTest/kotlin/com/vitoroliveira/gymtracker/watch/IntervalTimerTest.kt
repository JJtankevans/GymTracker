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

class WatchRecordsTest {
    @Test
    fun recordesSemRepetirDoMaisPesado() {
        val supino = WatchExercise("e1", "Supino", bestKg = 80.0)
        val r = WatchRoutines(
            routines = listOf(
                WatchRoutine("g1", "Peito", exercises = listOf(supino, WatchExercise("e2", "Crucifixo", bestKg = 16.0), WatchExercise("e3", "Flexão"))),
                WatchRoutine(null, "Todos", exercises = listOf(supino, WatchExercise("e4", "Terra", bestKg = 140.0)))
            )
        )
        assertEquals(listOf("Terra", "Supino", "Crucifixo"), r.records().map { it.name })
    }
}

class HeartZonesTest {
    @Test
    fun fcMaximaPelaIdade() {
        assertEquals(187, estimatedMaxHr(30)) // 208 - 21
        assertEquals(DEFAULT_MAX_HR, estimatedMaxHr(null))
        assertEquals(DEFAULT_MAX_HR, estimatedMaxHr(3))
    }

    @Test
    fun zonasPorPorcentagem() {
        assertEquals(HrZone.REST, HrZone.of(90, 200))
        assertEquals(HrZone.Z1, HrZone.of(100, 200))
        assertEquals(HrZone.Z2, HrZone.of(139, 200))
        assertEquals(HrZone.Z3, HrZone.of(140, 200))
        assertEquals(HrZone.Z4, HrZone.of(170, 200))
        assertEquals(HrZone.Z5, HrZone.of(185, 200))
        assertEquals(HrZone.Z5, HrZone.of(230, 200))
    }

    @Test
    fun sessaoContaAmostrasPorZona() {
        var s = WatchSession.quick("q", 10, 20.0, 0L)
        listOf(120, 125, 150, 155, 158, 20).forEach { s = s.withHeartRate(it, 200) } // 20 é descartado
        assertEquals(listOf(0, 0, 2, 3, 0, 0), s.hrZones)
        assertEquals(HrZone.Z3, s.hrZones.dominantZone())
        assertEquals(null, emptyList<Int>().dominantZone())
        assertEquals(listOf(0, 1, 0, 0, 0, 0), emptyList<Int>().addZoneSample(HrZone.Z1))
    }
}
