package com.vitoroliveira.gymtracker.presentation.workout

import com.vitoroliveira.gymtracker.data.model.Exercise
import com.vitoroliveira.gymtracker.data.model.ExportSet
import com.vitoroliveira.gymtracker.data.model.MuscleGroup
import com.vitoroliveira.gymtracker.data.model.Workout
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Backup completo em um arquivo (#364). Formato versionado para o futuro "importar":
 * grupos, exercícios, treinos finalizados e todas as séries (já com a data/grupo de cada treino).
 * Não leva senha, token nem dados de outras pessoas.
 */
@Serializable
data class BackupFile(
    val app: String = "GymTracker",
    val version: Int = 1,
    val createdAt: String,
    val groups: List<MuscleGroup>,
    val exercises: List<Exercise>,
    /** exercício → ids dos grupos onde ele está (o vínculo não vai dentro do Exercise). */
    val exerciseGroups: Map<String, List<String>> = emptyMap(),
    val workouts: List<Workout>,
    val sets: List<ExportSet>
)

private val backupJson = Json { prettyPrint = true; encodeDefaults = true; ignoreUnknownKeys = true }

fun buildBackupJson(
    createdAt: String,
    groups: List<MuscleGroup>,
    exercises: List<Exercise>,
    workouts: List<Workout>,
    sets: List<ExportSet>
): String = backupJson.encodeToString(
    BackupFile(
        createdAt = createdAt, groups = groups, exercises = exercises,
        exerciseGroups = exercises.mapNotNull { e -> e.id?.let { it to e.groupIds } }.filter { it.second.isNotEmpty() }.toMap(),
        workouts = workouts, sets = sets
    )
)

/** Lê um backup (para o futuro "importar" e para os testes). */
fun parseBackupJson(text: String): BackupFile = backupJson.decodeFromString(text)
