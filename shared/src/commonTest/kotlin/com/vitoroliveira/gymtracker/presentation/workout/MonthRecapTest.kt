package com.vitoroliveira.gymtracker.presentation.workout

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MonthRecapTest {
    private fun s(m: Int, d: Int, min: Long?, g: String) = RecapSession(LocalDate(2026, m, d), min, g)

    @Test
    fun comparaComOMesAnteriorEAchaOGrupoMaisTreinado() {
        val r = monthRecap(
            listOf(
                s(10, 2, 60, "Peito"), s(10, 2, 30, "Costas"), s(10, 9, 50, "Peito"), s(10, 20, null, "Sem grupo"),
                s(10, 21, 80, "Sem grupo"), s(10, 22, 40, "Sem grupo"),
                s(9, 5, 45, "Pernas"), s(9, 30, 45, "Pernas"),
                s(8, 1, 999, "Peito")                                  // fora dos dois meses
            ),
            LocalDate(2026, 10, 28)
        )
        assertEquals("outubro", r.monthName)
        assertEquals("setembro", r.previousMonthName)
        assertEquals(6, r.workouts)
        assertEquals(2, r.previousWorkouts)
        assertEquals(4, r.workoutsDelta)
        assertEquals(260L, r.minutes)
        assertEquals(90L, r.previousMinutes)
        assertEquals(5, r.activeDays)
        assertEquals("Peito", r.topGroup)                              // "Sem grupo" não conta, mesmo aparecendo 3 vezes
        assertEquals(2, r.topGroupCount)
        assertEquals(80L, r.longestMinutes)
    }

    @Test
    fun janeiroComparaComDezembroDoAnoAnterior() {
        val r = monthRecap(listOf(RecapSession(LocalDate(2025, 12, 31), 30, "A")), LocalDate(2026, 1, 3))
        assertEquals("janeiro", r.monthName)
        assertEquals("dezembro", r.previousMonthName)
        assertEquals(0, r.workouts)
        assertEquals(1, r.previousWorkouts)
        assertNull(r.topGroup)
        assertNull(r.longestMinutes)
    }

    @Test
    fun formataMinutos() {
        assertEquals("45 min", formatMinutes(45))
        assertEquals("2 h", formatMinutes(120))
        assertEquals("9 h 20 min", formatMinutes(560))
    }
}
