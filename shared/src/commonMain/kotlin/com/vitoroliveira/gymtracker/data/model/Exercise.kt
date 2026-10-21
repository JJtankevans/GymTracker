// shared/src/commonMain/kotlin/com/vitoroliveira/gymtracker/data/model/Exercise.kt

package com.vitoroliveira.gymtracker.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class Exercise(
    val id: String? = null,
    @SerialName("profile_id") val profileId: String? = null,
    /** LEGADO (antes da migração 006): o app não decide nada por ele; os grupos ficam em [groupIds]. */
    @SerialName("muscle_group_id") val muscleGroupId: String? = null,
    val name: String,
    @SerialName("default_sets") val defaultSets: Int = 3,
    @SerialName("default_reps") val defaultReps: Int = 12,
    val notes: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    /** Grupos em que o exercício está (tabela `group_exercises`); o repositório preenche e grava, não vai no JSON da linha. */
    @Transient val groupIds: List<String> = emptyList()
)