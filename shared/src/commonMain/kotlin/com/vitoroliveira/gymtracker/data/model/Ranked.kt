package com.vitoroliveira.gymtracker.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Linha de `ranked_players` (migração 018): o RP de quem entrou na ranqueada. */
@Serializable
data class RankedPlayer(
    @SerialName("profile_id") val profileId: String,
    val rp: Int = 0,
    @SerialName("peak_rp") val peakRp: Int = 0,
    @SerialName("weeks_played") val weeksPlayed: Int = 0
)

/** Linha de `ranked_board()`: alguém da minha turma da semana, com os pontos de agora. */
@Serializable
data class RankedBoardRow(
    val pos: Int,
    @SerialName("profile_id") val profileId: String,
    val username: String,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    val rp: Int = 0,
    val points: Int = 0,
    @SerialName("is_me") val isMe: Boolean = false
)

/** Linha de `ranked_last_result()`: como foi a última semana fechada. */
@Serializable
data class RankedLastResult(
    @SerialName("week_start") val weekStart: String,
    val points: Int? = null,
    val pos: Int? = null,
    @SerialName("cohort_size") val cohortSize: Int = 0,
    val delta: Int? = null,
    @SerialName("rp_after") val rpAfter: Int? = null
)

/** Linha de `ranked_my_duels()`: um x1 (pedido, em andamento ou terminado). */
@Serializable
data class RankedDuel(
    val id: String,
    val ranked: Boolean = true,
    /** pending, active, declined, cancelled ou finished. */
    val status: String,
    @SerialName("starts_at") val startsAt: String? = null,
    @SerialName("ends_at") val endsAt: String? = null,
    @SerialName("challenger_id") val challengerId: String,
    @SerialName("challenger_name") val challengerName: String,
    @SerialName("challenger_avatar") val challengerAvatar: String? = null,
    @SerialName("challenger_rp") val challengerRp: Int = 0,
    @SerialName("pt_challenger") val ptChallenger: Int? = null,
    @SerialName("opponent_id") val opponentId: String,
    @SerialName("opponent_name") val opponentName: String,
    @SerialName("opponent_avatar") val opponentAvatar: String? = null,
    @SerialName("opponent_rp") val opponentRp: Int = 0,
    @SerialName("pt_opponent") val ptOpponent: Int? = null,
    @SerialName("winner_id") val winnerId: String? = null,
    @SerialName("delta_challenger") val deltaChallenger: Int? = null,
    @SerialName("delta_opponent") val deltaOpponent: Int? = null
) {
    val isActive: Boolean get() = status == "active"
    val isPending: Boolean get() = status == "pending"
    val isFinished: Boolean get() = status == "finished"
}
