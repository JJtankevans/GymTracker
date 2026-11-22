package com.vitoroliveira.gymtracker.presentation.workout

import com.vitoroliveira.gymtracker.data.model.Exercise
import com.vitoroliveira.gymtracker.data.model.MuscleGroup
import kotlin.test.Test
import kotlin.test.assertEquals

class BackupRestoreTest {
    private fun backup() = BackupFile(
        createdAt = "x",
        groups = listOf(MuscleGroup(id = "g1", name = "Peito e Tríceps"), MuscleGroup(id = "g2", name = "Ombro")),
        exercises = listOf(Exercise(id = "e1", name = "Tríceps Pulley"), Exercise(id = "e2", name = "Elevação Lateral")),
        exerciseGroups = mapOf("e1" to listOf("g1"), "e2" to listOf("g2", "g1")),
        workouts = emptyList(), sets = emptyList()
    )

    @Test
    fun soCriaOQueAindaNaoExiste() {
        val plan = planCatalogRestore(
            backup(),
            existingGroups = listOf(MuscleGroup(id = "x", name = "  peito e tríceps ")),
            existingExercises = emptyList()
        )
        assertEquals(listOf("Ombro"), plan.newGroups.map { it.name })
        assertEquals(2, plan.newExercises.size)
        assertEquals(1, plan.skipped)
    }

    @Test
    fun contaIgualAoBackupNaoCriaNada() {
        val b = backup()
        val plan = planCatalogRestore(b, b.groups, b.exercises)
        assertEquals(0, plan.newGroups.size + plan.newExercises.size)
        assertEquals(4, plan.skipped)
    }

    @Test
    fun idsDeGrupoViramOsIdsDaContaNova() {
        val ids = remapExerciseGroups("e2", backup(), mapOf("g1" to "N1", "g2" to "N2"))
        assertEquals(listOf("N2", "N1"), ids)
        assertEquals(emptyList(), remapExerciseGroups("e1", backup(), emptyMap()))
    }
}
