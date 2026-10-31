package com.vitoroliveira.gymtracker.presentation.social

import com.vitoroliveira.gymtracker.data.model.Challenge
import com.vitoroliveira.gymtracker.data.model.ChallengeParticipant
import com.vitoroliveira.gymtracker.data.model.Follow
import com.vitoroliveira.gymtracker.data.model.SocialProfile
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class SocialAlertsTest {
    private val now = Instant.parse("2026-10-28T12:00:00Z")
    private fun ch(id: String, end: String) = Challenge(id = id, creatorId = "x", title = "Desafio $id", metric = "workouts", startsAt = "2026-10-01T00:00:00Z", endsAt = end)

    @Test
    fun pedidosPendentesEDesafiosQueTerminaramSemRepetir() {
        val alerts = socialAlerts(
            me = "me",
            follows = listOf(
                Follow("ana", "me", "pending"),        // pedido para mim: avisa
                Follow("beto", "me", "accepted"),      // já aceito: não
                Follow("me", "cris", "pending")        // pedido meu: não
            ),
            people = listOf(SocialProfile("ana", "ana_fit")),
            challenges = listOf(
                ch("c1", "2026-10-27T23:00:00Z"),      // terminou ontem e eu participo: avisa
                ch("c2", "2026-11-05T00:00:00Z"),      // ainda rolando
                ch("c3", "2026-10-01T00:00:00Z"),      // terminou há muito tempo
                ch("c4", "2026-10-27T00:00:00Z")       // terminou, mas não participo
            ),
            participations = listOf(
                ChallengeParticipant("c1", "me"), ChallengeParticipant("c2", "me"),
                ChallengeParticipant("c3", "me"), ChallengeParticipant("c4", "outro")
            ),
            seen = emptySet(),
            now = now
        )
        assertEquals(listOf("seguir:ana", "desafio:c1"), alerts.map { it.key })
        assertEquals("@ana_fit quer te seguir. Abra o Social para aceitar.", alerts.first().text)

        val again = socialAlerts("me", listOf(Follow("ana", "me", "pending")), emptyList(), emptyList(), emptyList(), setOf("seguir:ana"), now)
        assertEquals(emptyList(), again)
    }

    @Test
    fun fechamentoDaSemanaNoDomingoANoiteOuNaSegunda() {
        val d = { m: Int, day: Int -> kotlinx.datetime.LocalDate(2026, m, day) }
        val sessions = listOf(d(10, 26) to 60L, d(10, 28) to 50L, d(10, 30) to 70L, d(10, 20) to 40L)   // semana de 26/10 a 1/11
        val domingoNoite = kotlinx.datetime.LocalDateTime(2026, 11, 1, 19, 0)
        val alert = weekRecapAlert(sessions, goal = 3, now = domingoNoite, seen = emptySet())
        assertEquals("semana:2026-10-26", alert?.key)
        assertEquals("3 treinos (meta de 3 batida) · 3 h treinando. Mandou bem!", alert?.text)
        // segunda de manhã: mesma semana, mas já avisada
        assertEquals(null, weekRecapAlert(sessions, 3, kotlinx.datetime.LocalDateTime(2026, 11, 2, 8, 0), setOf("semana:2026-10-26")))
        // domingo de tarde e quarta: ainda não
        assertEquals(null, weekRecapAlert(sessions, 3, kotlinx.datetime.LocalDateTime(2026, 11, 1, 15, 0), emptySet()))
        assertEquals(null, weekRecapAlert(sessions, 3, kotlinx.datetime.LocalDateTime(2026, 10, 28, 20, 0), emptySet()))
        // faltou treino
        assertEquals("1 treino · 40 min. Faltou 3 para a meta de 4.",
            weekRecapAlert(listOf(d(10, 20) to 40L), 4, kotlinx.datetime.LocalDateTime(2026, 10, 26, 9, 0), emptySet())?.text)
    }

    // ---- sequência em risco ----
    private fun dia(dia: Int, mes: Int = 10) = kotlinx.datetime.LocalDate(2026, mes, dia)
    private val tresDias = listOf(dia(25), dia(26), dia(27)) // domingo a terça
    private fun quarta(h: Int) = kotlinx.datetime.LocalDateTime(2026, 10, 28, h, 0)

    @Test fun avisaSequenciaEmRiscoDepoisDas18() {
        val a = streakRiskAlert(tresDias, quarta(19), emptySet())
        assertEquals("sequencia:2026-10-28", a?.key)
        assertEquals("Você treinou 3 dias seguidos. Um treino hoje mantém a sequência.", a?.text)
    }

    @Test fun naoAvisaDeManhaNemDeTardeCedo() {
        assertEquals(null, streakRiskAlert(tresDias, quarta(9), emptySet()))
        assertEquals(null, streakRiskAlert(tresDias, quarta(17), emptySet()))
    }

    @Test fun naoAvisaSeJaTreinouHoje() {
        assertEquals(null, streakRiskAlert(tresDias + dia(28), quarta(20), emptySet()))
    }

    @Test fun naoAvisaSequenciaCurtaOuJaQuebrada() {
        assertEquals(null, streakRiskAlert(listOf(dia(26), dia(27)), quarta(20), emptySet()))      // só 2 dias
        assertEquals(null, streakRiskAlert(tresDias, kotlinx.datetime.LocalDateTime(2026, 10, 29, 20, 0), emptySet())) // terminou anteontem
        assertEquals(null, streakRiskAlert(emptyList(), quarta(20), emptySet()))
    }

    @Test fun avisaUmaVezPorDia() {
        assertEquals(null, streakRiskAlert(tresDias, quarta(21), setOf("sequencia:2026-10-28")))
        // dois dias depois, sem treino no meio: a sequência quebrou, nada a proteger
        assertEquals(null, streakRiskAlert(tresDias + dia(28), kotlinx.datetime.LocalDateTime(2026, 10, 30, 20, 0), emptySet()))
    }
}
