package com.vitoroliveira.gymtracker.presentation.viewmodel

import com.vitoroliveira.gymtracker.data.model.Exercise
import com.vitoroliveira.gymtracker.data.model.ExerciseBaseline
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class WorkoutViewModelTest {

    private val supino = Exercise(id = "e1", name = "Supino", defaultSets = 3, defaultReps = 10)
    private val crucifixo = Exercise(id = "e2", name = "Crucifixo", defaultSets = 3, defaultReps = 12)

    private lateinit var workouts: FakeWorkoutRepository
    private lateinit var exercises: FakeExerciseRepository
    private lateinit var clock: FixedClock

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        workouts = FakeWorkoutRepository()
        exercises = FakeExerciseRepository(listOf(supino, crucifixo))
        clock = FixedClock(Instant.parse("2026-10-20T10:45:00Z"))
    }

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun vm() = WorkoutViewModel(workouts, exercises, clock)

    @Test fun iniciaTreinoECarregaExercicios() {
        val vm = vm()
        vm.startWorkout(null)
        val s = vm.uiState.value
        assertNotNull(s.activeWorkout)
        assertEquals(2, s.exercises.size)
        assertFalse(s.isLoading)
    }

    @Test fun startWorkoutEIdempotente() {
        val vm = vm()
        vm.startWorkout(null)
        vm.startWorkout(null) // ex.: tela girou
        assertEquals(1, workouts.startCalls)
        assertNull(vm.uiState.value.activeWorkoutToRecover)
    }

    @Test fun oferecerRetomadaQuandoJaHaTreinoAtivo() {
        workouts.activeWorkout = com.vitoroliveira.gymtracker.data.model.Workout(id = "velho")
        val vm = vm()
        vm.startWorkout(null)
        assertEquals("velho", vm.uiState.value.activeWorkoutToRecover?.id)
        assertNull(vm.uiState.value.activeWorkout)
        assertEquals(0, workouts.startCalls)
    }

    @Test fun tempoDecorridoUsaRelogioInjetado() {
        val vm = vm()
        vm.startWorkout(null)
        assertEquals(45 * 60L, vm.elapsedSeconds.value)
    }

    @Test fun registraSerieNumerada() {
        val vm = vm()
        vm.startWorkout(null)
        vm.addSet("e1", 10, 80.0)
        vm.addSet("e1", 8, 82.5)
        val sets = vm.uiState.value.completedSets
        assertEquals(listOf(1, 2), sets.map { it.setNumber })
        assertEquals(82.5, sets.last().weightKg)
    }

    @Test fun apagarSerieDoMeioNaoDuplicaNumero() {
        val vm = vm()
        vm.startWorkout(null)
        repeat(3) { vm.addSet("e1", 10, 80.0) }
        val meio = vm.uiState.value.completedSets[1].id!!
        vm.deleteSet(meio)
        vm.addSet("e1", 10, 80.0)
        val numeros = vm.uiState.value.completedSets.map { it.setNumber }
        assertEquals(numeros.size, numeros.toSet().size, "números de série repetidos: $numeros")
    }

    @Test fun editarSerie() {
        val vm = vm()
        vm.startWorkout(null)
        vm.addSet("e1", 10, 80.0)
        val id = vm.uiState.value.completedSets.single().id!!
        vm.updateSet(id, 12, 85.0)
        val s = vm.uiState.value.completedSets.single()
        assertEquals(12, s.reps)
        assertEquals(85.0, s.weightKg)
    }

    @Test fun validaRepeticoesEPeso() {
        val vm = vm()
        vm.startWorkout(null)
        vm.addSet("e1", 0, 80.0)
        assertNotNull(vm.uiState.value.errorMessage)
        vm.clearError()
        vm.addSet("e1", 10, -5.0)
        assertNotNull(vm.uiState.value.errorMessage)
        assertTrue(vm.uiState.value.completedSets.isEmpty())
    }

    @Test fun pesoZeroEValidoParaPesoDoCorpo() {
        val vm = vm()
        vm.startWorkout(null)
        vm.addSet("e1", 12, 0.0)
        assertNull(vm.uiState.value.errorMessage)
        assertEquals(1, vm.uiState.value.completedSets.size)
    }

    @Test fun respeitaLimiteDeSeries() {
        val vm = vm()
        vm.startWorkout(null)
        repeat(5) { vm.addSet("e1", 10, 80.0) }   // 3 padrão + 2 extras
        assertEquals(5, vm.uiState.value.completedSets.size)
        vm.addSet("e1", 10, 80.0)
        assertEquals(5, vm.uiState.value.completedSets.size)
        val msg = assertNotNull(vm.uiState.value.errorMessage)
        // a mensagem diz qual exercício e qual é o limite
        assertEquals("Você chegou ao limite de 5 séries em Supino (3 do plano + 2 extras).", msg)
        assertEquals(5, maxSetsFor(supino))
    }

    @Test fun finalizarEsperaASerieQueAindaEstaGravando() {
        val vm = vm()
        vm.startWorkout(null)
        val gate = kotlinx.coroutines.CompletableDeferred<Unit>()
        workouts.addSetGate = gate
        vm.addSet("e1", 10, 80.0)      // última série do circuito, ainda indo para o banco
        vm.finishWorkout()             // logo em seguida o circuito termina
        assertFalse(workouts.finished, "o treino foi fechado antes de a série ser gravada")

        gate.complete(Unit)
        assertTrue(workouts.finished)
        assertEquals(1, workouts.sets.size)
        assertEquals(1, assertNotNull(vm.uiState.value.summary).totalSets)
    }

    @Test fun detectaRecordeDeCarga() {
        workouts.baselines = mapOf("e1" to ExerciseBaseline(lastWeightKg = 75.0, lastReps = 10, recordWeightKg = 80.0))
        val vm = vm()
        vm.startWorkout(null)
        vm.addSet("e1", 10, 80.0) // empate não é recorde
        assertTrue("e1" !in vm.uiState.value.newRecordExerciseIds)
        vm.addSet("e1", 6, 82.5)
        assertTrue("e1" in vm.uiState.value.newRecordExerciseIds)
        assertEquals(82.5, vm.uiState.value.baselines["e1"]?.recordWeightKg)
    }

    @Test fun primeiraVezNaoContaComoRecorde() {
        val vm = vm()
        vm.startWorkout(null)
        vm.addSet("e1", 10, 100.0)
        assertTrue(vm.uiState.value.newRecordExerciseIds.isEmpty())
    }

    @Test fun finalizarGeraResumo() {
        workouts.baselines = mapOf("e1" to ExerciseBaseline(recordWeightKg = 60.0))
        val vm = vm()
        vm.startWorkout(null)
        vm.addSet("e1", 10, 80.0)   // 800 (recorde)
        vm.addSet("e1", 10, 80.0)   // 800
        vm.addSet("e2", 12, 20.0)   // 240
        vm.finishWorkout()

        val s = vm.uiState.value
        assertTrue(s.isWorkoutFinished)
        assertTrue(workouts.finished)
        val r = assertNotNull(s.summary)
        assertEquals(3, r.totalSets)
        assertEquals(32, r.totalReps)
        assertEquals(1840.0, r.totalVolumeKg)
        assertEquals(2, r.exercisesWorked)
        assertEquals(45L, r.durationMinutes)
        assertEquals(listOf("Supino"), r.newRecords)
    }

    @Test fun descartarTreinoAtivoApagaEAvisa() {
        val vm = vm()
        vm.startWorkout(null)
        var avisou = false
        vm.discardActiveWorkout { avisou = true }

        assertTrue(avisou)
        assertNull(workouts.activeWorkout)
        assertNull(vm.uiState.value.activeWorkout)
        assertFalse(vm.uiState.value.isWorkoutFinished) // não vira "treino concluído"
    }

    @Test fun errosViramMensagemAmigavel() {
        workouts.failWith = RuntimeException("Unable to resolve host \"x.supabase.co\"")
        val vm = vm()
        vm.startWorkout(null)
        val msg = assertNotNull(vm.uiState.value.errorMessage)
        assertTrue("internet" in msg, msg)
        assertFalse("supabase" in msg)
    }
}
