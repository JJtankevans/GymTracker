package com.vitoroliveira.gymtracker.presentation.workout

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SupersetAndTempoTest {
    private val ids = listOf("a", "b", "c", "d")

    @Test fun `agrupa exercicios consecutivos ligados`() {
        assertEquals(listOf(listOf("a", "b"), listOf("c", "d")), supersetGroups(ids, setOf("a", "c")))
        assertEquals(listOf(listOf("a", "b", "c"), listOf("d")), supersetGroups(ids, setOf("a", "b")))
        assertEquals(listOf(listOf("a"), listOf("b"), listOf("c"), listOf("d")), supersetGroups(ids, emptySet()))
    }

    @Test fun `nomeia o grupo`() {
        assertEquals(listOf(null, "Bi-set", "Tri-set", "Circuito"), listOf(1, 2, 3, 4).map { groupLabel(it) })
    }

    @Test fun `depois da serie indica o proximo da rodada e o ultimo descansa`() {
        val g = supersetGroups(ids, setOf("a", "b"))
        assertEquals("b", nextInRound(g, "a"))
        assertEquals("c", nextInRound(g, "b"))
        assertNull(nextInRound(g, "c"))
        assertNull(nextInRound(g, "d"))
        assertEquals(listOf("a", "b", "c"), groupOf(g, "b"))
    }

    @Test fun `o ultimo da lista nao liga a nada`() {
        assertEquals(emptySet(), toggleLink(ids, emptySet(), "d"))
        assertEquals(setOf("a"), toggleLink(ids, emptySet(), "a"))
        assertEquals(emptySet(), toggleLink(ids, setOf("a"), "a"))
    }

    @Test fun `aceita formatos comuns de cadencia e rejeita invalidos`() {
        assertEquals(listOf(3, 1, 2, 0), parseTempo("3-1-2-0"))
        assertEquals(listOf(3, 1, 2, 0), parseTempo("3 1 2 0"))
        assertEquals(listOf(3, 1, 2, 0), parseTempo("3120"))
        assertNull(parseTempo("3-1-2"))
        assertNull(parseTempo("a-b-c-d"))
        assertNull(parseTempo(""))
        assertNull(parseTempo("10-1-2-0"))
    }

    @Test fun `calcula o TUT da serie`() {
        assertEquals(60, tutSeconds(10, listOf(3, 1, 2, 0)))
        assertEquals(0, tutSeconds(0, listOf(3, 1, 2, 0)))
        assertEquals("45s", formatTut(45))
        assertEquals("1min15", formatTut(75))
        assertEquals("3-1-2-0", tempoText(listOf(3, 1, 2, 0)))
    }
}
