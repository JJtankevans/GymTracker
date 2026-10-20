package com.vitoroliveira.gymtracker.presentation.viewmodel

import com.vitoroliveira.gymtracker.data.catalog.ExerciseCatalog
import com.vitoroliveira.gymtracker.data.catalog.WorkoutTemplates
import com.vitoroliveira.gymtracker.data.model.Exercise
import com.vitoroliveira.gymtracker.data.model.MuscleGroup
import com.vitoroliveira.gymtracker.domain.repository.ExerciseRepository
import com.vitoroliveira.gymtracker.domain.repository.MuscleGroupRepository
import com.vitoroliveira.gymtracker.domain.usecase.CreateWorkoutFromTemplate
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

private class MemExercises(var failAfter: Int = Int.MAX_VALUE) : ExerciseRepository {
    val rows = mutableListOf<Exercise>()
    override suspend fun getAll() = rows.toList()
    override suspend fun getByMuscleGroup(muscleGroupId: String) = rows.filter { it.muscleGroupId == muscleGroupId }
    override suspend fun insert(exercise: Exercise): Exercise {
        if (rows.size >= failAfter) throw RuntimeException("Unable to resolve host")
        return exercise.copy(id = "e${rows.size + 1}").also { rows += it }
    }
    override suspend fun update(exercise: Exercise) {}
    override suspend fun delete(id: String) {}
}

private class MemGroups : MuscleGroupRepository {
    val rows = mutableListOf<MuscleGroup>()
    override suspend fun getAll() = rows.toList()
    override suspend fun insert(muscleGroup: MuscleGroup) = muscleGroup.copy(id = "g${rows.size + 1}").also { rows += it }
    override suspend fun update(muscleGroup: MuscleGroup) {}
    override suspend fun delete(id: String) {}
}

@OptIn(ExperimentalCoroutinesApi::class)
class CatalogViewModelsTest {
    @BeforeTest fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @AfterTest fun tearDown() = Dispatchers.resetMain()

    @Test fun adicionaVariosExerciciosDoCatalogoComOsPadroes() {
        val repo = MemExercises()
        val vm = ExerciseViewModel(repo)
        val picks = ExerciseCatalog.all.take(3)
        vm.createMany(picks, "g1")

        assertEquals(picks.map { it.name }, repo.rows.map { it.name })
        assertTrue(repo.rows.all { it.muscleGroupId == "g1" })
        assertEquals(picks.map { it.sets }, repo.rows.map { it.defaultSets })
        assertEquals("3 exercícios adicionados", vm.uiState.value.successMessage)
        assertEquals(3, vm.uiState.value.exercises.size) // lista recarregada
    }

    @Test fun umSoExercicioUsaMensagemNoSingular() {
        val vm = ExerciseViewModel(MemExercises())
        vm.createMany(listOf(ExerciseCatalog.all.first()), null)
        assertEquals("Exercício '${ExerciseCatalog.all.first().name}' criado com sucesso", vm.uiState.value.successMessage)
    }

    @Test fun listaVaziaNaoFazNada() {
        val repo = MemExercises()
        val vm = ExerciseViewModel(repo)
        vm.createMany(emptyList(), "g1")
        assertTrue(repo.rows.isEmpty())
        assertNull(vm.uiState.value.successMessage)
    }

    @Test fun falhaNoMeioMostraErroEMantemOQueJaEntrou() {
        val repo = MemExercises(failAfter = 2)
        val vm = ExerciseViewModel(repo)
        vm.createMany(ExerciseCatalog.all.take(4), "g1")

        assertEquals("Sem conexão com a internet. Verifique sua rede e tente de novo.", vm.uiState.value.errorMessage)
        assertEquals(2, vm.uiState.value.exercises.size) // os 2 primeiros ficaram e a tela mostra isso
        assertNull(vm.uiState.value.successMessage)
    }

    @Test fun treinoProntoApareceNaListaDeGrupos() {
        val groups = MemGroups(); val exercises = MemExercises()
        val vm = MuscleGroupViewModel(groups, CreateWorkoutFromTemplate(groups, exercises))
        val template = WorkoutTemplates.all.first()
        vm.createFromTemplate(template)

        val s = vm.uiState.value
        assertEquals(listOf(template.name), s.muscleGroups.map { it.name })
        assertEquals("Treino '${template.name}' criado com ${template.exerciseNames.size} exercícios", s.successMessage)
        assertEquals(template.exerciseNames.size, exercises.rows.size)
        assertTrue(exercises.rows.all { it.muscleGroupId == "g1" })
    }

    @Test fun mesmoTreinoProntoDuasVezesNaoRepeteONome() {
        val groups = MemGroups(); val exercises = MemExercises()
        val vm = MuscleGroupViewModel(groups, CreateWorkoutFromTemplate(groups, exercises))
        val template = WorkoutTemplates.all.first()
        vm.createFromTemplate(template)
        vm.createFromTemplate(template)
        assertEquals(listOf(template.name, template.name + " 2"), vm.uiState.value.muscleGroups.map { it.name })
    }

    @Test fun semOCriadorNaoQuebra() {
        val vm = MuscleGroupViewModel(MemGroups()) // criador é opcional
        vm.createFromTemplate(WorkoutTemplates.all.first())
        assertNotNull(vm.uiState.value)
        assertTrue(vm.uiState.value.muscleGroups.isEmpty())
    }

    @Test fun falhaAoCriarTreinoProntoMostraErro() {
        val groups = MemGroups(); val exercises = MemExercises(failAfter = 1)
        val vm = MuscleGroupViewModel(groups, CreateWorkoutFromTemplate(groups, exercises))
        vm.createFromTemplate(WorkoutTemplates.all.first())
        assertNotNull(vm.uiState.value.errorMessage)
        assertNull(vm.uiState.value.successMessage)
        assertTrue(!vm.uiState.value.isLoading)
    }
}
