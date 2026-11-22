package com.vitoroliveira.gymtracker.presentation.workout

import com.vitoroliveira.gymtracker.data.model.Exercise
import com.vitoroliveira.gymtracker.data.model.MuscleGroup

/**
 * Restaurar o **catálogo** de um backup (#364): grupos e exercícios que ainda não existem na conta, sem duplicar
 * e sem apagar nada. O histórico (treinos/séries) não é restaurado: o banco só cria treino "agora", então reinserir
 * datas antigas exigiria uma função nova no servidor (ver docs/MIGRACOES_PENDENTES.md).
 *
 * Nomes são comparados sem diferenciar maiúsculas/minúsculas e sem espaços nas pontas.
 */
data class CatalogRestorePlan(
    /** Grupos do backup que não existem na conta (ainda com o id antigo). */
    val newGroups: List<MuscleGroup>,
    /** Exercícios do backup que não existem na conta (ainda com o id antigo). */
    val newExercises: List<Exercise>,
    /** Quantos já existiam e foram ignorados. */
    val skipped: Int
)

private fun norm(s: String) = s.trim().lowercase()

fun planCatalogRestore(backup: BackupFile, existingGroups: List<MuscleGroup>, existingExercises: List<Exercise>): CatalogRestorePlan {
    val haveGroups = existingGroups.map { norm(it.name) }.toSet()
    val haveExercises = existingExercises.map { norm(it.name) }.toSet()
    val newGroups = backup.groups.filter { norm(it.name) !in haveGroups }
    val newExercises = backup.exercises.filter { norm(it.name) !in haveExercises }
    val skipped = (backup.groups.size - newGroups.size) + (backup.exercises.size - newExercises.size)
    return CatalogRestorePlan(newGroups, newExercises, skipped)
}

/**
 * Para cada exercício novo, os ids de grupo do **backup** viram ids da conta atual: [groupIdMap] liga o id antigo
 * ao id novo (grupos criados agora ou que já existiam com o mesmo nome).
 */
fun remapExerciseGroups(exerciseOldId: String?, backup: BackupFile, groupIdMap: Map<String, String>): List<String> =
    backup.exerciseGroups[exerciseOldId].orEmpty().mapNotNull { groupIdMap[it] }.distinct()
