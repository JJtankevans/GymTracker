package com.vitoroliveira.gymtracker.presentation.workout

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TrainingCalendarTest {
    // 29/10/2026 é uma quinta-feira
    private val today = LocalDate(2026, 10, 29)

    @Test fun gradeComecaNaSegundaETerminaHoje() {
        val cal = buildTrainingCalendar(emptyList(), today, weeks = 3)
        assertEquals(3, cal.weeks.size)
        assertEquals(LocalDate(2026, 10, 12), cal.weeks[0][0]?.date)   // segunda de 2 semanas atrás
        assertEquals(today, cal.weeks[2][3]?.date)                    // quinta = índice 3
        assertNull(cal.weeks[2][4])                                   // sexta ainda não chegou
        assertEquals(0, cal.activeDays)
    }

    @Test fun contaTreinosPorDiaEIgnoraForaDaJanela() {
        val days = listOf(
            LocalDate(2026, 10, 27), LocalDate(2026, 10, 27), LocalDate(2026, 10, 29),
            LocalDate(2026, 1, 5) // fora das 3 semanas
        )
        val cal = buildTrainingCalendar(days, today, weeks = 3)
        assertEquals(2, cal.weeks[2][1]?.workouts)
        assertEquals(1, cal.weeks[2][3]?.workouts)
        assertEquals(2, cal.activeDays)
        assertEquals(3, cal.totalWorkouts)
    }

    @Test fun rotuloDoMesNaSemanaDoDiaPrimeiro() {
        val cal = buildTrainingCalendar(emptyList(), LocalDate(2026, 11, 4), weeks = 3)
        // semanas: 19/10, 26/10 (tem o 1/11), 2/11
        assertEquals(listOf("out", "nov", null), cal.monthLabels)
    }
}
