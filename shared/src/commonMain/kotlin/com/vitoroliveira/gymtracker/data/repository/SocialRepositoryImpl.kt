package com.vitoroliveira.gymtracker.data.repository

import com.vitoroliveira.gymtracker.data.model.Challenge
import com.vitoroliveira.gymtracker.data.model.ChallengeParticipant
import com.vitoroliveira.gymtracker.data.model.FeedPost
import com.vitoroliveira.gymtracker.data.model.Follow
import com.vitoroliveira.gymtracker.data.model.GroupMember
import com.vitoroliveira.gymtracker.data.model.NewChallenge
import com.vitoroliveira.gymtracker.data.model.NewGroup
import com.vitoroliveira.gymtracker.data.model.PostReaction
import com.vitoroliveira.gymtracker.data.model.ProfileStats
import com.vitoroliveira.gymtracker.data.model.TopTrack
import com.vitoroliveira.gymtracker.data.model.SocialGroup
import com.vitoroliveira.gymtracker.data.model.SocialProfile
import com.vitoroliveira.gymtracker.data.model.SocialSettings
import com.vitoroliveira.gymtracker.data.model.Standing
import com.vitoroliveira.gymtracker.domain.repository.SocialRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class SocialRepositoryImpl(private val supabase: SupabaseClient) : SocialRepository {

    private fun me(): String = supabase.auth.currentUserOrNull()?.id
        ?: throw IllegalStateException("Usuário não autenticado")

    override fun currentUserId(): String = me()

    // ── perfil ──

    override suspend fun getSettings(): SocialSettings =
        supabase.postgrest["profiles"]
            .select(Columns.list("bio", "visibility", "share_workouts", "share_music")) {
                filter { eq("id", me()) }
            }
            .decodeSingle()

    override suspend fun saveSettings(settings: SocialSettings) {
        // set(...) explícito: assim "false" e "null" sempre vão pro banco (valores padrão podem ser omitidos pelo serializador)
        supabase.postgrest["profiles"].update({
            set("bio", settings.bio)
            set("visibility", settings.visibility)
            set("share_workouts", settings.shareWorkouts)
            set("share_music", settings.shareMusic)
        }) { filter { eq("id", me()) } }
    }

    // ── pessoas ──

    override suspend fun people(): List<SocialProfile> =
        supabase.postgrest.rpc("public_profiles").decodeList()

    override suspend fun follows(): List<Follow> =
        supabase.postgrest["follows"].select().decodeList()

    override suspend fun follow(userId: String) {
        supabase.postgrest["follows"].insert(Follow(followerId = me(), followedId = userId))
    }

    override suspend fun unfollow(userId: String) {
        supabase.postgrest["follows"].delete {
            filter { eq("follower_id", me()); eq("followed_id", userId) }
        }
    }

    override suspend fun acceptFollower(userId: String) {
        supabase.postgrest["follows"].update({ set("status", "accepted") }) {
            filter { eq("follower_id", userId); eq("followed_id", me()) }
        }
    }

    override suspend fun rejectFollower(userId: String) {
        supabase.postgrest["follows"].delete {
            filter { eq("follower_id", userId); eq("followed_id", me()) }
        }
    }

    override suspend fun profileStats(userId: String): ProfileStats? =
        supabase.postgrest.rpc("profile_stats", buildJsonObject { put("p_profile", userId) }).decodeAsOrNull<ProfileStats>()

    override suspend fun topTracks(userId: String): List<TopTrack> =
        supabase.postgrest.rpc("profile_top_tracks", buildJsonObject { put("p_profile", userId); put("p_limit", 10) }).decodeList()

    // ── feed ──

    override suspend fun feed(limit: Int): List<FeedPost> =
        supabase.postgrest["feed_posts"].select {
            order("created_at", Order.DESCENDING)
            limit(limit.toLong())
        }.decodeList()

    override suspend fun reactions(postIds: List<String>): List<PostReaction> {
        if (postIds.isEmpty()) return emptyList()
        return supabase.postgrest["post_reactions"].select {
            filter { isIn("post_id", postIds) }
        }.decodeList()
    }

    override suspend fun updatePost(postId: String, caption: String?, visibility: String) {
        // só caption e visibility são editáveis (o banco bloqueia o resto)
        supabase.postgrest["feed_posts"].update({
            set("caption", caption)
            set("visibility", visibility)
        }) { filter { eq("id", postId) } }
    }

    override suspend fun addReaction(postId: String, kind: String) {
        supabase.postgrest["post_reactions"].insert(PostReaction(postId, me(), kind))
    }

    override suspend fun removeReaction(postId: String, kind: String) {
        supabase.postgrest["post_reactions"].delete {
            filter { eq("post_id", postId); eq("profile_id", me()); eq("kind", kind) }
        }
    }

    // ── grupos ──

    override suspend fun myGroups(): List<SocialGroup> {
        val ids = supabase.postgrest["group_members"]
            .select { filter { eq("profile_id", me()) } }
            .decodeList<GroupMember>().map { it.groupId }
        if (ids.isEmpty()) return emptyList()
        return supabase.postgrest["groups"].select {
            filter { isIn("id", ids) }
            order("created_at", Order.DESCENDING)
        }.decodeList()
    }

    override suspend fun publicGroups(): List<SocialGroup> =
        supabase.postgrest["groups"].select {
            filter { eq("privacy", "public") }
            order("created_at", Order.DESCENDING)
            limit(50)
        }.decodeList()

    override suspend fun groupMembers(groupId: String): List<GroupMember> =
        supabase.postgrest["group_members"].select { filter { eq("group_id", groupId) } }.decodeList()

    override suspend fun createGroup(group: NewGroup): SocialGroup =
        supabase.postgrest["groups"].insert(group) { select() }.decodeSingle()

    override suspend fun joinGroup(groupId: String) {
        supabase.postgrest.rpc("join_group", buildJsonObject { put("p_group", groupId) })
    }

    override suspend fun joinGroupByCode(code: String): String =
        supabase.postgrest.rpc("join_group_by_code", buildJsonObject { put("p_code", code) }).decodeAs()

    override suspend fun leaveGroup(groupId: String) {
        supabase.postgrest["group_members"].delete {
            filter { eq("group_id", groupId); eq("profile_id", me()) }
        }
    }

    // ── desafios ──

    override suspend fun challenges(): List<Challenge> =
        supabase.postgrest["challenges"].select {
            order("starts_at", Order.DESCENDING)
            limit(50)
        }.decodeList()

    override suspend fun myParticipations(): List<ChallengeParticipant> =
        supabase.postgrest["challenge_participants"].select { filter { eq("profile_id", me()) } }.decodeList()

    override suspend fun createChallenge(challenge: NewChallenge): Challenge =
        supabase.postgrest["challenges"].insert(challenge) { select() }.decodeSingle()

    override suspend fun joinChallenge(challengeId: String, team: String?) {
        supabase.postgrest.rpc("join_challenge", buildJsonObject {
            put("p_challenge", challengeId)
            if (team != null) put("p_team", team)
        })
    }

    override suspend fun leaveChallenge(challengeId: String) {
        supabase.postgrest["challenge_participants"].delete {
            filter { eq("challenge_id", challengeId); eq("profile_id", me()) }
        }
    }

    override suspend fun standings(challengeId: String): List<Standing> =
        supabase.postgrest.rpc("challenge_standings", buildJsonObject { put("p_challenge", challengeId) }).decodeList()
}
