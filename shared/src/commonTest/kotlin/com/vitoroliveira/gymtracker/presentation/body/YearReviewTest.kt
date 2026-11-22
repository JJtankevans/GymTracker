package com.vitoroliveira.gymtracker.presentation.body

import com.vitoroliveira.gymtracker.data.model.Workout
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class YearReviewTest {
    private val utc = TimeZone.UTC
    private fun w(id: String, start: String, group: String?) = Workout(id = id, muscleGroupId = group, startedAt = start, finishedAt = start)
    private val names = mapOf("peito" to "Peito e Tríceps", "costas" to "Costas")

    @Test
    fun contaPorMesAcheOMelhorMesEOsGruposMaisTreinados() {
        val ws = listOf(
            w("1", "2026-03-02T10:00:00Z", "peito"), w("2", "2026-03-04T10:00:00Z", "peito"),
            w("3", "2026-03-06T10:00:00Z", "costas"), w("4", "2026-05-01T10:00:00Z", "costas"),
            w("5", "2025-12-31T10:00:00Z", "peito") // outro ano: ignorado
        )
        val r = buildYearReview(ws, names, 2026, utc)
        assertEquals(3, r.perMonth[2])
        assertEquals(1, r.perMonth[4])
        assertEquals(12, r.perMonth.size)
        assertEquals(3, r.bestMonth)
        assertEquals(listOf(GroupShare("Costas", 2), GroupShare("Peito e Tríceps", 2)), r.topGroups) // empate: ordem alfabética
    }

    @Test
    fun diaDaSemanaFavoritoESequenciaDeDiasSeguidos() {
        // 2026-11-02 é segunda; 02, 03, 04 seguidos; depois 09 (segunda) e 16 (segunda)
        val ws = listOf("2026-11-02", "2026-11-03", "2026-11-04", "2026-11-09", "2026-11-16").mapIndexed { i, d -> w("$i", "${d}T10:00:00Z", null) } +
            w("x", "2026-11-03T20:00:00Z", null) // mesmo dia: não aumenta a sequência
        val r = buildYearReview(ws, names, 2026, utc)
        assertEquals(DayOfWeek.MONDAY, r.favoriteWeekday)
        assertEquals(3, r.longestStreakDays)
        assertEquals(LocalDate(2026, 11, 4), r.longestStreakEnd)
        assertEquals("Treino livre", r.topGroups.first().name)
    }

    @Test
    fun anoSemTreinoFicaVazio() {
        val r = buildYearReview(emptyList(), names, 2026, utc)
        assertTrue(r.isEmpty)
        assertNull(r.bestMonth)
        assertNull(r.favoriteWeekday)
        assertEquals(0, r.longestStreakDays)
    }
}
