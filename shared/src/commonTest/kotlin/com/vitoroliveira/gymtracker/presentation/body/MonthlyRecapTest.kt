package com.vitoroliveira.gymtracker.presentation.body

import com.vitoroliveira.gymtracker.data.model.ExportSet
import com.vitoroliveira.gymtracker.data.model.ExportWorkout
import com.vitoroliveira.gymtracker.data.model.Workout
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MonthlyRecapTest {
    private val utc = TimeZone.UTC
    private fun w(id: String, start: String, group: String?) = Workout(id = id, muscleGroupId = group, startedAt = start, finishedAt = start)
    private fun s(wStart: String, reps: Int, kg: Double) =
        ExportSet(exerciseId = "e", setNumber = 1, reps = reps, weightKg = kg, workouts = ExportWorkout(startedAt = wStart))

    @Test
    fun contaSoOMesPedidoEAcheOGrupoMaisTreinado() {
        val workouts = listOf(
            w("1", "2026-11-03T10:00:00Z", "peito"), w("2", "2026-11-05T10:00:00Z", "peito"),
            w("3", "2026-11-05T18:00:00Z", "costas"), w("4", "2026-10-30T10:00:00Z", "costas")
        )
        val sets = listOf(
            s("2026-11-03T10:00:00Z", 10, 20.0), s("2026-11-05T10:00:00Z", 12, 10.0),
            s("2026-10-30T10:00:00Z", 99, 99.0)
        )
        val r = buildMonthlyRecap(workouts, sets, mapOf("peito" to "Peito e Tríceps", "costas" to "Costas"), 2026, 11, utc)

        assertEquals(3, r.workouts)
        assertEquals(2, r.daysTrained) // dois treinos no dia 5 contam um dia
        assertEquals(2, r.sets)
        assertEquals(22, r.reps)
        assertEquals(320.0, r.volumeKg)
        assertEquals("Peito e Tríceps", r.topGroup)
        assertEquals(2, r.topGroupWorkouts)
    }

    @Test
    fun mesSemTreinoFicaVazio() {
        val r = buildMonthlyRecap(listOf(w("1", "2026-10-30T10:00:00Z", null)), emptyList(), emptyMap(), 2026, 11, utc)
        assertTrue(r.isEmpty)
        assertEquals(null, r.topGroup)
    }

    @Test
    fun treinoSemGrupoViraTreinoLivre() {
        val r = buildMonthlyRecap(listOf(w("1", "2026-11-01T10:00:00Z", null)), emptyList(), emptyMap(), 2026, 11, utc)
        assertEquals("Treino livre", r.topGroup)
    }
}
