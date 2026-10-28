package com.vitoroliveira.gymtracker.presentation.social

import com.vitoroliveira.gymtracker.data.model.Challenge
import com.vitoroliveira.gymtracker.data.model.NewChallenge
import com.vitoroliveira.gymtracker.data.model.NewGroup
import com.vitoroliveira.gymtracker.data.model.PostReaction
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

/** Número inteiro com ponto de milhar: 5200 -> "5.200". */
fun formatInt(value: Double): String {
    val n = value.toLong()
    val neg = n < 0
    val digits = kotlin.math.abs(n).toString()
    val grouped = digits.reversed().chunked(3).joinToString(".").reversed()
    return if (neg) "-$grouped" else grouped
}

// ───────────────────────── Desafios ─────────────────────────

enum class ChallengeMetric(val key: String, val label: String, val unit: String, val needsExercise: Boolean = false) {
    WORKOUTS("workouts", "Treinos feitos", "treinos"),
    ACTIVE_DAYS("active_days", "Dias ativos", "dias"),
    VOLUME("volume_kg", "Volume total", "kg"),
    SETS("sets", "Séries", "séries"),
    REPS("reps", "Repetições", "reps"),
    MINUTES("training_minutes", "Minutos treinados", "min"),
    EXERCISE_MAX("exercise_max_weight", "Maior carga em um exercício", "kg", needsExercise = true),
    EXERCISE_VOLUME("exercise_volume", "Volume em um exercício", "kg", needsExercise = true);

    fun format(score: Double): String = "${formatInt(score)} ${if (score == 1.0) SINGULAR[unit] ?: unit else unit}"

    companion object {
        private val SINGULAR = mapOf("treinos" to "treino", "dias" to "dia", "séries" to "série")

        fun fromKey(key: String): ChallengeMetric? = entries.firstOrNull { it.key == key }
    }
}

enum class ChallengeStatus(val label: String) { UPCOMING("Em breve"), ACTIVE("Em andamento"), ENDED("Encerrado") }

fun challengeStatus(c: Challenge, now: Instant): ChallengeStatus {
    val start = runCatching { Instant.parse(c.startsAt) }.getOrNull()
    val end = runCatching { Instant.parse(c.endsAt) }.getOrNull()
    return when {
        end != null && now >= end -> ChallengeStatus.ENDED
        start != null && now < start -> ChallengeStatus.UPCOMING
        else -> ChallengeStatus.ACTIVE
    }
}

/** Dias que faltam (arredondado pra cima); 0 se já acabou. */
fun daysLeft(c: Challenge, now: Instant): Int {
    val end = runCatching { Instant.parse(c.endsAt) }.getOrNull() ?: return 0
    val ms = (end - now).inWholeMilliseconds
    if (ms <= 0) return 0
    return ((ms + 86_399_999L) / 86_400_000L).toInt()
}

/** Dados do formulário de criação de desafio. */
data class ChallengeForm(
    val title: String,
    val metric: ChallengeMetric,
    val exerciseName: String? = null,
    val days: Int = 7,
    val groupId: String? = null,
    val minMinutesPerWorkout: Int = 20,
    val maxWorkoutsPerDay: Int = 1,
    val target: Double? = null,
    val description: String? = null,
    /** 2 ou mais nomes = desafio em equipes; vazio = individual. */
    val teams: List<String> = emptyList(),
    /** Mais que isso de treinos abaixo do mínimo de minutos = título de falha no perfil. Null = desligado. */
    val failThreshold: Int? = null,
    /** Dias seguidos para o título Alpha (Chad 2x, Giga Chad 3x, Mr. Olimpia 4x). Null = desligado. */
    val streakGoal: Int? = null
)

/** Valida e monta o desafio. Lança [IllegalArgumentException] com a mensagem pro usuário. */
fun ChallengeForm.toNew(creatorId: String, now: Instant): NewChallenge {
    val t = title.trim()
    require(t.length in 3..80) { "O título precisa ter entre 3 e 80 caracteres." }
    require(days in 1..365) { "A duração precisa ficar entre 1 e 365 dias." }
    require(minMinutesPerWorkout in 0..300) { "Os minutos mínimos por treino ficam entre 0 e 300." }
    require(maxWorkoutsPerDay in 1..10) { "O máximo de treinos por dia fica entre 1 e 10." }
    require(target == null || target > 0) { "A meta precisa ser maior que zero." }
    require(failThreshold == null || failThreshold in 1..100) { "As falhas permitidas ficam entre 1 e 100." }
    require(failThreshold == null || minMinutesPerWorkout > 0) { "Para contar falhas defina um mínimo de minutos maior que zero: falha é treino abaixo do mínimo." }
    require(streakGoal == null || streakGoal in 1..365) { "Os dias seguidos para o título ficam entre 1 e 365." }
    require(streakGoal == null || streakGoal <= days) { "Os dias seguidos para o título não podem passar da duração do desafio." }
    val ex = exerciseName?.trim().orEmpty()
    require(!metric.needsExercise || ex.isNotEmpty()) { "Escolha o exercício dessa métrica." }
    val teamNames = teams.map { it.trim() }.filter { it.isNotEmpty() }.distinctBy { it.lowercase() }
    require(teamNames.isEmpty() || teamNames.size in 2..6) { "Use de 2 a 6 equipes (nomes diferentes)." }
    require(teamNames.all { it.length <= 20 }) { "Cada nome de equipe pode ter até 20 caracteres." }
    return NewChallenge(
        creatorId = creatorId,
        groupId = groupId,
        title = t,
        description = description?.trim()?.takeIf { it.isNotEmpty() },
        metric = metric.key,
        exerciseName = if (metric.needsExercise) ex else null,
        mode = if (teamNames.isEmpty()) "individual" else "team",
        teams = teamNames,
        visibility = if (groupId != null) "group" else "public",
        startsAt = now.toString(),
        endsAt = (now + days.days).toString(),
        target = target,
        minMinutesPerWorkout = minMinutesPerWorkout,
        maxWorkoutsPerDay = maxWorkoutsPerDay,
        maxParticipants = null,
        failThreshold = failThreshold,
        streakGoal = streakGoal
    )
}

// ───────────────────────── Grupos ─────────────────────────

data class GroupForm(val name: String, val description: String? = null, val isPublic: Boolean = false, val maxMembers: Int? = null)

fun GroupForm.toNew(ownerId: String): NewGroup {
    val n = name.trim()
    require(n.length in 2..60) { "O nome do grupo precisa ter entre 2 e 60 caracteres." }
    require(maxMembers == null || maxMembers in 2..500) { "O limite de membros fica entre 2 e 500." }
    val d = description?.trim()?.takeIf { it.isNotEmpty() }
    require(d == null || d.length <= 500) { "A descrição passou de 500 caracteres." }
    return NewGroup(ownerId, n, d, if (isPublic) "public" else "invite", maxMembers)
}

// ───────────────────────── Feed ─────────────────────────

enum class ReactionKind(val key: String, val emoji: String, val label: String) {
    FLEX("flex", "💪", "Força"),
    FIRE("fire", "🔥", "Fogo"),
    CLAP("clap", "👏", "Aplauso"),
    HEADPHONES("headphones", "🎧", "Boa playlist")
}

data class ReactionSummary(val counts: Map<ReactionKind, Int>, val mine: Set<ReactionKind>) {
    fun count(kind: ReactionKind) = counts[kind] ?: 0

    /** Liga/desliga a minha reação [kind] e ajusta a contagem (atualização otimista do feed). */
    fun toggled(kind: ReactionKind): ReactionSummary =
        if (kind in mine) ReactionSummary(counts + (kind to (count(kind) - 1).coerceAtLeast(0)), mine - kind)
        else ReactionSummary(counts + (kind to count(kind) + 1), mine + kind)
}

fun summarizeReactions(postId: String, all: List<PostReaction>, me: String?): ReactionSummary {
    val forPost = all.filter { it.postId == postId }
    val counts = ReactionKind.entries.associateWith { k -> forPost.count { it.kind == k.key } }
    val mine = ReactionKind.entries.filter { k -> forPost.any { it.kind == k.key && it.profileId == me } }.toSet()
    return ReactionSummary(counts, mine)
}

private fun JsonObject.num(key: String): Double? = (this[key] as? JsonPrimitive)?.doubleOrNull

/** "45 min · 18 séries · 5.200 kg" a partir do resumo que o banco grava no post. */
fun formatFeedStats(stats: JsonObject): String {
    val parts = buildList {
        stats.num("minutes")?.takeIf { it > 0 }?.let { add("${formatInt(it)} min") }
        stats.num("sets")?.takeIf { it > 0 }?.let { add("${formatInt(it)} ${if (it == 1.0) "série" else "séries"}") }
        stats.num("volume_kg")?.takeIf { it > 0 }?.let { add("${formatInt(it)} kg") }
    }
    return parts.joinToString(" · ")
}

fun feedGroupName(stats: JsonObject): String? =
    runCatching { stats["group"]?.jsonPrimitive?.content }.getOrNull()?.takeIf { it.isNotBlank() && it != "null" }

/** Texto amigável da visibilidade do perfil/post. */
fun visibilityLabel(v: String): String = when (v) {
    "public" -> "Público"
    "followers" -> "Só seguidores"
    else -> "Privado"
}

// ───────────────────────── Placar ─────────────────────────

/** Quanto da meta já foi cumprido (0..1); null se o desafio não tem meta. */
fun progressToTarget(score: Double, target: Double?): Float? =
    if (target == null || target <= 0) null else (score / target).toFloat().coerceIn(0f, 1f)

/** Total por equipe. Maior carga conta o MAIOR valor da equipe; as demais métricas somam. */
fun teamTotals(standings: List<com.vitoroliveira.gymtracker.data.model.Standing>, metric: ChallengeMetric?): List<Pair<String, Double>> =
    standings.filter { !it.team.isNullOrBlank() }
        .groupBy { it.team!! }
        .map { (team, rows) ->
            team to if (metric == ChallengeMetric.EXERCISE_MAX) rows.maxOf { it.score } else rows.sumOf { it.score }
        }
        .sortedByDescending { it.second }
