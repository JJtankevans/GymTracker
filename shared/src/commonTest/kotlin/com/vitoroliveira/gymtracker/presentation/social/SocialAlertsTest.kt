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
}
