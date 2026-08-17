// shared/src/commonMain/kotlin/com/vitoroliveira/gymtracker/data/model/MuscleGroup.kt

package com.vitoroliveira.gymtracker.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MuscleGroup(
    val id: String? = null,
    @SerialName("profile_id") val profileId: String? = null,
    val name: String,
    @SerialName("color_hex") val colorHex: String = "#6200EE",
    @SerialName("created_at") val createdAt: String? = null
)