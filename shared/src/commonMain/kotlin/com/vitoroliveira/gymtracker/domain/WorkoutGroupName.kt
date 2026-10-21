package com.vitoroliveira.gymtracker.domain

import com.vitoroliveira.gymtracker.data.model.MuscleGroup
import com.vitoroliveira.gymtracker.data.model.Workout

/**
 * Nome do grupo de um treino do Histórico: o nome de agora se o grupo ainda existe (acompanha renomeações) ou o nome
 * que o banco guardou quando o grupo foi apagado (migração 009). Null só para treino livre ou de grupo apagado antes da migração.
 */
fun Workout.resolveGroupName(groups: Map<String?, MuscleGroup>): String? =
    muscleGroupId?.let { groups[it]?.name } ?: muscleGroupName?.takeIf { it.isNotBlank() }
