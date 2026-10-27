// shared/src/commonMain/kotlin/com/vitoroliveira/gymtracker/data/model/Workout.kt

package com.vitoroliveira.gymtracker.data.model

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class Workout(
    val id: String? = null,
    @SerialName("profile_id") val profileId: String? = null,
    @SerialName("muscle_group_id") val muscleGroupId: String? = null,
    /**
     * Nome do grupo guardado pelo banco (migração 009) quando o grupo é apagado, para o Histórico não perder o nome.
     * Nunca vai no INSERT: quem preenche é o gatilho do banco, e assim o app não quebra se a migração ainda não rodou.
     */
    @EncodeDefault(EncodeDefault.Mode.NEVER) @SerialName("muscle_group_name") val muscleGroupName: String? = null,
    @SerialName("started_at") val startedAt: String? = null,
    @SerialName("finished_at") val finishedAt: String? = null,
    @SerialName("finished_exercise_ids") val finishedExerciseIds: List<String> = emptyList(),
    val notes: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)