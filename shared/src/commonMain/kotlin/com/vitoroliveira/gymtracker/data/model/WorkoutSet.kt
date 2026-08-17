// shared/src/commonMain/kotlin/com/vitoroliveira/gymtracker/data/model/WorkoutSet.kt

package com.vitoroliveira.gymtracker.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WorkoutSet(
    val id: String? = null,
    @SerialName("workout_id") val workoutId: String,
    @SerialName("exercise_id") val exerciseId: String,
    @SerialName("set_number") val setNumber: Int,
    val reps: Int,
    @SerialName("weight_kg") val weightKg: Double = 0.0,
    @SerialName("rest_seconds") val restSeconds: Int? = null,
    @SerialName("completed_at") val completedAt: String? = null
)