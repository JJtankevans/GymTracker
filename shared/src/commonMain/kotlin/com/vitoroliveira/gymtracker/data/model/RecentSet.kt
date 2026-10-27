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

/** Série com os dados do treino embutidos, para exportar o histórico inteiro. */
@Serializable
data class ExportSet(
    @SerialName("exercise_id") val exerciseId: String,
    @SerialName("set_number") val setNumber: Int,
    val reps: Int,
    @SerialName("weight_kg") val weightKg: Double = 0.0,
    @SerialName("completed_at") val completedAt: String? = null,
    val workouts: ExportWorkout? = null
)

@Serializable
data class ExportWorkout(
    val id: String? = null,
    @SerialName("started_at") val startedAt: String? = null,
    @SerialName("muscle_group_id") val muscleGroupId: String? = null,
    @SerialName("muscle_group_name") val muscleGroupName: String? = null
)
