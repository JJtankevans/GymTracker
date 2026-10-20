package com.vitoroliveira.gymtracker.data.model

/**
 * O que o usuário fez neste exercício em treinos anteriores.
 * Usado pra preencher peso/reps com a última vez e detectar recordes.
 */
data class ExerciseBaseline(
    val lastWeightKg: Double? = null,
    val lastReps: Int? = null,
    /** Maior carga já registrada (de outros treinos). */
    val recordWeightKg: Double? = null
)
