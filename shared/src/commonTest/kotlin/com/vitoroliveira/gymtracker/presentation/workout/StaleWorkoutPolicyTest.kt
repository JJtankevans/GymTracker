package com.vitoroliveira.gymtracker.presentation.workout

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class StaleWorkoutPolicyTest {
    private val now = Instant.parse("2026-11-21T20:00:00Z")

    @Test
    fun treinoComSerieRecenteContinua() {
        val d = StaleWorkoutPolicy.decide("2026-11-21T18:00:00Z", listOf("2026-11-21T19:30:00Z"), now)
        assertEquals(StaleDecision.Keep, d)
    }

    @Test
    fun maisDeUmaHoraSemSerieEncerraNaUltimaSerie() {
        val d = StaleWorkoutPolicy.decide(
            "2026-11-21T16:00:00Z",
            listOf("2026-11-21T16:20:00Z", "2026-11-21T17:05:00Z", "2026-11-21T16:40:00Z"),
            now
        )
        assertEquals(StaleDecision.FinishAt("2026-11-21T17:05:00Z"), d)
    }

    @Test
    fun treinoAbertoSemNenhumaSerieHaMaisDeUmaHoraApaga() {
        val d = StaleWorkoutPolicy.decide("2026-11-21T18:30:00Z", emptyList(), now)
        assertEquals(StaleDecision.Discard, d)
    }

    @Test
    fun treinoRecemIniciadoSemSerieContinua() {
        val d = StaleWorkoutPolicy.decide("2026-11-21T19:40:00Z", emptyList(), now)
        assertEquals(StaleDecision.Keep, d)
    }

    @Test
    fun exatamenteUmaHoraAindaContinua() {
        val d = StaleWorkoutPolicy.decide("2026-11-21T17:00:00Z", listOf("2026-11-21T19:00:00Z"), now)
        assertEquals(StaleDecision.Keep, d)
    }

    @Test
    fun semDatasValidasNaoMexe() {
        assertEquals(StaleDecision.Keep, StaleWorkoutPolicy.decide(null, emptyList(), now))
        assertEquals(StaleDecision.Keep, StaleWorkoutPolicy.decide("lixo", listOf(null, "lixo"), now))
    }
}
