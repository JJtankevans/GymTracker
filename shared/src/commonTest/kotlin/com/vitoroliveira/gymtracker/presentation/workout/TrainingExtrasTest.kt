package com.vitoroliveira.gymtracker.presentation.workout

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TrainingExtrasTest {
    private val sunday = LocalDate(2026, 11, 22)
    private val monday = LocalDate(2026, 11, 16)

    @Test fun `aquecimento nao conta nas estatisticas`() {
        assertFalse(countsForStats(SetFlag.WARMUP))
        assertTrue(countsForStats(SetFlag.DROP))
        assertTrue(countsForStats(null))
    }

    @Test fun `1RM ao vivo`() {
        assertEquals(80.0, liveOneRm(60.0, 10))
        assertNull(liveOneRm(0.0, 10))
        assertEquals(100.0, liveOneRm(100.0, 1))
    }

    @Test fun `alvo de repeticoes da fase`() {
        assertEquals(15, phaseTargetReps(TrainingPhase.AUTO, 15))
        assertEquals(10, phaseTargetReps(TrainingPhase.VOLUME, 10))
        assertEquals(6, phaseTargetReps(TrainingPhase.STRENGTH, 10))
        assertEquals(15, phaseTargetReps(TrainingPhase.CUT, 8))
    }

    @Test fun `dias restantes na semana`() {
        assertEquals(1, daysLeftInWeek(sunday))
        assertEquals(7, daysLeftInWeek(monday))
    }

    @Test fun `lembrete inteligente`() {
        assertNull(smartReminder(4, 4, false, 1, "Pernas"))
        assertNull(smartReminder(2, 4, true, 1, "Pernas"))
        assertTrue(smartReminder(2, 4, false, 1, "Pernas")!!.contains("só restam 1 dia:"))
        assertTrue(smartReminder(2, 4, false, 7, null)!!.contains("Que tal hoje"))
        assertTrue(smartReminder(3, 4, false, 1, null)!!.contains("todos os dias"))
    }

    @Test fun `cardio da semana e ida e volta da codificacao`() {
        val e = listOf(
            CardioEntry("1", LocalDate(2026, 11, 21), "Corrida", 30, 4.5),
            CardioEntry("2", LocalDate(2026, 11, 22), "Bike", 20),
            CardioEntry("3", LocalDate(2026, 11, 10), "Corrida", 60, 9.0)
        )
        assertEquals(CardioWeek(50, 4.5, 2), weeklyCardio(e, sunday))
        assertEquals(e, decodeCardio(encodeCardio(e)))
    }

    @Test fun `timer ao vivo monta as fases e acha a atual`() {
        val s = liveSchedule(listOf(3, 1, 2, 0), 2)
        assertEquals(listOf("Descendo", "Pausa embaixo", "Subindo", "Descendo", "Pausa embaixo", "Subindo"), s.map { it.label })
        assertEquals(12, scheduleSeconds(s))
        val one = liveSchedule(listOf(3, 1, 2, 0), 1)
        assertEquals("Descendo", phaseAt(one, 0.0)!!.phase.label)
        assertEquals(3, phaseAt(one, 0.0)!!.left)
        assertEquals("Pausa embaixo", phaseAt(one, 3.5)!!.phase.label)
        assertEquals(2, phaseAt(one, 4.5)!!.left)
        assertNull(phaseAt(one, 6.0))
        assertNotNull(phaseAt(one, 5.9))
    }
}
