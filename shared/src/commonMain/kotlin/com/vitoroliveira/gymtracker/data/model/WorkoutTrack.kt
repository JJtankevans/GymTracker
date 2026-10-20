package com.vitoroliveira.gymtracker.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Música que tocou durante um treino (tabela `workout_tracks`, ver docs/supabase/002_social.sql). */
@Serializable
data class WorkoutTrack(
    @SerialName("workout_id") val workoutId: String,
    @SerialName("profile_id") val profileId: String,
    val title: String,
    val artist: String? = null,
    val album: String? = null,
    @SerialName("source_app") val sourceApp: String? = null,
    @SerialName("played_at") val playedAt: String
)

/** Faixa capturada no celular, ainda sem dono (o repositório preenche o profile_id). */
data class CapturedTrack(
    val title: String,
    val artist: String?,
    val album: String?,
    val sourceApp: String?,
    val playedAtIso: String
)
