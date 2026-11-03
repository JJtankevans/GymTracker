package com.vitoroliveira.gymtracker.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Linha de `group_week_board()` (migração 019): um membro do grupo com a semana dele. */
@Serializable
data class GroupMemberRow(
    @SerialName("profile_id") val profileId: String,
    val username: String,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    /** owner, admin ou member. */
    val role: String = "member",
    val workouts: Int = 0,
    @SerialName("active_days") val activeDays: Int = 0,
    val minutes: Int = 0,
    @SerialName("is_me") val isMe: Boolean = false
)

/** Mensagem do mural do grupo (`group_messages`, migração 019). */
@Serializable
data class GroupMessage(
    val id: String,
    @SerialName("group_id") val groupId: String,
    @SerialName("profile_id") val profileId: String,
    val body: String,
    @SerialName("created_at") val createdAt: String
)

/** O que vai no INSERT do mural (o autor vem do banco: `profile_id` default auth.uid()). */
@Serializable
data class NewGroupMessage(@SerialName("group_id") val groupId: String, val body: String)
