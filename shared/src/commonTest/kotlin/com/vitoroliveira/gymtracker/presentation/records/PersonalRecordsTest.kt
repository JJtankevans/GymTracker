package com.vitoroliveira.gymtracker.presentation.records

import com.vitoroliveira.gymtracker.data.model.Exercise
import com.vitoroliveira.gymtracker.data.model.ExerciseProgression
import com.vitoroliveira.gymtracker.data.model.RecordSummary
import com.vitoroliveira.gymtracker.presentation.viewmodel.FakeExerciseRepository
import com.vitoroliveira.gymtracker.presentation.viewmodel.FakeWorkoutRepository
import com.vitoroliveira.gymtracker.presentation.viewmodel.FixedClock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

private fun p(date: String, kg: Double, reps: Int = 10, sets: Int = 2, id: String = "e1") =
    ExerciseProgression(id, "Supino", "u", date, kg, reps, sets)

@OptIn(ExperimentalCoroutinesApi::class)
class PersonalRecordsTest {

    @BeforeTest fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @AfterTest fun tearDown() = Dispatchers.resetMain()

    @Test fun semCargaNaoViraRecorde() {
        assertNull(buildRecordRow("e1", "Flexão", listOf(p("2026-10-01", 0.0))))
        assertNull(buildRecordRow("e1", "Flexão", emptyList()))
    }

    @Test fun pegaMaiorCargaEOPrimeiroDiaQueChegouNela() {
        val r = buildRecordRow("e1", "Supino", listOf(p("2026-10-10", 80.0), p("2026-10-01", 60.0), p("2026-10-20", 80.0)))!!
        assertEquals(80.0, r.bestWeightKg)
        assertEquals("2026-10-10", r.bestDate)
        assertEquals(60.0, r.previousBestKg)
        assertEquals(20.0, r.improvementKg)
        assertEquals(3, r.sessions)
    }

    @Test fun primeiraSessaoNaoTemRecordeAnterior() {
        val r = buildRecordRow("e1", "Supino", listOf(p("2026-10-01", 50.0)))!!
        assertNull(r.previousBestKg)
        assertNull(r.improvementKg)
    }

    @Test fun cargaQueCaiuDepoisNaoMudaORecorde() {
        val r = buildRecordRow("e1", "Supino", listOf(p("2026-09-01", 100.0), p("2026-10-01", 70.0)))!!
        assertEquals(100.0, r.bestWeightKg)
        assertNull(r.previousBestKg)
    }

    @Test fun umRmEstimadoNuncaEMenorQueACarga() {
        val r = buildRecordRow("e1", "Supino", listOf(p("2026-10-01", 80.0, reps = 16, sets = 2)))!!
        assertTrue(r.bestOneRmKg >= 80.0)
    }

    @Test fun recenteConsideraJanelaDe30Dias() {
        val r = buildRecordRow("e1", "Supino", listOf(p("2026-10-01", 80.0)))!!
        assertTrue(r.isRecent(LocalDate(2026, 10, 20)))
        assertFalse(r.isRecent(LocalDate(2026, 12, 1)))
        assertFalse(r.isRecent(LocalDate(2026, 9, 1))) // data no futuro não é "recente"
    }

    @Test fun ordenaDoMaisNovoParaOMaisAntigo() {
        val a = buildRecordRow("a", "A", listOf(p("2026-10-01", 50.0)))!!
        val b = buildRecordRow("b", "B", listOf(p("2026-10-15", 40.0)))!!
        assertEquals(listOf("b", "a"), listOf(a, b).sortedForDisplay().map { it.exerciseId })
    }

    @Test fun viewModelMontaListaEIgnoraExerciciosSemCarga() {
        val exercises = FakeExerciseRepository(
            listOf(Exercise(id = "e1", name = "Supino"), Exercise(id = "e2", name = "Flexão"))
        )
        val workouts = FakeWorkoutRepository().apply {
            progression = mapOf(
                "e1" to listOf(p("2026-10-01", 60.0), p("2026-10-18", 70.0)),
                "e2" to listOf(p("2026-10-18", 0.0, id = "e2"))
            )
        }
        val vm = RecordsViewModel(exercises, workouts, FixedClock(Instant.parse("2026-10-20T12:00:00Z")))
        val s = vm.state.value
        assertEquals(listOf("Supino"), s.records.map { it.exerciseName })
        assertEquals(70.0, s.records.single().bestWeightKg)
        assertEquals(1, s.recentCount)
        assertEquals(1, workouts.summaryCalls) // tentou o caminho rápido e caiu no antigo
    }

    @Test fun usaOResumoDoServidorEmUmaChamadaSoSemConsultarCadaExercicio() {
        val exercises = FakeExerciseRepository(listOf(Exercise(id = "e1", name = "Supino"), Exercise(id = "e2", name = "Terra")))
        val workouts = FakeWorkoutRepository().apply {
            recordsSummary = listOf(
                RecordSummary("e1", "Supino", 70.0, "2026-10-18", 93.3, 60.0, 2),
                RecordSummary("e2", "Terra", 100.0, "2026-10-05", 120.0, null, 3)
            )
        }
        val vm = RecordsViewModel(exercises, workouts, FixedClock(Instant.parse("2026-10-20T12:00:00Z")))
        val s = vm.state.value

        assertEquals(1, workouts.summaryCalls)
        assertEquals(0, workouts.progressionCalls) // nada de uma consulta por exercício
        assertEquals(listOf("Supino", "Terra"), s.records.map { it.exerciseName })
        assertEquals(10.0, s.records.first().improvementKg)
        assertNull(s.records.last().previousBestKg)
        assertEquals(2, s.recentCount) // 10/18 e 10/05 estão a menos de 30 dias de 10/20
        assertNull(s.errorMessage)
    }

    @Test fun semNenhumRecordeMostraListaVazia() {
        val workouts = FakeWorkoutRepository().apply { recordsSummary = emptyList() }
        val vm = RecordsViewModel(FakeExerciseRepository(), workouts, FixedClock(Instant.parse("2026-10-20T12:00:00Z")))
        assertTrue(vm.state.value.records.isEmpty())
        assertNull(vm.state.value.errorMessage)
    }

    @Test fun caminhoAntigoTambemFalhandoMostraErro() {
        val workouts = FakeWorkoutRepository().apply { failWith = null }
        val broken = object : com.vitoroliveira.gymtracker.domain.repository.ExerciseRepository by FakeExerciseRepository() {
            override suspend fun getAll(): List<Exercise> = throw RuntimeException("Unable to resolve host")
        }
        val vm = RecordsViewModel(broken, workouts, FixedClock(Instant.parse("2026-10-20T12:00:00Z")))
        assertEquals("Sem conexão com a internet. Verifique sua rede e tente de novo.", vm.state.value.errorMessage)
    }
}
