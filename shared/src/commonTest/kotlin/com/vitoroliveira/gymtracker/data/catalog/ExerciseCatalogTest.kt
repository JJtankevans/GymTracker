package com.vitoroliveira.gymtracker.data.catalog

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ExerciseCatalogTest {
    @Test fun temPeloMenos50() = assertTrue(ExerciseCatalog.all.size >= 50, "${ExerciseCatalog.all.size}")

    @Test fun nomesUnicos() {
        val names = ExerciseCatalog.all.map { it.name }
        assertEquals(names.size, names.toSet().size)
    }

    @Test fun todoGrupoTemExercicio() =
        CatalogMuscle.entries.forEach { m -> assertTrue(ExerciseCatalog.all.any { it.muscle == m }, m.name) }

    @Test fun buscaIgnoraAcentoECaixa() {
        assertTrue(ExerciseCatalog.search("gluteo").any { it.muscle == CatalogMuscle.GLUTEOS })
        assertTrue(ExerciseCatalog.search("AGACHAMENTO").size >= 3)
    }

    @Test fun filtraPorMusculo() =
        assertTrue(ExerciseCatalog.search("", CatalogMuscle.PEITO).all { it.muscle == CatalogMuscle.PEITO })

    @Test fun recentesNaoRepetemELimitam() {
        assertEquals(listOf("C", "A", "B"), ExerciseCatalog.pushRecent(listOf("A", "B"), listOf("C", "A")))
        assertEquals(3, ExerciseCatalog.pushRecent(listOf("A", "B", "C"), listOf("D"), max = 3).size)
        assertEquals("D", ExerciseCatalog.pushRecent(listOf("A", "B", "C"), listOf("D"), max = 3).first())
    }

    @Test fun favoritosVemPrimeiroMantendoAOrdem() {
        val list = ExerciseCatalog.search("supino")
        val fav = setOf(list[2].name, list[4].name)
        val out = ExerciseCatalog.favoritesFirst(list, fav)
        assertEquals(listOf(list[2].name, list[4].name), out.take(2).map { it.name })
        assertEquals(list.size, out.size)
    }

    @Test fun soRecentesNaOrdemDeUso() {
        val names = listOf(ExerciseCatalog.all[5].name, ExerciseCatalog.all[1].name, "Inexistente")
        assertEquals(names.take(2), ExerciseCatalog.recentOnly(ExerciseCatalog.all, names).map { it.name })
    }

    @Test fun mesmoNomeIgnoraAcento() =
        assertTrue(ExerciseCatalog.isSameName("Elevação Pélvica", "elevacao pelvica"))
}
