// shared/src/commonMain/kotlin/com/vitoroliveira/gymtracker/data/model/Workout.kt

package com.vitoroliveira.gymtracker.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Workout(
    val id: String? = null,
    @SerialName("profile_id") val profileId: String? = null,
    @SerialName("muscle_group_id") val muscleGroupId: String? = null,
    @SerialName("started_at") val startedAt: String? = null,
    @SerialName("finished_at") val finishedAt: String? = null,
    @SerialName("finished_exercise_ids") val finishedExerciseIds: List<String> = emptyList(),
    val notes: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)