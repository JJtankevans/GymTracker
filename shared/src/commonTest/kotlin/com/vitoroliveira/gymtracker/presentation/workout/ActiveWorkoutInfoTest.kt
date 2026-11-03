package com.vitoroliveira.gymtracker.presentation.workout

import kotlin.test.Test
import kotlin.test.assertEquals

class ActiveWorkoutInfoTest {
    @Test fun tempoDesdeOInicio() {
        assertEquals("agora mesmo", formatElapsedSince(0))
        assertEquals("12 min", formatElapsedSince(12))
        assertEquals("1 h", formatElapsedSince(60))
        assertEquals("1 h 5 min", formatElapsedSince(65))
        assertEquals("23 h 59 min", formatElapsedSince(24 * 60 - 1))
        assertEquals("1 dia", formatElapsedSince(24 * 60))
        assertEquals("1 dia", formatElapsedSince(40 * 60))
        assertEquals("3 dias", formatElapsedSince(3 * 24 * 60 + 5))
    }
}
