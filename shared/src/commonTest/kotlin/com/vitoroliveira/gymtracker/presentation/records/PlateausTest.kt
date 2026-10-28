package com.vitoroliveira.gymtracker.presentation.records

import com.vitoroliveira.gymtracker.data.model.Exercise
import com.vitoroliveira.gymtracker.data.model.RecentSet
import com.vitoroliveira.gymtracker.data.model.RecordSummary
import com.vitoroliveira.gymtracker.data.model.WorkoutDate
import com.vitoroliveira.gymtracker.presentation.viewmodel.FakeExerciseRepository
import com.vitoroliveira.gymtracker.presentation.viewmodel.FakeWorkoutRepository
import com.vitoroliveira.gymtracker.presentation.viewmodel.FixedClock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class PlateausTest {

    @BeforeTest fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @AfterTest fun tearDown() = Dispatchers.resetMain()

    private val today = LocalDate.parse("2026-10-20")
    private val utc = TimeZone.UTC

    private fun row(id: String, name: String, kg: Double, date: String) = RecordRow(id, name, kg, date, kg * 1.2, null, 10)
    private fun set(id: String, kg: Double, day: String) = RecentSet(id, 8, kg, WorkoutDate("${day}T18:00:00Z"))

    @Test
    fun recordeVelhoComTreinosRecentesEstaEstagnado() {
        val records = listOf(row("e1", "Supino", 80.0, "2026-08-01"))
        val sets = listOf("2026-09-20", "2026-09-27", "2026-10-04", "2026-10-11").flatMap { listOf(set("e1", 77.5, it), set("e1", 75.0, it)) }
        val p = findPlateaus(records, sets, today, utc).single()
        assertEquals("Supino", p.exerciseName)
        assertEquals(11, p.weeksSince) // 80 dias
        assertEquals(4, p.sessionsSince) // dias distintos, não séries
        assertEquals(77.5, p.topRecentKg)
        assertTrue(p.tip.contains("repetição a mais"))
    }

    @Test
    fun naoContaQuemBateuRecordeHaPoucoOuParouDeFazer() {
        val records = listOf(
            row("e1", "Supino", 80.0, "2026-10-01"), // recorde novo
            row("e2", "Terra", 140.0, "2026-06-01") // velho, mas só 2 treinos recentes
        )
        val sets = listOf(
            set("e1", 80.0, "2026-10-08"), set("e1", 80.0, "2026-10-15"), set("e1", 80.0, "2026-10-18"),
            set("e2", 130.0, "2026-10-01"), set("e2", 130.0, "2026-10-10")
        )
        assertTrue(findPlateaus(records, sets, today, utc).isEmpty())
    }

    @Test
    fun dicasPelaDistanciaDoRecordeEPeloTempo() {
        assertTrue(plateauTip(100.0, 85.0, 3).contains("deload"))
        assertTrue(plateauTip(100.0, 97.5, 6).contains("faixa de repetições"))
        assertTrue(plateauTip(100.0, 97.5, 3).contains("dupla progressão"))
    }

    @Test
    fun maisAntigosPrimeiroELimite() {
        val records = (1..7).map { row("e$it", "Ex $it", 50.0, "2026-0$it-01") }
        val sets = records.flatMap { r -> listOf("2026-10-01", "2026-10-08", "2026-10-15").map { set(r.exerciseId, 45.0, it) } }
        val out = findPlateaus(records, sets, today, utc)
        assertEquals(5, out.size)
        assertEquals("Ex 1", out.first().exerciseName)
    }

    @Test
    fun viewModelCarregaEstagnados() {
        val workouts = FakeWorkoutRepository().apply {
            recordsSummary = listOf(RecordSummary("e1", "Supino", 80.0, "2026-08-01", 96.0, 75.0, 12))
            recentSets = listOf("2026-10-01", "2026-10-08", "2026-10-15").map { set("e1", 77.5, it) }
        }
        val vm = RecordsViewModel(FakeExerciseRepository(listOf(Exercise(id = "e1", name = "Supino"))), workouts, FixedClock(Instant.parse("2026-10-20T12:00:00Z")))
        assertEquals(listOf("Supino"), vm.state.value.plateaus.map { it.exerciseName })
        assertEquals(1, vm.state.value.records.size)
    }
}
