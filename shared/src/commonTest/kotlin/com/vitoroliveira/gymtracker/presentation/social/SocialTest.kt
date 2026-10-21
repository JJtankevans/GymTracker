package com.vitoroliveira.gymtracker.presentation.social

import com.vitoroliveira.gymtracker.data.model.Challenge
import com.vitoroliveira.gymtracker.data.model.ChallengeParticipant
import com.vitoroliveira.gymtracker.data.model.FeedPost
import com.vitoroliveira.gymtracker.data.model.Follow
import com.vitoroliveira.gymtracker.data.model.GroupMember
import com.vitoroliveira.gymtracker.data.model.NewChallenge
import com.vitoroliveira.gymtracker.data.model.NewGroup
import com.vitoroliveira.gymtracker.data.model.PostReaction
import com.vitoroliveira.gymtracker.data.model.SocialGroup
import com.vitoroliveira.gymtracker.data.model.SocialProfile
import com.vitoroliveira.gymtracker.data.model.SocialSettings
import com.vitoroliveira.gymtracker.data.model.Standing
import com.vitoroliveira.gymtracker.domain.repository.SocialRepository
import com.vitoroliveira.gymtracker.presentation.viewmodel.FixedClock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

class FakeSocialRepository(val meId: String = "me") : SocialRepository {
    var settings = SocialSettings()
    var profiles = mutableListOf<SocialProfile>()
    var followList = mutableListOf<Follow>()
    var posts = mutableListOf<FeedPost>()
    var reactionList = mutableListOf<PostReaction>()
    var groups = mutableListOf<SocialGroup>()
    var members = mutableListOf<GroupMember>()
    var challengeList = mutableListOf<Challenge>()
    var participations = mutableListOf<ChallengeParticipant>()
    var failFeed = false
    var standingsFor = mutableMapOf<String, List<Standing>>()
    val calls = mutableListOf<String>()

    override fun currentUserId() = meId
    override suspend fun getSettings() = settings
    override suspend fun saveSettings(settings: SocialSettings) { this.settings = settings; calls += "save" }
    override suspend fun people() = profiles.toList()
    override suspend fun follows() = followList.toList()
    override suspend fun follow(userId: String) { followList += Follow(meId, userId, "pending"); calls += "follow:$userId" }
    override suspend fun unfollow(userId: String) { followList.removeAll { it.followerId == meId && it.followedId == userId } }
    override suspend fun acceptFollower(userId: String) {
        followList.replaceAll { if (it.followerId == userId && it.followedId == meId) it.copy(status = "accepted") else it }
    }
    override suspend fun rejectFollower(userId: String) { followList.removeAll { it.followerId == userId && it.followedId == meId } }
    var titleList = mutableListOf<com.vitoroliveira.gymtracker.data.model.ProfileTitle>()
    var failTitles = false
    override suspend fun titles(profileIds: List<String>): List<com.vitoroliveira.gymtracker.data.model.ProfileTitle> {
        if (failTitles) throw UnsupportedOperationException("profile_titles indisponível")
        return titleList.filter { it.profileId in profileIds }
    }
    var statsFor = mutableMapOf<String, com.vitoroliveira.gymtracker.data.model.ProfileStats>()
    var tracksFor = mutableMapOf<String, List<com.vitoroliveira.gymtracker.data.model.TopTrack>>()
    var failTracks = false
    override suspend fun profileStats(userId: String) = statsFor[userId]
    override suspend fun topTracks(userId: String): List<com.vitoroliveira.gymtracker.data.model.TopTrack> {
        if (failTracks) throw RuntimeException("permission denied"); return tracksFor[userId].orEmpty()
    }
    override suspend fun feed(limit: Int): List<FeedPost> { if (failFeed) throw RuntimeException("Unable to resolve host"); return posts.toList() }
    override suspend fun reactions(postIds: List<String>) = reactionList.filter { it.postId in postIds }
    override suspend fun updatePost(postId: String, caption: String?, visibility: String) {
        posts.replaceAll { if (it.id == postId) it.copy(caption = caption, visibility = visibility) else it }
    }
    override suspend fun addReaction(postId: String, kind: String) { reactionList += PostReaction(postId, meId, kind) }
    override suspend fun removeReaction(postId: String, kind: String) {
        reactionList.removeAll { it.postId == postId && it.profileId == meId && it.kind == kind }
    }
    override suspend fun myGroups() = groups.filter { g -> members.any { it.groupId == g.id && it.profileId == meId } }
    override suspend fun publicGroups() = groups.filter { it.isPublic }
    override suspend fun groupMembers(groupId: String) = members.filter { it.groupId == groupId }
    override suspend fun createGroup(group: NewGroup): SocialGroup {
        val g = SocialGroup("g${groups.size + 1}", group.ownerId, group.name, group.description, group.privacy, "code1234", group.maxMembers)
        groups += g; members += GroupMember(g.id, meId, "owner"); return g
    }
    override suspend fun joinGroup(groupId: String) { members += GroupMember(groupId, meId) }
    override suspend fun joinGroupByCode(code: String): String {
        val g = groups.firstOrNull { it.inviteCode == code } ?: throw RuntimeException("código inválido")
        members += GroupMember(g.id, meId); return g.id
    }
    override suspend fun leaveGroup(groupId: String) { members.removeAll { it.groupId == groupId && it.profileId == meId } }
    override suspend fun challenges() = challengeList.toList()
    override suspend fun myParticipations() = participations.filter { it.profileId == meId }
    override suspend fun createChallenge(challenge: NewChallenge): Challenge {
        val c = Challenge(
            "c${challengeList.size + 1}", challenge.creatorId, challenge.groupId, challenge.title, challenge.description,
            challenge.metric, challenge.exerciseName, challenge.mode, challenge.teams, challenge.visibility, "inv",
            challenge.startsAt, challenge.endsAt, challenge.target, challenge.minMinutesPerWorkout, challenge.maxWorkoutsPerDay, null
        )
        challengeList += c; return c
    }
    override suspend fun joinChallenge(challengeId: String, team: String?) { participations += ChallengeParticipant(challengeId, meId, team) }
    override suspend fun leaveChallenge(challengeId: String) { participations.removeAll { it.challengeId == challengeId && it.profileId == meId } }
    override suspend fun standings(challengeId: String) = standingsFor[challengeId].orEmpty()
}

private val NOW = Instant.parse("2026-10-20T12:00:00Z")

private fun challenge(id: String, start: String, end: String, metric: String = "workouts") =
    Challenge(id = id, creatorId = "x", title = "Desafio $id", metric = metric, startsAt = start, endsAt = end)

@OptIn(ExperimentalCoroutinesApi::class)
class SocialTest {
    @BeforeTest fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @AfterTest fun tearDown() = Dispatchers.resetMain()

    private fun vm(repo: FakeSocialRepository) = SocialViewModel(repo, FixedClock(NOW))

    // ── regras puras ──

    @Test fun formataNumerosComPontoDeMilhar() {
        assertEquals("5.200", formatInt(5200.0))
        assertEquals("12", formatInt(12.0))
        assertEquals("1.234.567", formatInt(1234567.0))
        assertEquals("0", formatInt(0.0))
    }

    @Test fun metricaFormataComUnidade() {
        assertEquals("5.200 kg", ChallengeMetric.VOLUME.format(5200.0))
        assertEquals("3 treinos", ChallengeMetric.WORKOUTS.format(3.0))
        assertEquals("1 treino", ChallengeMetric.WORKOUTS.format(1.0))
        assertEquals("1 dia", ChallengeMetric.ACTIVE_DAYS.format(1.0))
        assertEquals("1 kg", ChallengeMetric.VOLUME.format(1.0))
        assertTrue(ChallengeMetric.EXERCISE_MAX.needsExercise)
        assertEquals(ChallengeMetric.MINUTES, ChallengeMetric.fromKey("training_minutes"))
        assertNull(ChallengeMetric.fromKey("inexistente"))
    }

    @Test fun statusDoDesafio() {
        assertEquals(ChallengeStatus.UPCOMING, challengeStatus(challenge("a", "2026-10-25T00:00:00Z", "2026-11-01T00:00:00Z"), NOW))
        assertEquals(ChallengeStatus.ACTIVE, challengeStatus(challenge("a", "2026-10-15T00:00:00Z", "2026-11-01T00:00:00Z"), NOW))
        assertEquals(ChallengeStatus.ENDED, challengeStatus(challenge("a", "2026-10-01T00:00:00Z", "2026-10-10T00:00:00Z"), NOW))
    }

    @Test fun diasQueFaltamArredondaPraCima() {
        assertEquals(2, daysLeft(challenge("a", "2026-10-01T00:00:00Z", "2026-10-22T06:00:00Z"), NOW))
        assertEquals(0, daysLeft(challenge("a", "2026-10-01T00:00:00Z", "2026-10-10T00:00:00Z"), NOW))
    }

    @Test fun formularioDeDesafioValida() {
        val ok = ChallengeForm("Mês do supino", ChallengeMetric.EXERCISE_MAX, "Supino Reto com Barra", days = 30).toNew("me", NOW)
        assertEquals("exercise_max_weight", ok.metric)
        assertEquals("Supino Reto com Barra", ok.exerciseName)
        assertEquals("2026-11-19T12:00:00Z", ok.endsAt)
        assertEquals("public", ok.visibility)
        assertFailsWith<IllegalArgumentException> { ChallengeForm("ab", ChallengeMetric.WORKOUTS).toNew("me", NOW) }
        assertFailsWith<IllegalArgumentException> { ChallengeForm("Sem exercício", ChallengeMetric.EXERCISE_VOLUME).toNew("me", NOW) }
        assertFailsWith<IllegalArgumentException> { ChallengeForm("Dias zero", ChallengeMetric.WORKOUTS, days = 0).toNew("me", NOW) }
    }

    @Test fun metricaSemExercicioDescartaONomeDoExercicio() {
        val c = ChallengeForm("Treinos do mês", ChallengeMetric.WORKOUTS, exerciseName = "Supino", groupId = "g1").toNew("me", NOW)
        assertNull(c.exerciseName) // o banco exige exercise_name nulo nessas métricas
        assertEquals("group", c.visibility)
    }

    @Test fun desafioEmEquipes() {
        val c = ChallengeForm("Time contra time", ChallengeMetric.VOLUME, teams = listOf(" Vermelhos ", "azuis", "VERMELHOS")).toNew("me", NOW)
        assertEquals("team", c.mode)
        assertEquals(listOf("Vermelhos", "azuis"), c.teams) // tira espaço e repetido (sem diferenciar caixa)
        assertEquals("individual", ChallengeForm("Solo", ChallengeMetric.VOLUME).toNew("me", NOW).mode)
        assertFailsWith<IllegalArgumentException> { ChallengeForm("Uma só", ChallengeMetric.VOLUME, teams = listOf("A")).toNew("me", NOW) }
        assertFailsWith<IllegalArgumentException> { ChallengeForm("Muitas", ChallengeMetric.VOLUME, teams = List(7) { "T$it" }).toNew("me", NOW) }
        assertFailsWith<IllegalArgumentException> { ChallengeForm("Nome grande", ChallengeMetric.VOLUME, teams = listOf("A", "x".repeat(21))).toNew("me", NOW) }
    }

    @Test fun totalPorEquipe() {
        val s = listOf(
            Standing("a", "ana", team = "Azuis", score = 100.0), Standing("b", "beto", team = "Azuis", score = 50.0),
            Standing("c", "cris", team = "Vermelhos", score = 120.0), Standing("d", "dani", team = null, score = 999.0)
        )
        assertEquals(listOf("Azuis" to 150.0, "Vermelhos" to 120.0), teamTotals(s, ChallengeMetric.VOLUME))
        // na maior carga conta o melhor da equipe, não a soma
        assertEquals(listOf("Vermelhos" to 120.0, "Azuis" to 100.0), teamTotals(s, ChallengeMetric.EXERCISE_MAX))
    }

    @Test fun progressoAteAMeta() {
        assertEquals(0.5f, progressToTarget(50.0, 100.0))
        assertEquals(1f, progressToTarget(250.0, 100.0)) // passou da meta: trava em 100%
        assertEquals(0f, progressToTarget(-5.0, 100.0))
        assertNull(progressToTarget(50.0, null))
        assertNull(progressToTarget(50.0, 0.0))
    }

    @Test fun entrarEmDesafioDeEquipeGuardaAEquipe() {
        val repo = FakeSocialRepository().apply {
            challengeList += challenge("c1", "2026-10-15T00:00:00Z", "2026-11-01T00:00:00Z")
        }
        val vm = vm(repo)
        vm.joinChallenge("c1", "Azuis")
        assertEquals("Azuis", repo.participations.single().team)
    }

    @Test fun criadorDeDesafioEmEquipesEntraNaPrimeira() {
        val repo = FakeSocialRepository()
        val vm = vm(repo)
        vm.createChallenge(ChallengeForm("Equipes", ChallengeMetric.WORKOUTS, teams = listOf("Azuis", "Vermelhos")))
        assertEquals("Azuis", repo.participations.single().team)
    }

    @Test fun formularioDeGrupoValida() {
        assertEquals("invite", GroupForm("Turma da manhã").toNew("me").privacy)
        assertEquals("public", GroupForm("Turma da manhã", isPublic = true).toNew("me").privacy)
        assertFailsWith<IllegalArgumentException> { GroupForm("a").toNew("me") }
        assertFailsWith<IllegalArgumentException> { GroupForm("Grupo", maxMembers = 1).toNew("me") }
    }

    @Test fun resumoDasReacoes() {
        val all = listOf(
            PostReaction("p1", "me", "fire"), PostReaction("p1", "ana", "fire"), PostReaction("p1", "ana", "flex"),
            PostReaction("p2", "me", "clap")
        )
        val s = summarizeReactions("p1", all, "me")
        assertEquals(2, s.count(ReactionKind.FIRE))
        assertEquals(1, s.count(ReactionKind.FLEX))
        assertEquals(0, s.count(ReactionKind.CLAP))
        assertEquals(setOf(ReactionKind.FIRE), s.mine)
    }

    @Test fun textoDasEstatisticasDoFeed() {
        val stats = JsonObject(mapOf("minutes" to JsonPrimitive(45), "sets" to JsonPrimitive(18), "volume_kg" to JsonPrimitive(5200.5)))
        assertEquals("45 min · 18 séries · 5.200 kg", formatFeedStats(stats))
        assertEquals("1 min · 1 série · 96 kg", formatFeedStats(JsonObject(mapOf("minutes" to JsonPrimitive(1), "sets" to JsonPrimitive(1), "volume_kg" to JsonPrimitive(96)))))
        assertEquals("", formatFeedStats(JsonObject(emptyMap())))
        assertEquals("Peito", feedGroupName(JsonObject(mapOf("group" to JsonPrimitive("Peito")))))
    }

    // ── ViewModel ──

    @Test fun carregaPessoasComRelacaoEPedidos() {
        val repo = FakeSocialRepository().apply {
            profiles += listOf(SocialProfile("me", "eu"), SocialProfile("a", "ana"), SocialProfile("b", "beto"), SocialProfile("c", "cris"))
            followList += listOf(Follow("me", "a", "accepted"), Follow("me", "b", "pending"), Follow("c", "me", "pending"))
        }
        val s = vm(repo).state.value
        assertEquals(setOf("a", "b", "c"), s.people.map { it.profile.id }.toSet()) // sem o próprio usuário
        assertEquals(Relation.FOLLOWING, s.people.first { it.profile.id == "a" }.relation)
        assertEquals(Relation.REQUESTED, s.people.first { it.profile.id == "b" }.relation)
        assertEquals(Relation.NONE, s.people.first { it.profile.id == "c" }.relation)
        assertEquals(listOf("c"), s.followRequests.map { it.id })
    }

    @Test fun seguirEAceitarAtualizamAListas() {
        val repo = FakeSocialRepository().apply {
            profiles += listOf(SocialProfile("a", "ana"), SocialProfile("c", "cris"))
            followList += Follow("c", "me", "pending")
        }
        val vm = vm(repo)
        vm.follow("a")
        assertEquals(Relation.REQUESTED, vm.state.value.people.first { it.profile.id == "a" }.relation)
        vm.acceptFollower("c")
        assertTrue(vm.state.value.followRequests.isEmpty())
        assertTrue(vm.state.value.people.first { it.profile.id == "c" }.followsMe)
    }

    @Test fun feedMostraAutorEReacoes() {
        val repo = FakeSocialRepository().apply {
            profiles += SocialProfile("a", "ana", fullName = "Ana Souza")
            posts += listOf(FeedPost("p1", "a", "w1"), FeedPost("p2", "me", "w2"), FeedPost("p3", "zzz", "w3"))
            reactionList += PostReaction("p1", "me", "fire")
        }
        val feed = vm(repo).state.value.feed
        assertEquals(listOf("Ana Souza", "Você", "Alguém"), feed.map { it.author })
        assertTrue(feed[1].isMine)
        assertEquals(setOf(ReactionKind.FIRE), feed[0].reactions.mine)
    }

    @Test fun editarLegendaEVisibilidadeDoPost() {
        val repo = FakeSocialRepository().apply { posts += FeedPost("p1", "me", "w1", "antiga", "followers") }
        val vm = vm(repo)
        vm.editPost("p1", "  Dia de perna!  ", "public")
        val p = vm.state.value.feed.single().post
        assertEquals("Dia de perna!", p.caption)
        assertEquals("public", p.visibility)
        vm.editPost("p1", "", "private")
        assertNull(vm.state.value.feed.single().post.caption) // legenda vazia limpa
    }

    @Test fun postComLegendaGrandeOuVisibilidadeInvalidaNaoSalva() {
        val repo = FakeSocialRepository().apply { posts += FeedPost("p1", "me", "w1", "ok", "followers") }
        val vm = vm(repo)
        vm.editPost("p1", "x".repeat(281), "public")
        assertEquals("A legenda passou de 280 caracteres.", vm.state.value.error)
        vm.clearMessages()
        vm.editPost("p1", "ok", "todos")
        assertEquals("Visibilidade inválida.", vm.state.value.error)
        assertEquals("ok", vm.state.value.feed.single().post.caption)
    }

    @Test fun reagirLigaEDesliga() {
        val repo = FakeSocialRepository().apply { posts += FeedPost("p1", "me", "w1") }
        val vm = vm(repo)
        vm.toggleReaction("p1", ReactionKind.FLEX)
        assertEquals(1, vm.state.value.feed.single().reactions.count(ReactionKind.FLEX))
        vm.toggleReaction("p1", ReactionKind.FLEX)
        assertEquals(0, vm.state.value.feed.single().reactions.count(ReactionKind.FLEX))
    }

    @Test fun semNadaNaTelaGuardaOErroParaTentarDeNovo() {
        val repo = FakeSocialRepository().apply { failFeed = true } // só o feed falha e não há mais nada
        val s = vm(repo).state.value
        assertEquals("Sem conexão com a internet. Verifique sua rede e tente de novo.", s.loadError)
        repo.failFeed = false; repo.posts += FeedPost("p1", "me", "w1")
        val vm = vm(repo)
        assertNull(vm.state.value.loadError)
    }

    @Test fun erroNoFeedNaoDerrubaOsOutrosBlocos() {
        val repo = FakeSocialRepository().apply {
            failFeed = true
            groups += SocialGroup("g1", "x", "Turma", privacy = "public")
        }
        val s = vm(repo).state.value
        assertEquals("Sem conexão com a internet. Verifique sua rede e tente de novo.", s.error)
        assertEquals(listOf("g1"), s.publicGroups.map { it.id }) // grupos carregaram mesmo assim
        assertFalse(s.isLoading)
    }

    @Test fun criarGrupoMostraEmMeusGrupos() {
        val vm = vm(FakeSocialRepository())
        vm.createGroup(GroupForm("Turma do treino", isPublic = false))
        val s = vm.state.value
        assertEquals(listOf("Turma do treino"), s.myGroups.map { it.name })
        assertEquals("Grupo 'Turma do treino' criado", s.message)
    }

    @Test fun grupoInvalidoGeraErroSemChamarOBanco() {
        val repo = FakeSocialRepository()
        val vm = vm(repo)
        vm.createGroup(GroupForm("a"))
        assertEquals("O nome do grupo precisa ter entre 2 e 60 caracteres.", vm.state.value.error)
        assertTrue(repo.groups.isEmpty())
    }

    @Test fun entrarPorCodigo() {
        val repo = FakeSocialRepository().apply { groups += SocialGroup("g1", "x", "Privado", inviteCode = "abc12345") }
        val vm = vm(repo)
        vm.joinGroupByCode("   ")
        assertNotNull(vm.state.value.error)
        vm.clearMessages()
        vm.joinGroupByCode("abc12345")
        assertEquals(listOf("g1"), vm.state.value.myGroups.map { it.id })
    }

    @Test fun criadorEntraNoProprioDesafio() {
        val vm = vm(FakeSocialRepository())
        vm.createChallenge(ChallengeForm("Mês de treinos", ChallengeMetric.WORKOUTS, days = 30))
        val item = vm.state.value.challenges.single()
        assertTrue(item.joined)
        assertEquals(ChallengeStatus.ACTIVE, item.status)
        assertEquals(30, item.daysLeft)
    }

    @Test fun desafiosEncerradosVaoParaOFim() {
        val repo = FakeSocialRepository().apply {
            challengeList += challenge("old", "2026-09-01T00:00:00Z", "2026-09-10T00:00:00Z")
            challengeList += challenge("now", "2026-10-15T00:00:00Z", "2026-11-01T00:00:00Z")
        }
        assertEquals(listOf("now", "old"), vm(repo).state.value.challenges.map { it.challenge.id })
    }

    @Test fun entrarESairDoDesafio() {
        val repo = FakeSocialRepository().apply { challengeList += challenge("c1", "2026-10-15T00:00:00Z", "2026-11-01T00:00:00Z") }
        val vm = vm(repo)
        assertFalse(vm.state.value.challenges.single().joined)
        vm.joinChallenge("c1")
        assertTrue(vm.state.value.challenges.single().joined)
        vm.leaveChallenge("c1")
        assertFalse(vm.state.value.challenges.single().joined)
    }

    @Test fun abrePerfilComEstatisticasEMusicas() {
        val repo = FakeSocialRepository().apply {
            profiles += SocialProfile("a", "ana")
            statsFor["a"] = com.vitoroliveira.gymtracker.data.model.ProfileStats(workouts = 12, volumeKg = 5200.0)
            tracksFor["a"] = listOf(com.vitoroliveira.gymtracker.data.model.TopTrack("Eye of the Tiger", "Survivor", plays = 4))
        }
        val vm = vm(repo)
        vm.openProfile(vm.state.value.people.single())
        val pv = vm.state.value.profileView!!
        assertFalse(pv.isLoading)
        assertEquals(12, pv.stats?.workouts)
        assertEquals("Eye of the Tiger", pv.tracks.single().title)
        vm.closeProfile()
        assertNull(vm.state.value.profileView)
    }

    @Test fun musicaNegadaNaoEscondeAsEstatisticas() {
        val repo = FakeSocialRepository().apply {
            profiles += SocialProfile("a", "ana")
            statsFor["a"] = com.vitoroliveira.gymtracker.data.model.ProfileStats(workouts = 3)
            failTracks = true
        }
        val vm = vm(repo)
        vm.openProfile(vm.state.value.people.single())
        val pv = vm.state.value.profileView!!
        assertEquals(3, pv.stats?.workouts)
        assertTrue(pv.tracks.isEmpty())
        assertFalse(pv.isLoading)
    }

    @Test fun carregaPlacar() {
        val repo = FakeSocialRepository().apply {
            standingsFor["c1"] = listOf(Standing("a", "ana", score = 12.0, rank = 1), Standing("me", "eu", score = 9.0, rank = 2))
        }
        val vm = vm(repo)
        vm.loadStandings("c1")
        assertEquals(listOf("ana", "eu"), vm.state.value.standings["c1"]!!.map { it.username })
    }

    @Test fun bioGrandeDemaisNaoSalva() {
        val repo = FakeSocialRepository()
        val vm = vm(repo)
        vm.saveSettings(SocialSettings(bio = "x".repeat(281)))
        assertNotNull(vm.state.value.error)
        assertFalse("save" in repo.calls)
        vm.clearMessages()
        vm.saveSettings(SocialSettings(bio = "  oi  ", visibility = "public", shareWorkouts = true))
        assertEquals("oi", vm.state.value.settings?.bio)
        assertTrue(vm.state.value.settings!!.shareWorkouts)
    }
}
