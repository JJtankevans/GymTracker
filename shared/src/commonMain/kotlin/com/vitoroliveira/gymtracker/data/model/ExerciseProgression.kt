// shared/src/commonMain/kotlin/com/vitoroliveira/gymtracker/data/model/ExerciseProgression.kt

package com.vitoroliveira.gymtracker.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Espelha uma linha da view `vw_exercise_progression` do Supabase:
 * carga máxima, repetições totais e número de séries por dia de
 * treino, para um exercício específico. Base do gráfico de
 * progressão de carga (Fase 4, passo 13).
 */
@Serializable
data class ExerciseProgression(
    @SerialName("exercise_id") val exerciseId: String,
    @SerialName("exercise_name") val exerciseName: String,
    @SerialName("profile_id") val profileId: String,
    @SerialName("session_date") val sessionDate: String,
    @SerialName("max_weight_kg") val maxWeightKg: Double,
    @SerialName("total_reps") val totalReps: Int,
    @SerialName("total_sets") val totalSets: Int
)