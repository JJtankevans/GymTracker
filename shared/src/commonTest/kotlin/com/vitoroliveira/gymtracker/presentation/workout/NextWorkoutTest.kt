package com.vitoroliveira.gymtracker.presentation.workout

import com.vitoroliveira.gymtracker.data.model.MuscleGroup
import com.vitoroliveira.gymtracker.data.model.Workout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class NextWorkoutTest {
    private val a = MuscleGroup(id = "a", name = "A", colorHex = "#000000")
    private val b = MuscleGroup(id = "b", name = "B", colorHex = "#000000")
    private val c = MuscleGroup(id = "c", name = "C", colorHex = "#000000")
    private fun w(group: String?, day: Int) = Workout(id = "w$day", muscleGroupId = group, startedAt = "2026-10-${day.toString().padStart(2, '0')}T10:00:00Z")

    @Test fun rotacaoABCSugereOTreinadoHaMaisTempo() {
        val hist = listOf(w("a", 20), w("b", 21), w("c", 22))
        assertEquals("a", suggestNextGroup(listOf(a, b, c), hist))
        assertEquals("b", suggestNextGroup(listOf(a, b, c), hist + w("a", 23)))
    }

    @Test fun grupoNuncaTreinadoVemPrimeiro() {
        assertEquals("c", suggestNextGroup(listOf(a, b, c), listOf(w("a", 20), w("b", 21))))
    }

    @Test fun usaOTreinoMaisRecenteDeCadaGrupo() {
        // A foi treinado dia 10 e de novo dia 25: o mais antigo agora é B (dia 20)
        assertEquals("b", suggestNextGroup(listOf(a, b), listOf(w("a", 10), w("b", 20), w("a", 25))))
    }

    @Test fun semHistoricoOuComUmGrupoSoNaoSugere() {
        assertNull(suggestNextGroup(listOf(a, b), emptyList()))
        assertNull(suggestNextGroup(listOf(a), listOf(w("a", 20))))
        assertNull(suggestNextGroup(listOf(a, b), listOf(w(null, 20)))) // só treino livre
    }

    @Test fun ignoraTreinosDeGruposApagados() {
        assertEquals("b", suggestNextGroup(listOf(a, b), listOf(w("x", 1), w("a", 20))))
    }
}
