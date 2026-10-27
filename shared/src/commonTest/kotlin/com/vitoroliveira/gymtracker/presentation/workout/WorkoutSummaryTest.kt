package com.vitoroliveira.gymtracker.presentation.workout

import com.vitoroliveira.gymtracker.data.model.Exercise
import com.vitoroliveira.gymtracker.data.model.Workout
import com.vitoroliveira.gymtracker.data.model.WorkoutSet
import com.vitoroliveira.gymtracker.presentation.viewmodel.ExerciseSetGroup
import com.vitoroliveira.gymtracker.presentation.viewmodel.FakeExerciseRepository
import com.vitoroliveira.gymtracker.presentation.viewmodel.FakeWorkoutRepository
import com.vitoroliveira.gymtracker.presentation.viewmodel.WorkoutDetailViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.TimeZone
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class WorkoutSummaryTest {
    @BeforeTest fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @AfterTest fun tearDown() = Dispatchers.resetMain()

    private val utc = TimeZone.UTC
    private fun set(n: Int, reps: Int, kg: Double, ex: String = "e1") = WorkoutSet(workoutId = "w1", exerciseId = ex, setNumber = n, reps = reps, weightKg = kg)
    private val workout = Workout(id = "w1", startedAt = "2026-10-26T10:00:00Z", finishedAt = "2026-10-26T11:02:00Z")
    private val groups = listOf(
        ExerciseSetGroup("e1", "Cadeira Extensora", listOf(set(1, 12, 40.0), set(2, 10, 42.5))),
        ExerciseSetGroup("e2", "Prancha", listOf(set(1, 30, 0.0, "e2")))
    )

    @Test fun cargaSemCasasInuteisEComVirgula() {
        assertEquals("40", formatKgBr(40.0))
        assertEquals("42,5", formatKgBr(42.5))
        assertEquals("2,25", formatKgBr(2.25))
        assertEquals("0", formatKgBr(0.0))
    }

    @Test fun resumoContaSeriesRepsVolumeEDuracao() {
        val s = summarize(workout, groups, utc)
        assertEquals("26/10/2026", s.date)
        assertEquals(62L, s.durationMinutes)
        assertEquals(3, s.sets)
        assertEquals(52, s.reps)
        assertEquals(12 * 40.0 + 10 * 42.5, s.volumeKg)
    }

    @Test fun treinoSemDatasNaoQuebra() {
        val s = summarize(null, groups, utc)
        assertNull(s.date)
        assertNull(s.durationMinutes)
    }

    @Test fun textoDeCompartilharTemTituloSeriesTotalENota() {
        val txt = shareText("Pernas", workout.copy(notes = " dia pesado "), groups, utc)
        val lines = txt.lines()
        assertEquals("Treino de Pernas · 26/10/2026 · 62 min", lines[0])
        assertEquals("• Cadeira Extensora: 12×40, 10×42,5 kg", lines[1])
        assertEquals("• Prancha: 30 reps", lines[2])
        assertEquals("3 séries · 905 kg de volume", lines[3])
        assertEquals("Nota: dia pesado", lines[4])
        assertEquals("Registrado no GymTracker", lines.last())
    }

    @Test fun recordesNovosEntramAntesDoTotal() {
        val lines = shareText("Pernas", workout, groups, utc, newRecords = listOf("Cadeira Extensora")).lines()
        assertEquals("Novo recorde: Cadeira Extensora", lines[3])
        assertEquals("3 séries · 905 kg de volume", lines[4])
    }

    @Test fun semGrupoViraSoTreino() {
        assertTrue(shareText("Sem grupo", null, groups, utc).startsWith("Treino\n"))
        assertTrue(shareText(null, null, groups, utc).startsWith("Treino\n"))
    }

    @Test fun detalheCarregaOTreinoESalvaAAnotacao() {
        val repo = FakeWorkoutRepository().apply {
            history += workout
            sets += set(1, 12, 40.0)
        }
        val vm = WorkoutDetailViewModel("w1", repo, FakeExerciseRepository(listOf(Exercise(id = "e1", name = "Cadeira Extensora"))))
        assertEquals("w1", vm.uiState.value.workout?.id)
        assertEquals(1, vm.uiState.value.summary?.sets)

        vm.saveNotes("  ombro incomodou ")
        assertEquals("ombro incomodou", vm.uiState.value.workout?.notes)
        assertEquals("ombro incomodou", repo.history.single().notes)
        assertEquals("Anotação salva", vm.uiState.value.successMessage)

        vm.saveNotes("   ")
        assertNull(repo.history.single().notes)
        assertEquals("Anotação apagada", vm.uiState.value.successMessage)
    }

    @Test fun anotacaoLongaDemaisNaoESalva() {
        val repo = FakeWorkoutRepository().apply { history += workout }
        val vm = WorkoutDetailViewModel("w1", repo, FakeExerciseRepository())
        vm.saveNotes("x".repeat(1001))
        assertEquals("A anotação pode ter até 1000 caracteres", vm.uiState.value.errorMessage)
        assertNull(repo.history.single().notes)
    }

    @Test fun falhaAoSalvarAnotacaoMostraErro() {
        val repo = FakeWorkoutRepository().apply { history += workout; failNotes = RuntimeException("Unable to resolve host") }
        val vm = WorkoutDetailViewModel("w1", repo, FakeExerciseRepository())
        vm.saveNotes("algo")
        assertEquals("Sem conexão com a internet. Verifique sua rede e tente de novo.", vm.uiState.value.errorMessage)
        assertTrue(!vm.uiState.value.isSavingNotes)
    }
}
