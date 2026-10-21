package com.vitoroliveira.gymtracker.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Linha de `group_exercises`: o exercício [exerciseId] está no grupo [groupId]. O `profile_id` o banco preenche (auth.uid()). */
@Serializable
data class GroupExerciseLink(
    @SerialName("group_id") val groupId: String,
    @SerialName("exercise_id") val exerciseId: String
)
