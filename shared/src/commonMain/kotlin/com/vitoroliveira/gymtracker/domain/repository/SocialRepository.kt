package com.vitoroliveira.gymtracker.domain.repository

import com.vitoroliveira.gymtracker.data.model.Challenge
import com.vitoroliveira.gymtracker.data.model.ChallengeParticipant
import com.vitoroliveira.gymtracker.data.model.FeedPost
import com.vitoroliveira.gymtracker.data.model.Follow
import com.vitoroliveira.gymtracker.data.model.GroupEdit
import com.vitoroliveira.gymtracker.data.model.GroupMember
import com.vitoroliveira.gymtracker.data.model.GroupMemberRow
import com.vitoroliveira.gymtracker.data.model.GroupMessage
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
    /** Liga da semana: eu + quem eu sigo, ordenado (migração 015). Vazio por padrão (fakes e banco sem a função). */
    suspend fun weeklyLeague(): List<com.vitoroliveira.gymtracker.data.model.LeagueRow> = emptyList()
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
    /** O dono muda identidade, aviso fixado e meta semanal (migração 017). */
    suspend fun updateGroup(groupId: String, edit: GroupEdit) {
        throw UnsupportedOperationException("Editar o grupo não é possível aqui")
    }
    /** Treinos dos membros na semana (migração 017); null sem a função no banco. */
    suspend fun groupProgress(groupId: String): com.vitoroliveira.gymtracker.presentation.social.GroupWeekProgress? = null
    /** Ranking dos membros na semana (migração 019); vazio sem a função. */
    suspend fun groupBoard(groupId: String): List<GroupMemberRow> = emptyList()
    /** Semanas seguidas batendo a meta do grupo (migração 019). */
    suspend fun groupStreak(groupId: String): Int = 0
    /** O dono muda o cargo de um membro (admin ou member). */
    suspend fun setMemberRole(groupId: String, profileId: String, role: String) {
        throw UnsupportedOperationException("Mudar cargo não é possível aqui")
    }
    /** O dono ou um admin tira alguém do grupo. */
    suspend fun removeMember(groupId: String, profileId: String) {
        throw UnsupportedOperationException("Remover membro não é possível aqui")
    }
    /** Mural do grupo, do mais novo para o mais antigo (migração 019). */
    suspend fun groupMessages(groupId: String): List<GroupMessage> = emptyList()
    suspend fun postGroupMessage(groupId: String, body: String) {
        throw UnsupportedOperationException("Mural não é possível aqui")
    }
    suspend fun deleteGroupMessage(messageId: String) {
        throw UnsupportedOperationException("Mural não é possível aqui")
    }

    // Desafios
    suspend fun challenges(): List<Challenge>
    suspend fun myParticipations(): List<ChallengeParticipant>
    suspend fun createChallenge(challenge: NewChallenge): Challenge
    suspend fun joinChallenge(challengeId: String, team: String? = null)
    suspend fun leaveChallenge(challengeId: String)
    suspend fun standings(challengeId: String): List<Standing>
}
