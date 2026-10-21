package com.vitoroliveira.gymtracker.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Linha de `records_summary()` (migração 008): o recorde de carga de um exercício, já calculado no servidor. */
@Serializable
data class RecordSummary(
    @SerialName("exercise_id") val exerciseId: String,
    @SerialName("exercise_name") val exerciseName: String,
    @SerialName("best_weight_kg") val bestWeightKg: Double,
    /** Dia (yyyy-MM-dd) em que a carga recorde foi batida pela primeira vez. */
    @SerialName("best_date") val bestDate: String,
    @SerialName("best_one_rm_kg") val bestOneRmKg: Double,
    @SerialName("previous_best_kg") val previousBestKg: Double? = null,
    val sessions: Int
)
