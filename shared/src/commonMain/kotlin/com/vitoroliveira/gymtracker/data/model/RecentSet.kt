package com.vitoroliveira.gymtracker.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Série recente, com a data do treino embutida (workout_sets + workouts). Usada no volume semanal por músculo. */
@Serializable
data class RecentSet(
    @SerialName("exercise_id") val exerciseId: String,
    val reps: Int,
    @SerialName("weight_kg") val weightKg: Double = 0.0,
    val workouts: WorkoutDate? = null
)

@Serializable
data class WorkoutDate(@SerialName("started_at") val startedAt: String? = null)
