package com.vitoroliveira.gymtracker.presentation.workout

import com.vitoroliveira.gymtracker.data.model.MuscleGroup
import com.vitoroliveira.gymtracker.data.model.Workout

/**
 * Próximo grupo da rotação (ABC...): um grupo que ainda nunca foi treinado ou, se todos já foram, o treinado há mais tempo.
 * Null quando não há o que sugerir (menos de 2 grupos ou nenhum treino com grupo ainda).
 */
fun suggestNextGroup(groups: List<MuscleGroup>, history: List<Workout>): String? {
    val ids = groups.mapNotNull { it.id }
    if (ids.size < 2) return null
    val lastByGroup = history
        .filter { it.muscleGroupId in ids && it.startedAt != null }
        .groupBy { it.muscleGroupId!! }
        .mapValues { (_, ws) -> ws.maxOf { it.startedAt!! } }
    if (lastByGroup.isEmpty()) return null
    ids.firstOrNull { it !in lastByGroup }?.let { return it }
    return lastByGroup.minByOrNull { it.value }?.key
}
