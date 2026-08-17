// shared/src/commonMain/kotlin/com/vitoroliveira/gymtracker/data/model/Exercise.kt

package com.vitoroliveira.gymtracker.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Exercise(
    val id: String? = null,
    @SerialName("profile_id") val profileId: String? = null,
    @SerialName("muscle_group_id") val muscleGroupId: String? = null,
    val name: String,
    @SerialName("default_sets") val defaultSets: Int = 3,
    @SerialName("default_reps") val defaultReps: Int = 12,
    val notes: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
)