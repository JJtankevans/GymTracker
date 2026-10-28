package com.vitoroliveira.gymtracker.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

// Modelos da camada social. Espelham as tabelas/funções de docs/supabase/002_social.sql.

/** Linha de `public_profiles()`: só colunas seguras, respeitando a visibilidade. */
@Serializable
data class SocialProfile(
    val id: String,
    val username: String,
    @SerialName("full_name") val fullName: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    val bio: String? = null,
    val visibility: String = "private",
    @SerialName("created_at") val createdAt: String? = null
) {
    val displayName: String get() = fullName?.takeIf { it.isNotBlank() } ?: username
}

/** Configurações de compartilhamento do próprio usuário (colunas novas de `profiles`). */
@Serializable
data class SocialSettings(
    val bio: String? = null,
    val visibility: String = "private",
    @SerialName("share_workouts") val shareWorkouts: Boolean = false,
    @SerialName("share_music") val shareMusic: Boolean = false
)

@Serializable
data class Follow(
    @SerialName("follower_id") val followerId: String,
    @SerialName("followed_id") val followedId: String,
    val status: String = "pending"
)

@Serializable
data class SocialGroup(
    val id: String,
    @SerialName("owner_id") val ownerId: String,
    val name: String,
    val description: String? = null,
    val privacy: String = "invite",
    @SerialName("invite_code") val inviteCode: String? = null,
    @SerialName("max_members") val maxMembers: Int? = null,
    @SerialName("created_at") val createdAt: String? = null
) {
    val isPublic: Boolean get() = privacy == "public"
}

@Serializable
data class NewGroup(
    @SerialName("owner_id") val ownerId: String,
    val name: String,
    val description: String?,
    val privacy: String,
    @SerialName("max_members") val maxMembers: Int?,
    /** Gerado pelo app: o INSERT não pode pedir a linha de volta (a política de leitura só vale depois do trigger que põe o dono como membro). */
    val id: String = ""
)

@Serializable
data class GroupMember(
    @SerialName("group_id") val groupId: String,
    @SerialName("profile_id") val profileId: String,
    val role: String = "member"
)

@Serializable
data class Challenge(
    val id: String,
    @SerialName("creator_id") val creatorId: String,
    @SerialName("group_id") val groupId: String? = null,
    val title: String,
    val description: String? = null,
    val metric: String,
    @SerialName("exercise_name") val exerciseName: String? = null,
    val mode: String = "individual",
    val teams: List<String> = emptyList(),
    val visibility: String = "group",
    @SerialName("invite_code") val inviteCode: String? = null,
    @SerialName("starts_at") val startsAt: String,
    @SerialName("ends_at") val endsAt: String,
    val target: Double? = null,
    @SerialName("min_minutes_per_workout") val minMinutesPerWorkout: Int = 0,
    @SerialName("max_workouts_per_day") val maxWorkoutsPerDay: Int = 1,
    @SerialName("max_participants") val maxParticipants: Int? = null,
    /** Título de falha: quem tiver MAIS que isso de treinos abaixo do mínimo de minutos (migração 010). Null = sem título de falha. */
    @SerialName("fail_threshold") val failThreshold: Int? = null,
    /** Dias seguidos para o título Alpha (2x Chad, 3x Giga Chad, 4x Mr. Olimpia). Null = sem títulos de honra. */
    @SerialName("streak_goal") val streakGoal: Int? = null
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class NewChallenge(
    @SerialName("creator_id") val creatorId: String,
    @SerialName("group_id") val groupId: String?,
    val title: String,
    val description: String?,
    val metric: String,
    @SerialName("exercise_name") val exerciseName: String?,
    val mode: String,
    val teams: List<String>,
    val visibility: String,
    @SerialName("starts_at") val startsAt: String,
    @SerialName("ends_at") val endsAt: String,
    val target: Double?,
    @SerialName("min_minutes_per_workout") val minMinutesPerWorkout: Int,
    @SerialName("max_workouts_per_day") val maxWorkoutsPerDay: Int,
    @SerialName("max_participants") val maxParticipants: Int?,
    /** Só vão no INSERT quando definidos: assim o app não quebra se a migração 010 ainda não rodou. */
    @EncodeDefault(EncodeDefault.Mode.NEVER) @SerialName("fail_threshold") val failThreshold: Int? = null,
    @EncodeDefault(EncodeDefault.Mode.NEVER) @SerialName("streak_goal") val streakGoal: Int? = null,
    /** Gerado pelo app, pelo mesmo motivo de [NewGroup.id]. */
    val id: String = ""
)

@Serializable
data class ChallengeParticipant(
    @SerialName("challenge_id") val challengeId: String,
    @SerialName("profile_id") val profileId: String,
    val team: String? = null
)

/** Linha de `challenge_standings()`. */
@Serializable
data class Standing(
    @SerialName("profile_id") val profileId: String,
    val username: String,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    val team: String? = null,
    val score: Double = 0.0,
    val rank: Long = 0
)

@Serializable
data class FeedPost(
    val id: String,
    @SerialName("profile_id") val profileId: String,
    @SerialName("workout_id") val workoutId: String,
    val caption: String? = null,
    val visibility: String = "followers",
    val stats: JsonObject = JsonObject(emptyMap()),
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class PostReaction(
    @SerialName("post_id") val postId: String,
    @SerialName("profile_id") val profileId: String,
    val kind: String
)

/** Resultado de `profile_stats()` (null quando você não pode ver o perfil). */
@Serializable
data class ProfileStats(
    val workouts: Int = 0,
    @SerialName("active_days") val activeDays: Int = 0,
    val sets: Int = 0,
    @SerialName("volume_kg") val volumeKg: Double = 0.0,
    val since: String? = null
)

/** Linha de `profile_top_tracks()`: música mais tocada nos treinos de alguém. */
@Serializable
data class TopTrack(
    val title: String,
    val artist: String? = null,
    val album: String? = null,
    val plays: Long = 0,
    @SerialName("last_played") val lastPlayed: String? = null
)

/** Linha de `profile_titles()` (migração 010): o título de desafio que o perfil exibe agora. */
@Serializable
data class ProfileTitle(
    @SerialName("profile_id") val profileId: String,
    /** "honor" (Alpha...Mr. Olimpia) ou "fail". */
    val kind: String,
    /** Honra: 1 Alpha, 2 Chad, 3 Giga Chad, 4 Mr. Olimpia. Falha: 0. */
    val level: Int = 0,
    @SerialName("challenge_id") val challengeId: String? = null,
    @SerialName("challenge_title") val challengeTitle: String? = null,
    @SerialName("streak_days") val streakDays: Int = 0,
    val failures: Int = 0
)

/** Uma linha da liga da semana (função `weekly_league()`, migração 015). */
@Serializable
data class LeagueRow(
    @SerialName("profile_id") val profileId: String,
    val username: String,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    val workouts: Int = 0,
    @SerialName("active_days") val activeDays: Int = 0,
    @SerialName("volume_kg") val volumeKg: Double = 0.0,
    val minutes: Int = 0,
    @SerialName("is_me") val isMe: Boolean = false
)
