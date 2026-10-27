package com.vitoroliveira.gymtracker.presentation.social

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vitoroliveira.gymtracker.data.model.Challenge
import com.vitoroliveira.gymtracker.data.model.FeedPost
import com.vitoroliveira.gymtracker.data.model.ProfileStats
import com.vitoroliveira.gymtracker.data.model.TopTrack
import com.vitoroliveira.gymtracker.data.model.SocialGroup
import com.vitoroliveira.gymtracker.data.model.SocialProfile
import com.vitoroliveira.gymtracker.data.model.SocialSettings
import com.vitoroliveira.gymtracker.data.model.ProfileTitle
import com.vitoroliveira.gymtracker.data.model.Standing
import com.vitoroliveira.gymtracker.domain.repository.SocialRepository
import com.vitoroliveira.gymtracker.presentation.toUserMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.time.Clock

data class FeedItem(
    val post: FeedPost, val author: String, val isMine: Boolean, val reactions: ReactionSummary,
    /** Foto de quem postou (null = mostra a inicial). */
    val avatarUrl: String? = null
)

enum class Relation { NONE, FOLLOWING, REQUESTED }

data class PersonItem(val profile: SocialProfile, val relation: Relation, val followsMe: Boolean)

data class ChallengeItem(
    val challenge: Challenge,
    val joined: Boolean,
    val status: ChallengeStatus,
    val daysLeft: Int,
    val groupName: String?,
    val metric: ChallengeMetric?
)

data class ProfileView(
    val person: PersonItem,
    val stats: ProfileStats? = null,
    val tracks: List<TopTrack> = emptyList(),
    val isLoading: Boolean = true
)

data class SocialUiState(
    val isLoading: Boolean = false,
    val me: String? = null,
    val settings: SocialSettings? = null,
    val feed: List<FeedItem> = emptyList(),
    val people: List<PersonItem> = emptyList(),
    /** Quem pediu pra me seguir e espera eu aceitar. */
    val followRequests: List<SocialProfile> = emptyList(),
    val myGroups: List<SocialGroup> = emptyList(),
    val publicGroups: List<SocialGroup> = emptyList(),
    val challenges: List<ChallengeItem> = emptyList(),
    val standings: Map<String, List<Standing>> = emptyMap(),
    /** Título de desafio de cada perfil (por id), incluindo o meu. Vazio se a migração 010 não rodou. */
    val titles: Map<String, ProfileTitle> = emptyMap(),
    /** Meu perfil público (foto e nome nos meus posts). */
    val myProfile: SocialProfile? = null,
    val profileView: ProfileView? = null,
    val message: String? = null,
    val error: String? = null,
    /** A última atualização falhou e não há nada na tela (guarda a mensagem pra mostrar "Tentar de novo" em vez de "vazio"). */
    val loadError: String? = null
)

class SocialViewModel(
    private val repo: SocialRepository,
    private val clock: Clock = Clock.System
) : ViewModel() {

    private val _state = MutableStateFlow(SocialUiState())
    val state: StateFlow<SocialUiState> = _state.asStateFlow()

    init { refresh() }

    private fun update(block: SocialUiState.() -> SocialUiState) { _state.value = _state.value.block() }

    /** Roda uma ação; mostra a mensagem de sucesso ou o erro em português. */
    private fun act(errorFallback: String, success: String? = null, then: suspend () -> Unit = {}, block: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                block()
                then()
                if (success != null) update { copy(message = success) }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                update { copy(error = e.toUserMessage(errorFallback)) }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            update { copy(isLoading = true, error = null) }
            val me = runCatching { repo.currentUserId() }.getOrNull()
            update { copy(me = me) }
            // cada seção carrega sozinha: se uma falhar (ex.: tabela ainda não criada), as outras seguem
            val errors = mutableListOf<String>()
            suspend fun section(name: String, block: suspend () -> Unit) {
                try { block() } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    errors += e.toUserMessage("Erro ao carregar $name")
                }
            }
            section("seu perfil social") { loadSettings() }
            section("as pessoas") { loadPeople() }
            loadTitlesQuietly()
            section("o feed") { loadFeed() }
            section("os grupos") { loadGroups() }
            section("os desafios") { loadChallenges() }
            update {
                val nothing = feed.isEmpty() && people.isEmpty() && myGroups.isEmpty() && publicGroups.isEmpty() && challenges.isEmpty()
                copy(isLoading = false, error = errors.firstOrNull(), loadError = if (nothing) errors.firstOrNull() else null)
            }
        }
    }

    private suspend fun loadSettings() { val s = repo.getSettings(); update { copy(settings = s) } }

    private suspend fun loadPeople() {
        val me = repo.currentUserId()
        val all = repo.people()
        val profiles = all.filter { it.id != me }
        update { copy(myProfile = all.firstOrNull { it.id == me }) }
        val follows = repo.follows()
        val items = profiles.map { p ->
            val mine = follows.firstOrNull { it.followerId == me && it.followedId == p.id }
            val theirs = follows.firstOrNull { it.followerId == p.id && it.followedId == me }
            PersonItem(
                p,
                when (mine?.status) { "accepted" -> Relation.FOLLOWING; "pending" -> Relation.REQUESTED; else -> Relation.NONE },
                followsMe = theirs?.status == "accepted"
            )
        }
        val requests = follows.filter { it.followedId == me && it.status == "pending" }
            .mapNotNull { f -> profiles.firstOrNull { it.id == f.followerId } }
        update { copy(people = items, followRequests = requests) }
    }

    /** Títulos são um enfeite: se falhar (ex.: migração 010 ainda não rodou), a tela segue sem eles e sem mensagem de erro. */
    private suspend fun loadTitlesQuietly() {
        try {
            val ids = _state.value.people.map { it.profile.id } + listOfNotNull(_state.value.me)
            val titles = repo.titles(ids).associateBy { it.profileId }
            update { copy(titles = titles) }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
        }
    }

    /** Junta os títulos de mais perfis (ex.: participantes de um desafio que eu não sigo). Silencioso como [loadTitlesQuietly]. */
    private suspend fun mergeTitlesQuietly(ids: List<String>) {
        val missing = ids.filter { it !in _state.value.titles }
        if (missing.isEmpty()) return
        try {
            val more = repo.titles(missing).associateBy { it.profileId }
            update { copy(titles = titles + more) }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
        }
    }

    private suspend fun loadFeed() {
        val me = repo.currentUserId()
        val posts = repo.feed()
        val reactions = repo.reactions(posts.map { it.id })
        val names = _state.value.people.associate { it.profile.id to it.profile.displayName }
        val avatars = _state.value.people.associate { it.profile.id to it.profile.avatarUrl }
        update {
            copy(feed = posts.map { p ->
                FeedItem(
                    post = p,
                    author = if (p.profileId == me) "Você" else names[p.profileId] ?: "Alguém",
                    isMine = p.profileId == me,
                    reactions = summarizeReactions(p.id, reactions, me),
                    avatarUrl = if (p.profileId == me) _state.value.myProfile?.avatarUrl else avatars[p.profileId]
                )
            })
        }
    }

    private suspend fun loadGroups() {
        val mine = repo.myGroups()
        val public = repo.publicGroups().filter { g -> mine.none { it.id == g.id } }
        update { copy(myGroups = mine, publicGroups = public) }
    }

    private suspend fun loadChallenges() {
        val list = repo.challenges()
        val joined = repo.myParticipations().map { it.challengeId }.toSet()
        val groups = (_state.value.myGroups + _state.value.publicGroups).associate { it.id to it.name }
        val now = clock.now()
        update {
            copy(challenges = list.map { c ->
                ChallengeItem(c, c.id in joined, challengeStatus(c, now), daysLeft(c, now), c.groupId?.let { groups[it] }, ChallengeMetric.fromKey(c.metric))
            }.sortedWith(compareBy<ChallengeItem> { it.status == ChallengeStatus.ENDED }.thenByDescending { it.challenge.startsAt }))
        }
    }

    // ── privacidade ──

    fun saveSettings(s: SocialSettings) {
        val bio = s.bio?.trim()?.takeIf { it.isNotEmpty() }
        if (bio != null && bio.length > 280) { update { copy(error = "A bio passou de 280 caracteres.") }; return }
        act("Erro ao salvar as configurações", "Configurações salvas", then = { loadSettings() }) {
            repo.saveSettings(s.copy(bio = bio))
        }
    }

    // ── pessoas ──

    fun follow(id: String) = act("Erro ao seguir", then = { loadPeople() }) { repo.follow(id) }
    fun unfollow(id: String) = act("Erro ao deixar de seguir", then = { loadPeople() }) { repo.unfollow(id) }
    fun acceptFollower(id: String) = act("Erro ao aceitar", "Pedido aceito", then = { loadPeople() }) { repo.acceptFollower(id) }
    fun rejectFollower(id: String) = act("Erro ao recusar", then = { loadPeople() }) { repo.rejectFollower(id) }

    // ── feed ──

    /** Liga/desliga a reação do usuário. Atualiza a lista na hora e desfaz se o servidor recusar. */
    fun toggleReaction(postId: String, kind: ReactionKind) {
        val item = _state.value.feed.firstOrNull { it.post.id == postId } ?: return
        val had = kind in item.reactions.mine
        act("Erro ao reagir", then = { loadFeed() }) {
            if (had) repo.removeReaction(postId, kind.key) else repo.addReaction(postId, kind.key)
        }
    }

    /** Edita a legenda e a visibilidade de um post seu (as estatísticas não mudam). */
    fun editPost(postId: String, caption: String?, visibility: String) {
        val c = caption?.trim()?.takeIf { it.isNotEmpty() }
        if (c != null && c.length > 280) { update { copy(error = "A legenda passou de 280 caracteres.") }; return }
        if (visibility !in listOf("public", "followers", "private")) { update { copy(error = "Visibilidade inválida.") }; return }
        act("Erro ao salvar o post", "Post atualizado", then = { loadFeed() }) { repo.updatePost(postId, c, visibility) }
    }

    // ── grupos ──

    fun createGroup(form: GroupForm) {
        val me = _state.value.me ?: return
        val group = try { form.toNew(me) } catch (e: IllegalArgumentException) {
            update { copy(error = e.message) }; return
        }
        act("Erro ao criar o grupo", "Grupo '${group.name}' criado", then = { loadGroups() }) { repo.createGroup(group) }
    }

    fun joinGroup(id: String) = act("Erro ao entrar no grupo", "Você entrou no grupo", then = { loadGroups() }) { repo.joinGroup(id) }

    fun joinGroupByCode(code: String) {
        if (code.isBlank()) { update { copy(error = "Digite o código do convite.") }; return }
        act("Erro ao entrar com o código", "Você entrou no grupo", then = { loadGroups() }) { repo.joinGroupByCode(code.trim()) }
    }

    fun leaveGroup(id: String) = act("Erro ao sair do grupo", "Você saiu do grupo", then = { loadGroups(); loadChallenges() }) { repo.leaveGroup(id) }

    // ── desafios ──

    fun createChallenge(form: ChallengeForm) {
        val me = _state.value.me ?: return
        val new = try { form.toNew(me, clock.now()) } catch (e: IllegalArgumentException) {
            update { copy(error = e.message) }; return
        }
        act("Erro ao criar o desafio", "Desafio '${new.title}' criado", then = { loadChallenges() }) {
            val created = repo.createChallenge(new)
            // quem cria entra no próprio desafio
            runCatching { repo.joinChallenge(created.id, new.teams.firstOrNull()) }
        }
    }

    fun joinChallenge(id: String, team: String? = null) = act("Erro ao entrar no desafio", "Você entrou no desafio", then = { loadChallenges(); loadStandings(id) }) {
        repo.joinChallenge(id, team)
    }

    fun leaveChallenge(id: String) = act("Erro ao sair do desafio", "Você saiu do desafio", then = { loadChallenges() }) { repo.leaveChallenge(id) }

    fun loadStandings(id: String) {
        viewModelScope.launch {
            try {
                val s = repo.standings(id)
                update { copy(standings = standings + (id to s)) }
                mergeTitlesQuietly(s.map { it.profileId })
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                update { copy(error = e.toUserMessage("Erro ao carregar o placar")) }
            }
        }
    }

    fun clearMessages() = update { copy(message = null, error = null) }

    // ── perfil de outra pessoa ──

    fun openProfile(person: PersonItem) {
        update { copy(profileView = ProfileView(person)) }
        viewModelScope.launch {
            // estatísticas e músicas são independentes: se uma falhar (ex.: pessoa não liberou música) a outra aparece
            val stats = try { repo.profileStats(person.profile.id) } catch (e: Exception) { if (e is CancellationException) throw e; null }
            val tracks = try { repo.topTracks(person.profile.id) } catch (e: Exception) { if (e is CancellationException) throw e; emptyList() }
            update {
                // só aplica se ainda é o mesmo perfil aberto
                if (profileView?.person?.profile?.id == person.profile.id)
                    copy(profileView = profileView.copy(stats = stats, tracks = tracks, isLoading = false))
                else this
            }
        }
    }

    fun closeProfile() = update { copy(profileView = null) }
}
