package com.vitoroliveira.gymtracker.presentation.share

import com.vitoroliveira.gymtracker.data.model.Exercise
import com.vitoroliveira.gymtracker.data.model.MuscleGroup
import com.vitoroliveira.gymtracker.data.model.NewSharedRoutine
import com.vitoroliveira.gymtracker.data.model.SharedExercise
import com.vitoroliveira.gymtracker.data.model.SharedRoutine
import com.vitoroliveira.gymtracker.domain.repository.ExerciseRepository
import com.vitoroliveira.gymtracker.domain.repository.MuscleGroupRepository
import com.vitoroliveira.gymtracker.domain.repository.RoutineShareRepository
import com.vitoroliveira.gymtracker.domain.usecase.uniqueGroupName
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
import kotlin.test.assertTrue

private class MemShares : RoutineShareRepository {
    val saved = mutableMapOf<String, SharedRoutine>()
    val countedImports = mutableListOf<String>()
    override suspend fun share(routine: NewSharedRoutine): String = seed(routine)
    fun seed(routine: NewSharedRoutine): String {
        val code = "CODE${saved.size + 1}".padEnd(8, 'X')
        saved[code] = SharedRoutine(code, routine.name, routine.colorHex, routine.exercises, ownerUsername = "thiago")
        return code
    }
    override suspend fun find(code: String, countImport: Boolean): SharedRoutine? {
        if (countImport) countedImports += code
        return saved[code]
    }
}

private class MemGroups : MuscleGroupRepository {
    val rows = mutableListOf<MuscleGroup>()
    override suspend fun getAll() = rows.toList()
    override suspend fun insert(muscleGroup: MuscleGroup) = muscleGroup.copy(id = "g${rows.size + 1}").also { rows += it }
    override suspend fun update(muscleGroup: MuscleGroup) {}
    override suspend fun delete(id: String) { rows.removeAll { it.id == id } }
}

private class MemExercises(var failAfterInserts: Int = Int.MAX_VALUE) : ExerciseRepository {
    val rows = mutableListOf<Exercise>()
    private var inserts = 0
    override suspend fun getAll() = rows.toList()
    override suspend fun getByMuscleGroup(muscleGroupId: String) = rows.filter { muscleGroupId in it.groupIds }
    override suspend fun insert(exercise: Exercise): Exercise {
        if (inserts++ >= failAfterInserts) throw RuntimeException("Unable to resolve host")
        return exercise.copy(id = "e${rows.size + 1}").also { rows += it }
    }
    override suspend fun update(exercise: Exercise) { rows[rows.indexOfFirst { it.id == exercise.id }] = exercise }
    override suspend fun delete(id: String) {}
}

@OptIn(ExperimentalCoroutinesApi::class)
class RoutineShareTest {
    @BeforeTest fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @AfterTest fun tearDown() = Dispatchers.resetMain()

    @Test fun codigoDigitadoDeVariosJeitos() {
        assertEquals("K7F2Q9XA", normalizeShareCode("k7f2q9xa"))
        assertEquals("K7F2Q9XA", normalizeShareCode("  K7F2-Q9XA "))
        assertEquals("K7F2Q9XA", normalizeShareCode("K7F2 Q9XA"))
        assertEquals("K7F2Q9XA", normalizeShareCode(shareInvite(SharedCode("Pernas", "K7F2Q9XA"))))
        assertNull(normalizeShareCode("abc"))
        assertNull(normalizeShareCode(""))
    }

    @Test fun nomeRepetidoGanhaNumero() {
        assertEquals("Pernas", uniqueGroupName("Pernas", listOf("Peito")))
        assertEquals("Pernas 3", uniqueGroupName(" pernas ", listOf("Pernas", "PERNAS 2")).replaceFirstChar { it.uppercase() })
    }

    @Test fun compartilhaOsExerciciosDoGrupo() {
        val shares = MemShares(); val groups = MemGroups(); val ex = MemExercises()
        ex.rows += Exercise(id = "e1", name = "Cadeira Extensora", defaultSets = 3, defaultReps = 15, groupIds = listOf("g1"))
        ex.rows += Exercise(id = "e2", name = "Supino", groupIds = listOf("g9"))
        val vm = RoutineShareViewModel(shares, groups, ex)
        vm.share(MuscleGroup(id = "g1", name = "Pernas", colorHex = "#4CAF50"))

        val shared = assertNotNull(vm.state.value.shared)
        assertEquals("Pernas", shared.groupName)
        assertEquals(listOf(SharedExercise("Cadeira Extensora", 3, 15)), shares.saved.getValue(shared.code).exercises)
    }

    @Test fun grupoVazioNaoECompartilhado() {
        val shares = MemShares()
        val vm = RoutineShareViewModel(shares, MemGroups(), MemExercises())
        vm.share(MuscleGroup(id = "g1", name = "Vazio", colorHex = "#000000"))
        assertEquals("Adicione exercícios ao grupo antes de compartilhar", vm.state.value.error)
        assertTrue(shares.saved.isEmpty())
    }

    @Test fun amigoBuscaPrevisualizaEImporta() {
        val shares = MemShares()
        val code = shares.seed(NewSharedRoutine("Pernas", "#4CAF50", listOf(SharedExercise("Cadeira Extensora", 3, 15), SharedExercise("Agachamento Hack", 4, 10, "pés altos"))))
        val groups = MemGroups().apply { rows += MuscleGroup(id = "g0", name = "Pernas", colorHex = "#000000") }
        val ex = MemExercises().apply { rows += Exercise(id = "e0", name = "Cadeira Extensora", defaultSets = 5, defaultReps = 8) }
        val vm = RoutineShareViewModel(shares, groups, ex)

        vm.lookup(code.lowercase())
        assertEquals("Pernas", vm.state.value.preview?.name)
        vm.confirmImport()

        val s = vm.state.value
        assertNull(s.preview)
        assertEquals(1, s.importedCount)
        assertEquals("Treino 'Pernas 2' importado com 2 exercícios", s.message)
        val newGroup = groups.rows.first { it.name == "Pernas 2" }
        assertEquals("#4CAF50", newGroup.colorHex)
        // a Cadeira que ele já tinha foi reaproveitada (mantém 5×8 dele); o Hack foi criado com a nota do amigo
        assertEquals(2, ex.rows.size)
        assertEquals(listOf(newGroup.id), ex.rows.first { it.name == "Cadeira Extensora" }.groupIds)
        assertEquals(5, ex.rows.first { it.name == "Cadeira Extensora" }.defaultSets)
        assertEquals("pés altos", ex.rows.first { it.name == "Agachamento Hack" }.notes)
        assertEquals(listOf(code), shares.countedImports)
    }

    @Test fun treinoGigantePassaPeloLimiteDeImportacao() {
        val shares = MemShares()
        val many = (1..100).map { SharedExercise(if (it == 1) "N".repeat(300) else "Exercício $it", 3, 12, "x".repeat(900)) }
        val code = shares.seed(NewSharedRoutine("G".repeat(200), "#4CAF50", many))
        val groups = MemGroups()
        val ex = MemExercises()
        val vm = RoutineShareViewModel(shares, groups, ex)
        vm.lookup(code)
        vm.confirmImport()
        assertEquals(60, ex.rows.size)
        assertEquals(80, groups.rows.single().name.length)
        assertEquals(80, ex.rows.maxOf { it.name.length })
        assertEquals(500, ex.rows.first().notes!!.length)
        assertTrue("com 60 exercícios" in vm.state.value.message.orEmpty())
    }

    @Test fun codigoInexistenteAvisa() {
        val vm = RoutineShareViewModel(MemShares(), MemGroups(), MemExercises())
        vm.lookup("ZZZZZZZZ")
        assertEquals("Nenhum treino com o código ZZZZZZZZ", vm.state.value.error)
    }

    @Test fun falhaNoMeioDesfazOGrupo() {
        val shares = MemShares()
        shares.seed(NewSharedRoutine("Costas", "#2196F3", listOf(SharedExercise("Remada A"), SharedExercise("Remada B"))))
        val groups = MemGroups()
        val vm = RoutineShareViewModel(shares, groups, MemExercises(failAfterInserts = 1))
        vm.lookup(shares.saved.keys.first())
        vm.confirmImport()
        assertEquals("Sem conexão com a internet. Verifique sua rede e tente de novo.", vm.state.value.error)
        assertTrue(groups.rows.isEmpty())
    }
}
