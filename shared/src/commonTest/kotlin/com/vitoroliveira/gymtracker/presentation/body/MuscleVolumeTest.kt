package com.vitoroliveira.gymtracker.presentation.body

import com.vitoroliveira.gymtracker.data.catalog.CatalogMuscle
import com.vitoroliveira.gymtracker.data.model.RecentSet
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MuscleVolumeTest {
    private fun s(ex: String, reps: Int = 10) = RecentSet(exerciseId = ex, reps = reps, weightKg = 40.0)
    private val names = mapOf("e1" to "Supino Reto com Barra", "e2" to "Agachamento Hack", "e3" to "Meu Exercício Inventado", "e4" to "Crossover na Polia")

    @Test fun contaSeriesPorMusculoUsandoNomeEApelido() {
        val r = weeklySetsByMuscle(listOf(s("e1"), s("e1"), s("e4"), s("e2"), s("e3")), names)
        assertEquals(3, r.first { it.muscle == CatalogMuscle.PEITO }.sets)
        assertEquals(1, r.first { it.muscle == CatalogMuscle.QUADRICEPS }.sets) // "Agachamento Hack" -> Hack Squat
        assertEquals(1, r.first { it.muscle == null }.sets)
        assertEquals(CatalogMuscle.PEITO, r.first().muscle) // mais séries primeiro
    }

    @Test fun mostraTodosOsMusculosMesmoComZeroESemOutrosQuandoNaoHa() {
        val r = weeklySetsByMuscle(listOf(s("e1")), names)
        assertEquals(CatalogMuscle.entries.size, r.size)
        assertTrue(r.none { it.muscle == null })
        assertTrue(r.drop(1).all { it.sets == 0 })
    }

    @Test fun ignoraSeriesSemRepeticoes() {
        val r = weeklySetsByMuscle(listOf(s("e1", reps = 0)), names)
        assertTrue(r.all { it.sets == 0 })
    }
}
