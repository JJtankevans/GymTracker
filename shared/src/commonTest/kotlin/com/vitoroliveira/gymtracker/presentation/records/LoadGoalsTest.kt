package com.vitoroliveira.gymtracker.presentation.records

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LoadGoalsTest {

    @Test
    fun progressoAteAMeta() {
        val g = GoalProgress(targetKg = 100.0, bestKg = 80.0)
        assertEquals(0.8f, g.fraction)
        assertEquals(80, g.percent)
        assertEquals(20.0, g.remainingKg)
        assertFalse(g.reached)
        val done = GoalProgress(100.0, 102.5)
        assertTrue(done.reached)
        assertEquals(0.0, done.remainingKg)
        assertEquals(1f, done.fraction)
    }

    @Test
    fun leOQueFoiDigitado() {
        assertEquals(100.0, parseGoalKg("100"))
        assertEquals(102.5, parseGoalKg("102,5"))
        assertEquals(102.5, parseGoalKg(" 102.5 kg "))
        assertEquals(42.5, parseGoalKg("42,6")) // arredonda a 0,25
        assertNull(parseGoalKg("abc"))
        assertNull(parseGoalKg("0"))
        assertNull(parseGoalKg("5000"))
    }

    @Test
    fun guardaELeOMapa() {
        val m = mapOf("a1-b2" to 100.0, "c3" to 42.5)
        assertEquals(m, decodeGoals(encodeGoals(m)))
        assertEquals(emptyMap(), decodeGoals(null))
        assertEquals(mapOf("x" to 10.0), decodeGoals("x=10;lixo;=5;y=abc"))
    }
}
