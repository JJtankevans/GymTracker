package com.vitoroliveira.gymtracker.domain.repository

import com.vitoroliveira.gymtracker.data.model.Challenge
import com.vitoroliveira.gymtracker.data.model.ChallengeParticipant
import com.vitoroliveira.gymtracker.data.model.FeedPost
import com.vitoroliveira.gymtracker.data.model.Follow
import com.vitoroliveira.gymtracker.data.model.GroupMember
import com.vitoroliveira.gymtracker.data.model.NewChallenge
import com.vitoroliveira.gymtracker.data.model.NewGroup
import com.vitoroliveira.gymtracker.data.model.PostReaction
import com.vitoroliveira.gymtracker.data.model.ProfileTitle
import com.vitoroliveira.gymtracker.data.model.ProfileStats
import com.vitoroliveira.gymtracker.data.model.TopTrack
import com.vitoroliveira.gymtracker.data.model.SocialGroup
import com.vitoroliveira.gymtracker.data.model.SocialProfile
import com.vitoroliveira.gymtracker.data.model.SocialSettings
import com.vitoroliveira.gymtracker.data.model.Standing

/** Tudo da camada social. As regras de quem pode ver o quê ficam no banco (RLS), não aqui. */
interface SocialRepository {
    fun currentUserId(): String

    // Perfil / privacidade
    suspend fun getSettings(): SocialSettings
    suspend fun saveSettings(settings: SocialSettings)

    // Pessoas
    suspend fun people(): List<SocialProfile>
    suspend fun follows(): List<Follow>
    suspend fun follow(userId: String)
    suspend fun unfollow(userId: String)
    suspend fun acceptFollower(userId: String)
    suspend fun rejectFollower(userId: String)
    suspend fun profileStats(userId: String): ProfileStats?
    suspend fun topTracks(userId: String): List<TopTrack>
    /** Títulos de desafio (honra/falha) dos perfis, quando quem consulta pode vê-los: no máximo um por perfil. */
    suspend fun titles(profileIds: List<String>): List<ProfileTitle>

    // Feed
    suspend fun feed(limit: Int = 50): List<FeedPost>
    suspend fun reactions(postIds: List<String>): List<PostReaction>
    suspend fun updatePost(postId: String, caption: String?, visibility: String)
    suspend fun addReaction(postId: String, kind: String)
    suspend fun removeReaction(postId: String, kind: String)

    // Grupos
    suspend fun myGroups(): List<SocialGroup>
    suspend fun publicGroups(): List<SocialGroup>
    suspend fun groupMembers(groupId: String): List<GroupMember>
    suspend fun createGroup(group: NewGroup): SocialGroup
    suspend fun joinGroup(groupId: String)
    suspend fun joinGroupByCode(code: String): String
    suspend fun leaveGroup(groupId: String)

    // Desafios
    suspend fun challenges(): List<Challenge>
    suspend fun myParticipations(): List<ChallengeParticipant>
    suspend fun createChallenge(challenge: NewChallenge): Challenge
    suspend fun joinChallenge(challengeId: String, team: String? = null)
    suspend fun leaveChallenge(challengeId: String)
    suspend fun standings(challengeId: String): List<Standing>
}
