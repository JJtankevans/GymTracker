package com.vitoroliveira.gymtracker.presentation.workout

import com.vitoroliveira.gymtracker.data.model.Exercise
import com.vitoroliveira.gymtracker.data.model.ExportSet
import com.vitoroliveira.gymtracker.data.model.ExportWorkout
import com.vitoroliveira.gymtracker.data.model.MuscleGroup
import com.vitoroliveira.gymtracker.domain.repository.MuscleGroupRepository
import com.vitoroliveira.gymtracker.presentation.viewmodel.FakeExerciseRepository
import com.vitoroliveira.gymtracker.presentation.viewmodel.FakeWorkoutRepository
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

private class Groups(val list: List<MuscleGroup>) : MuscleGroupRepository {
    override suspend fun getAll() = list
    override suspend fun insert(muscleGroup: MuscleGroup) = muscleGroup
    override suspend fun update(muscleGroup: MuscleGroup) {}
    override suspend fun delete(id: String) {}
}

@OptIn(ExperimentalCoroutinesApi::class)
class CsvExportTest {
    @BeforeTest fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @AfterTest fun tearDown() = Dispatchers.resetMain()

    private val w1 = ExportWorkout(id = "w1", startedAt = "2026-10-26T13:05:00Z", muscleGroupId = "g1")
    private val w2 = ExportWorkout(id = "w2", startedAt = "2026-10-20T09:00:00Z", muscleGroupId = null, muscleGroupName = "Pernas (apagado)")
    private val rows = listOf(
        ExportSet("e1", 2, 10, 42.5, "2026-10-26T13:10:00Z", w1),
        ExportSet("e1", 1, 12, 40.0, "2026-10-26T13:07:00Z", w1),
        ExportSet("e2", 1, 15, 0.0, "2026-10-20T09:05:00Z", w2)
    )

    @Test fun geraUmaLinhaPorSerieOrdenadaPorData() {
        val csv = historyCsv(rows, mapOf("e1" to "Supino; Reto", "e2" to "Prancha"), mapOf("g1" to "Peito"), TimeZone.UTC)
        val lines = csv.trimEnd().lines()
        assertEquals("data;hora;grupo;exercicio;serie;reps;carga_kg;volume_kg", lines[0])
        assertEquals("20/10/2026;09:00;Pernas (apagado);Prancha;1;15;0;0", lines[1])
        assertEquals("26/10/2026;13:05;Peito;\"Supino; Reto\";1;12;40;480", lines[2])
        assertEquals("26/10/2026;13:05;Peito;\"Supino; Reto\";2;10;42,5;425", lines[3])
    }

    @Test fun celulaComAspasDobraAsAspas() {
        assertEquals("\"Rosca \"\"21\"\"\"", csvCell("Rosca \"21\""))
        assertEquals("simples", csvCell("simples"))
    }

    @Test fun viewModelGeraECsvEDepoisLimpa() {
        val repo = FakeWorkoutRepository().apply { exportSets = rows }
        val vm = ExportViewModel(repo, FakeExerciseRepository(listOf(Exercise(id = "e1", name = "Supino"))), Groups(emptyList()))
        vm.export()
        assertEquals(3, vm.state.value.rows)
        assertEquals(true, vm.state.value.csv?.contains("Exercício removido"))
        vm.consumed()
        assertNull(vm.state.value.csv)
    }

    @Test fun semSeriesAvisa() {
        val vm = ExportViewModel(FakeWorkoutRepository(), FakeExerciseRepository(), Groups(emptyList()))
        vm.export()
        assertEquals("Ainda não há séries registradas para exportar", vm.state.value.errorMessage)
    }
}
