package com.vitoroliveira.gymtracker.presentation.body

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

class InsightsTest {
    private val now = Instant.parse("2026-11-22T12:00:00Z")
    private fun set(ex: String, reps: Int, kg: Double, daysAgo: Int) = AnalysisSet(ex, reps, kg, now - daysAgo.days)

    @Test fun `recorde por faixa usa a maior carga com pelo menos N reps`() {
        val r = bandRecords(listOf(set("Supino", 10, 60.0, 5), set("Supino", 5, 80.0, 4), set("Supino", 1, 100.0, 3)))
        fun kg(b: Int) = r.first { it.band == b }.kg
        assertEquals(listOf(100.0, 80.0, 60.0), listOf(kg(1), kg(5), kg(10)))
    }

    @Test fun `calorias`() {
        assertEquals(400, estimateKcal(60.0, 80.0))
        assertEquals(0, estimateKcal(-5.0, 80.0))
    }

    @Test fun `desequilibrio avisa quando um lado passa do dobro`() {
        val sets = List(12) { set("Supino Reto com Barra", 10, 50.0, 2) }
        val r = imbalances(sets, now)
        assertEquals(12, r[0].setsA); assertEquals(0, r[0].setsB); assertTrue(r[0].warn)
        assertFalse(r[1].warn)
    }

    @Test fun `plato quando o recorde tem 5 semanas e o exercicio foi treinado recentemente`() {
        val p = plateaus(listOf(set("Agachamento", 5, 100.0, 40), set("Agachamento", 5, 90.0, 3)), now)
        assertEquals(1, p.size); assertEquals(5, p[0].weeks)
        assertTrue(plateaus(listOf(set("Agachamento", 5, 90.0, 40), set("Agachamento", 5, 100.0, 3)), now).isEmpty())
    }
}

class TodayPanelTest {
    private val today = kotlinx.datetime.LocalDate(2026, 11, 22) // domingo

    @Test fun `volume por semana separa a atual da anterior`() {
        val v = weeklyVolumes(listOf(kotlinx.datetime.LocalDate(2026, 11, 20) to 100.0, kotlinx.datetime.LocalDate(2026, 11, 14) to 200.0, kotlinx.datetime.LocalDate(2026, 11, 22) to 50.0), today)
        assertEquals(12, v.size); assertEquals(150.0, v[11]); assertEquals(200.0, v[10])
    }

    @Test fun `descarga quando 4 semanas ficam acima da mediana`() {
        assertTrue(needsDeload(listOf(5.0, 5.0, 5.0, 5.0, 5.0, 5.0, 5.0, 5.0, 9.0, 9.0, 9.0, 9.0)))
        assertFalse(needsDeload(listOf(5.0, 5.0, 5.0, 5.0, 5.0, 5.0, 5.0, 5.0, 9.0, 9.0, 5.0, 9.0)))
        assertFalse(needsDeload(listOf(9.0, 9.0, 9.0)))
    }
}

class SearchTest {
    private val now = kotlin.time.Instant.parse("2026-11-22T12:00:00Z")
    private val sets = listOf(
        AnalysisSet("Rosca Direta", 10, 20.0, now),
        AnalysisSet("Supino Reto", 8, 80.0, now),
        AnalysisSet("Elevação Lateral", 12, 8.0, now)
    )

    @Test fun `pesquisa por nome sem acento`() {
        assertEquals(1, searchSets(sets, "ROSCA").size)
        assertEquals(1, searchSets(sets, "elevacao").size)
    }

    @Test fun `pesquisa por carga e consulta vazia`() {
        assertEquals("Supino Reto", searchSets(sets, "80kg").single().exercise)
        assertEquals("Elevação Lateral", searchSets(sets, "8").single().exercise)
        assertTrue(searchSets(sets, "  ").isEmpty())
    }
}
