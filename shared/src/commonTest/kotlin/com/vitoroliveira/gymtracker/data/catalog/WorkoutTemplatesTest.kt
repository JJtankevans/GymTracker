package com.vitoroliveira.gymtracker.data.catalog

import com.vitoroliveira.gymtracker.data.model.Exercise
import com.vitoroliveira.gymtracker.data.model.MuscleGroup
import com.vitoroliveira.gymtracker.domain.repository.ExerciseRepository
import com.vitoroliveira.gymtracker.domain.repository.MuscleGroupRepository
import com.vitoroliveira.gymtracker.domain.usecase.CreateWorkoutFromTemplate
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private class RecordingGroups : MuscleGroupRepository {
    val inserted = mutableListOf<MuscleGroup>()
    override suspend fun getAll() = inserted.toList()
    override suspend fun insert(muscleGroup: MuscleGroup) =
        muscleGroup.copy(id = "g${inserted.size + 1}").also { inserted += it }
    override suspend fun update(muscleGroup: MuscleGroup) {}
    override suspend fun delete(id: String) {}
}

private class RecordingExercises : ExerciseRepository {
    val inserted = mutableListOf<Exercise>()
    override suspend fun getAll() = inserted.toList()
    override suspend fun getByMuscleGroup(muscleGroupId: String) = inserted.filter { muscleGroupId in it.groupIds }
    override suspend fun insert(exercise: Exercise) = exercise.copy(id = "e${inserted.size + 1}").also { inserted += it }
    override suspend fun update(exercise: Exercise) {}
    override suspend fun delete(id: String) {}
}

class WorkoutTemplatesTest {

    @Test fun todoNomeExisteNoCatalogo() = WorkoutTemplates.all.forEach { t ->
        assertEquals(t.exerciseNames.size, t.exercises.size, "nome fora do catálogo em ${t.name}")
    }

    @Test fun semExercicioRepetidoNoMesmoTreino() = WorkoutTemplates.all.forEach { t ->
        assertEquals(t.exerciseNames.size, t.exerciseNames.toSet().size, t.name)
    }

    @Test fun temVariosTreinosComNomesUnicos() {
        assertTrue(WorkoutTemplates.all.size >= 6)
        assertEquals(WorkoutTemplates.all.size, WorkoutTemplates.all.map { it.name }.toSet().size)
    }

    @Test fun criaGrupoEExerciciosLigadosAoGrupo() = runTest {
        val groups = RecordingGroups(); val exercises = RecordingExercises()
        val template = WorkoutTemplates.all.first()
        val n = CreateWorkoutFromTemplate(groups, exercises)(template, emptyList())

        assertEquals(template.exerciseNames.size, n)
        assertEquals(template.name, groups.inserted.single().name)
        assertTrue(exercises.inserted.all { it.groupIds == listOf("g1") })
        assertEquals(template.exerciseNames, exercises.inserted.map { it.name })
    }

    @Test fun nomeRepetidoGanhaNumero() = runTest {
        val groups = RecordingGroups(); val exercises = RecordingExercises()
        val template = WorkoutTemplates.all.first()
        CreateWorkoutFromTemplate(groups, exercises)(template, listOf(template.name.uppercase(), template.name + " 2"))
        assertEquals(template.name + " 3", groups.inserted.single().name)
    }

    @Test fun repsEmSegundosViramObservacao() = runTest {
        val groups = RecordingGroups(); val exercises = RecordingExercises()
        val inferior = WorkoutTemplates.all.first { "Prancha" in it.exerciseNames }
        CreateWorkoutFromTemplate(groups, exercises)(inferior, emptyList())
        assertEquals("Repetições = segundos por série", exercises.inserted.first { it.name == "Prancha" }.notes)
    }
}
