package com.vitoroliveira.gymtracker.presentation.viewmodel

import com.vitoroliveira.gymtracker.data.model.MuscleGroup
import com.vitoroliveira.gymtracker.data.model.Workout
import com.vitoroliveira.gymtracker.domain.repository.MuscleGroupRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

private class NoGroups : MuscleGroupRepository {
    override suspend fun getAll() = emptyList<MuscleGroup>()
    override suspend fun insert(muscleGroup: MuscleGroup) = muscleGroup
    override suspend fun update(muscleGroup: MuscleGroup) {}
    override suspend fun delete(id: String) {}
}

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModelTest {
    @BeforeTest fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @AfterTest fun tearDown() = Dispatchers.resetMain()

    private fun workout(id: String) = Workout(
        id = id, startedAt = "2026-10-20T10:00:00Z", finishedAt = "2026-10-20T10:30:00Z"
    )

    private class SomeGroups(val groups: List<MuscleGroup>) : MuscleGroupRepository {
        override suspend fun getAll() = groups
        override suspend fun insert(muscleGroup: MuscleGroup) = muscleGroup
        override suspend fun update(muscleGroup: MuscleGroup) {}
        override suspend fun delete(id: String) {}
    }

    @Test fun grupoApagadoMantemONomeGuardadoPeloBanco() {
        val repo = FakeWorkoutRepository().apply {
            // grupo apagado: muscle_group_id virou null, mas o banco guardou o nome
            history += workout("a").copy(muscleGroupId = null, muscleGroupName = "Pernas")
            history += workout("b").copy(muscleGroupId = null) // treino livre
        }
        val vm = HistoryViewModel(repo, NoGroups())
        assertEquals(listOf("Pernas", "Sem grupo"), vm.uiState.value.workouts.map { it.muscleGroupName })
    }

    @Test fun grupoQueAindaExisteUsaONomeDeAgoraMesmoComNomeGuardadoAntigo() {
        val repo = FakeWorkoutRepository().apply {
            history += workout("a").copy(muscleGroupId = "g1", muscleGroupName = "Nome antigo")
        }
        val vm = HistoryViewModel(repo, SomeGroups(listOf(MuscleGroup(id = "g1", name = "Pernas e Glúteos", colorHex = "#4CAF50"))))
        val item = vm.uiState.value.workouts.single()
        assertEquals("Pernas e Glúteos", item.muscleGroupName)
        assertEquals("#4CAF50", item.muscleGroupColorHex)
    }

    @Test fun apagarTreinoTiraDaListaEDoRepositorio() {
        val repo = FakeWorkoutRepository().apply { history += listOf(workout("a"), workout("b")) }
        val vm = HistoryViewModel(repo, NoGroups())
        assertEquals(listOf("a", "b"), vm.uiState.value.workouts.map { it.workoutId })

        vm.deleteWorkout("a")

        // sai da lista na hora, mas só é apagado de verdade depois do tempo de desfazer
        assertEquals(listOf("b"), vm.uiState.value.workouts.map { it.workoutId })
        assertEquals(listOf("a", "b"), repo.history.map { it.id })
        vm.commitPendingDelete()
        assertEquals(listOf("b"), repo.history.map { it.id })
        assertNull(vm.uiState.value.pendingUndo)
        assertNull(vm.uiState.value.errorMessage)
    }

    @Test fun desfazerVoltaOTreinoParaOMesmoLugarSemApagar() {
        val repo = FakeWorkoutRepository().apply { history += workout("a"); history += workout("b"); history += workout("c") }
        val vm = HistoryViewModel(repo, NoGroups())

        vm.deleteWorkout("b")
        assertEquals(listOf("a", "c"), vm.uiState.value.workouts.map { it.workoutId })
        vm.undoDelete()

        assertEquals(listOf("a", "b", "c"), vm.uiState.value.workouts.map { it.workoutId })
        assertEquals(listOf("a", "b", "c"), repo.history.map { it.id })
        vm.commitPendingDelete() // não há pendente: não apaga nada
        assertEquals(listOf("a", "b", "c"), repo.history.map { it.id })
    }

    @Test fun apagarOutroTreinoFechaOPendenteAnterior() {
        val repo = FakeWorkoutRepository().apply { history += workout("a"); history += workout("b"); history += workout("c") }
        val vm = HistoryViewModel(repo, NoGroups())

        vm.deleteWorkout("a")
        vm.deleteWorkout("b")

        assertEquals(listOf("c"), vm.uiState.value.workouts.map { it.workoutId })
        assertEquals(listOf("b", "c"), repo.history.map { it.id }) // "a" já foi apagado; "b" está pendente
        assertEquals("b", vm.uiState.value.pendingUndo?.item?.workoutId)
    }

    @Test fun falhaAoApagarMantemOTreinoEMostraErro() {
        val repo = FakeWorkoutRepository().apply {
            history += workout("a")
            failDelete = Exception("Unable to resolve host")
        }
        val vm = HistoryViewModel(repo, NoGroups())

        vm.deleteWorkout("a")
        vm.commitPendingDelete()

        assertEquals(listOf("a"), vm.uiState.value.workouts.map { it.workoutId })
        assertNotNull(vm.uiState.value.errorMessage)
    }

    private fun item(durationMin: Long?) = WorkoutHistoryItem(
        workoutId = "w", muscleGroupName = "x", muscleGroupColorHex = "#000000",
        startedAt = kotlin.time.Instant.parse("2026-11-04T10:00:00Z"),
        finishedAt = durationMin?.let { kotlin.time.Instant.parse("2026-11-04T10:00:00Z") + kotlin.time.Duration.parse("${it}m") },
        durationMinutes = durationMin
    )

    @Test fun duracaoAbsurdaUsaOHorarioDaUltimaSerie() {
        val w = item(1060) // 17 h 40 min: ficou aberto
        assertEquals(75L, realDuration(w, listOf("2026-11-04T10:30:00Z", "2026-11-04T11:15:00Z")))
    }

    @Test fun serieLancadaDiasDepoisNaoEsticaOTreino() {
        val w = item(1060)
        // pausa de 17 h antes da última série: o treino acabou às 11:15
        assertEquals(75L, realDuration(w, listOf("2026-11-04T10:30:00Z", "2026-11-04T11:15:00Z", "2026-11-05T04:00:00Z")))
    }

    @Test fun duracaoNormalNaoMuda() {
        assertEquals(60L, realDuration(item(60), listOf("2026-11-04T11:30:00Z")))
    }

    @Test fun semSerieMantemODadoDoBanco() {
        assertEquals(1060L, realDuration(item(1060), emptyList()))
    }
}
