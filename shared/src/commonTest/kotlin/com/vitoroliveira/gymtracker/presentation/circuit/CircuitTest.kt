package com.vitoroliveira.gymtracker.presentation.circuit

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SetTimingLearnerTest {
    @Test fun semDadosUsaRitmoPadraoPorRep() {
        val l = SetTimingLearner()
        assertEquals(36, l.predictSeconds("a", 12)) // 3 s/rep
    }

    @Test fun aprendeComMediana() {
        val l = SetTimingLearner()
        repeat(5) { l.record("a", 48, 12) } // 4 s/rep
        assertEquals(48, l.predictSeconds("a", 12))
        assertEquals(32, l.predictSeconds("a", 8)) // escala por reps
    }

    @Test fun poucosDadosMisturamComPadrao() {
        val l = SetTimingLearner()
        l.record("a", 60, 12) // 5 s/rep, 1 amostra => peso 1/3
        val spr = l.secPerRep("a")
        assertTrue(spr > 3.0 && spr < 5.0, "spr=$spr")
    }

    @Test fun usaSoOsUltimos10() {
        val l = SetTimingLearner()
        repeat(10) { l.record("a", 24, 12) } // 2 s/rep
        repeat(10) { l.record("a", 48, 12) } // 4 s/rep
        assertEquals(10, l.snapshot()["a"]!!.size)
        assertEquals(48, l.predictSeconds("a", 12))
    }

    @Test fun descartaOutlierAbsurdo() {
        val l = SetTimingLearner()
        repeat(6) { l.record("a", 36, 12) }
        l.record("a", 240, 12) // parou pra atender o telefone
        assertEquals(36, l.predictSeconds("a", 12))
    }

    @Test fun ignoraTemposImpossiveis() {
        val l = SetTimingLearner()
        l.record("a", 2, 12); l.record("a", 999, 12); l.record("a", 30, 0)
        assertEquals(0, l.sampleCount("a"))
    }
}

class CircuitEngineTest {
    private val ex = CircuitPlanner.Input("e1", "Supino", sets = 2, reps = 10, weightKg = 40.0)

    private fun engine(learner: SetTimingLearner = SetTimingLearner(), steps: List<CircuitStep> =
        CircuitPlanner.build(listOf(ex), restSeconds = 60)) = CircuitEngine(steps, learner)

    @Test fun planejaSequencialComTrocaDeAparelho() {
        val s = CircuitPlanner.build(
            listOf(CircuitPlanner.Input("a", "A", 2, 10, 0.0), CircuitPlanner.Input("b", "B", 2, 10, 0.0)),
            restSeconds = 60, transitionSeconds = 30
        )
        assertEquals(listOf(60, 90, 60, 60), s.map { it.restSeconds }) // 90 = troca A→B; B é o último
        assertEquals(listOf("a", "a", "b", "b"), s.map { it.exerciseId })
    }

    @Test fun planejaRoundRobin() {
        val s = CircuitPlanner.build(
            listOf(CircuitPlanner.Input("a", "A", 2, 10, 0.0), CircuitPlanner.Input("b", "B", 2, 10, 0.0)),
            order = CircuitPlanner.Order.ROUND_ROBIN
        )
        assertEquals(listOf("a", "b", "a", "b"), s.map { it.exerciseId })
    }

    @Test fun fluxoSemToques() {
        val e = engine()
        e.start(0)
        assertEquals(CircuitPhase.SET, e.state.phase)
        assertEquals(30, e.state.predictedSetSec) // 10 reps * 3 s
        e.tick(29_000); assertEquals(CircuitPhase.SET, e.state.phase)
        val ev = e.tick(30_000)
        assertEquals(CircuitPhase.REST, e.state.phase)
        assertTrue(ev.any { it is CircuitEvent.SetAutoEnded })
        e.tick(89_000); assertEquals(CircuitPhase.REST, e.state.phase)
        val ev2 = e.tick(90_000)
        assertEquals(CircuitPhase.SET, e.state.phase)
        assertEquals(1, e.state.stepIndex)
        assertTrue(ev2.any { it is CircuitEvent.SetCommitted })
        assertTrue(ev2.any { it is CircuitEvent.SetStarted })
    }

    @Test fun terminarAntesGravaTempoRealEAprende() {
        val l = SetTimingLearner()
        val e = engine(l)
        e.start(0)
        e.tapDone(20_000)
        assertEquals(CircuitPhase.REST, e.state.phase)
        assertEquals(20, e.state.pendingDurationSec)
        e.tick(80_000)
        assertEquals(20, e.state.completed.single().durationSec)
        assertEquals(1, l.sampleCount("e1"))
    }

    @Test fun aindaNaoTerminei_somaDescansoNaSerieEReiniciaDescanso() {
        val l = SetTimingLearner()
        val e = engine(l)
        e.start(0)
        e.tick(30_000)                         // auto: previu 30 s
        e.tapStillGoing(40_000)                // 10 s depois: "ainda não terminei"
        assertEquals(40, e.state.pendingDurationSec)
        assertEquals(60_000, e.state.restLeftMs) // descanso recomeçou inteiro
        e.tick(100_000)                        // 60 s depois do reinício
        val c = e.state.completed.single()
        assertEquals(40, c.durationSec)
        assertTrue(c.corrected)
        assertEquals(40, l.snapshot()["e1"]!!.single().durationSec)
    }

    @Test fun pularDescanso() {
        val e = engine()
        e.start(0); e.tapDone(25_000)
        e.tapDone(30_000)
        assertEquals(CircuitPhase.SET, e.state.phase)
        assertEquals(1, e.state.stepIndex)
    }

    @Test fun ultimaSerieTemJanelaDeCorrecaoEDepoisTermina() {
        val e = engine()
        e.start(0); e.tapDone(20_000); e.tapDone(21_000) // série 2 começa
        val ev = e.tick(51_000)                           // auto-termina aos 30 s
        assertEquals(CircuitPhase.REST, e.state.phase)
        assertEquals(15, e.state.restTotalSec)            // janela de 15 s
        val ev2 = e.tick(66_000)
        assertEquals(CircuitPhase.DONE, e.state.phase)
        assertTrue(ev2.any { it is CircuitEvent.Finished })
        assertEquals(2, e.state.completed.size)
        assertTrue(ev.none { it is CircuitEvent.Finished })
    }

    @Test fun prever_adaptaDepoisDeAprender() {
        val l = SetTimingLearner()
        repeat(4) { l.record("e1", 50, 10) }
        val e = engine(l)
        e.start(0)
        assertEquals(50, e.state.predictedSetSec)
    }

    @Test fun estenderDescanso() {
        val e = engine()
        e.start(0); e.tapDone(20_000)
        e.extendRest(15)
        assertEquals(75, e.state.restTotalSec)
        e.tick(20_000 + 74_000); assertEquals(CircuitPhase.REST, e.state.phase)
        e.tick(20_000 + 75_000); assertEquals(CircuitPhase.SET, e.state.phase)
    }
}

class CircuitAdjustTest {
    @Test fun ajustarMudaSerieAtualEProximasDoMesmoExercicio() {
        val steps = CircuitPlanner.build(
            listOf(CircuitPlanner.Input("a", "A", 3, 10, 40.0), CircuitPlanner.Input("b", "B", 2, 10, 20.0)), restSeconds = 60
        )
        val e = CircuitEngine(steps, SetTimingLearner())
        e.start(0)
        e.adjustCurrent(weightKg = 45.0)
        assertEquals(listOf(45.0, 45.0, 45.0, 20.0, 20.0), e.state.steps.map { it.weightKg })
    }
}
