package com.vitoroliveira.gymtracker.presentation.social

import com.vitoroliveira.gymtracker.data.model.Challenge
import com.vitoroliveira.gymtracker.data.model.ChallengeParticipant
import com.vitoroliveira.gymtracker.data.model.Follow
import com.vitoroliveira.gymtracker.data.model.SocialProfile
import com.vitoroliveira.gymtracker.presentation.DashboardCalculator
import com.vitoroliveira.gymtracker.presentation.workout.formatMinutes
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.minus
import kotlinx.datetime.plus
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

/**
 * Fechamento da semana (segunda a domingo): aparece no domingo a partir das 18h (semana atual) ou na segunda
 * (semana que passou), uma vez por semana. [sessions] = (dia do treino, minutos). Null se não é hora ou já avisou.
 */
fun weekRecapAlert(sessions: List<Pair<LocalDate, Long?>>, goal: Int, now: LocalDateTime, seen: Set<String>): SocialAlert? {
    val today = now.date
    val weekStart = when {
        today.dayOfWeek == DayOfWeek.SUNDAY && now.hour >= 18 -> DashboardCalculator.weekStart(today)
        today.dayOfWeek == DayOfWeek.MONDAY -> DashboardCalculator.weekStart(today.minus(1, DateTimeUnit.DAY))
        else -> return null
    }
    val key = "semana:$weekStart"
    if (key in seen) return null
    val weekEnd = weekStart.plus(6, DateTimeUnit.DAY)
    val week = sessions.filter { it.first in weekStart..weekEnd }
    val n = week.size
    val minutes = week.sumOf { it.second ?: 0L }
    val treinos = if (n == 1) "1 treino" else "$n treinos"
    val text = when {
        n == 0 -> "Semana sem treino. Bora voltar: o primeiro treino da semana é o mais importante."
        goal > 0 && n >= goal -> "$treinos (meta de $goal batida) · ${formatMinutes(minutes)} treinando. Mandou bem!"
        goal > 0 -> "$treinos · ${formatMinutes(minutes)}. Faltou ${goal - n} para a meta de $goal."
        else -> "$treinos · ${formatMinutes(minutes)} treinando."
    }
    return SocialAlert(key, "Sua semana", text)
}
