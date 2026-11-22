package com.vitoroliveira.gymtracker.presentation.workout

import com.vitoroliveira.gymtracker.data.model.Exercise
import com.vitoroliveira.gymtracker.data.model.ExportSet
import com.vitoroliveira.gymtracker.data.model.ExportWorkout
import com.vitoroliveira.gymtracker.data.model.MuscleGroup
import com.vitoroliveira.gymtracker.data.model.Workout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BackupExportTest {
    private val group = MuscleGroup(id = "g1", name = "Peito e Tríceps", colorHex = "#4CAF50")
    private val exercise = Exercise(id = "e1", name = "Tríceps Pulley", defaultSets = 3, defaultReps = 12)
    private val workout = Workout(id = "w1", muscleGroupId = "g1", startedAt = "2026-11-21T20:00:00Z", finishedAt = "2026-11-21T21:00:00Z")
    private val set = ExportSet(
        exerciseId = "e1", setNumber = 1, reps = 12, weightKg = 12.5, completedAt = "2026-11-21T20:10:00Z",
        workouts = ExportWorkout(id = "w1", startedAt = "2026-11-21T20:00:00Z", muscleGroupId = "g1")
    )

    @Test
    fun backupVoltaIgualAoLer() {
        val json = buildBackupJson("2026-11-22T01:00:00Z", listOf(group), listOf(exercise), listOf(workout), listOf(set))
        val back = parseBackupJson(json)

        assertEquals("GymTracker", back.app)
        assertEquals(1, back.version)
        assertEquals("2026-11-22T01:00:00Z", back.createdAt)
        assertEquals(listOf("g1"), back.groups.map { it.id })
        assertEquals(listOf("Tríceps Pulley"), back.exercises.map { it.name })
        assertEquals(listOf("w1"), back.workouts.map { it.id })
        assertEquals(12.5, back.sets.single().weightKg)
    }

    @Test
    fun acentosFicamLegiveisNoArquivo() {
        val json = buildBackupJson("x", listOf(group), emptyList(), emptyList(), emptyList())
        assertTrue(json.contains("Peito e Tríceps"))
    }
}
