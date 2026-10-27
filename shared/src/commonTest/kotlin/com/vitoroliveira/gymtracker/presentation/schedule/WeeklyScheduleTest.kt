package com.vitoroliveira.gymtracker.presentation.schedule

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class WeeklyScheduleTest {
    @Test fun defineETiraDiasEGuardaNoAparelho() {
        val store = InMemoryScheduleStore()
        val repo = ScheduleRepository(store)
        repo.set(1, "peito"); repo.set(3, "pernas"); repo.set(1, null)
        assertEquals(mapOf(3 to "pernas"), repo.plan.value)
        assertEquals(mapOf(3 to "pernas"), store.load())
    }

    @Test fun planejadoSoValeSeOGrupoAindaExiste() {
        val plan = mapOf(1 to "peito", 2 to "apagado")
        assertEquals("peito", plannedGroupFor(plan, 1, listOf("peito", "costas")))
        assertNull(plannedGroupFor(plan, 2, listOf("peito", "costas")))
        assertNull(plannedGroupFor(plan, 5, listOf("peito")))
    }

    @Test fun diaForaDaSemanaEhRecusado() {
        assertFailsWith<IllegalArgumentException> { ScheduleRepository(InMemoryScheduleStore()).set(8, "x") }
    }

    @Test fun ignoraDiasInvalidosGuardados() {
        assertEquals(mapOf(2 to "a"), ScheduleRepository(InMemoryScheduleStore(mapOf(0 to "x", 2 to "a", 9 to "y"))).plan.value)
    }
}
