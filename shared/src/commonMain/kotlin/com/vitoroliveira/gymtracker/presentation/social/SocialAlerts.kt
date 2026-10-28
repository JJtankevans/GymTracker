package com.vitoroliveira.gymtracker.presentation.social

import com.vitoroliveira.gymtracker.data.model.Challenge
import com.vitoroliveira.gymtracker.data.model.ChallengeParticipant
import com.vitoroliveira.gymtracker.data.model.Follow
import com.vitoroliveira.gymtracker.data.model.SocialProfile
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

/** Um aviso local do Social. [key] identifica o aviso para não repetir (guardado no aparelho). */
data class SocialAlert(val key: String, val title: String, val text: String)

/**
 * Avisos novos para mostrar como notificação (sem servidor de push: o app confere de tempos em tempos):
 * - pedidos para me seguir ainda pendentes;
 * - desafios de que participo e que terminaram nos últimos 7 dias.
 * Tira os que já foram avisados ([seen]).
 */
fun socialAlerts(
    me: String,
    follows: List<Follow>,
    people: List<SocialProfile>,
    challenges: List<Challenge>,
    participations: List<ChallengeParticipant>,
    seen: Set<String>,
    now: Instant
): List<SocialAlert> {
    val names = people.associate { it.id to it.username }
    val requests = follows.filter { it.followedId == me && it.status == "pending" }.map { f ->
        val who = names[f.followerId]?.let { "@$it" } ?: "Alguém"
        SocialAlert("seguir:${f.followerId}", "Pedido para seguir", "$who quer te seguir. Abra o Social para aceitar.")
    }
    val mine = participations.filter { it.profileId == me }.map { it.challengeId }.toSet()
    val ended = challenges.filter { c ->
        val end = runCatching { Instant.parse(c.endsAt) }.getOrNull() ?: return@filter false
        c.id in mine && end <= now && end > now - 7.days
    }.map { c -> SocialAlert("desafio:${c.id}", "Desafio terminou", "\"${c.title}\" acabou. Veja como você ficou no placar.") }
    return (requests + ended).filter { it.key !in seen }
}
