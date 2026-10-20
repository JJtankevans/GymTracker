package com.vitoroliveira.gymtracker.presentation

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class DashboardCalculatorTest {

    // 2026-10-21 é quarta-feira → a semana começa na segunda 2026-10-19
    private val quarta = LocalDate(2026, 10, 21)

    @Test fun semanaComecaNaSegunda() {
        assertEquals(LocalDate(2026, 10, 19), DashboardCalculator.weekStart(quarta))
        assertEquals(LocalDate(2026, 10, 19), DashboardCalculator.weekStart(LocalDate(2026, 10, 19)))
        assertEquals(LocalDate(2026, 10, 19), DashboardCalculator.weekStart(LocalDate(2026, 10, 25))) // domingo
    }

    @Test fun contagemSemanalIgnoraOutrasSemanas() {
        val dates = listOf(
            LocalDate(2026, 10, 18), // domingo da semana passada
            LocalDate(2026, 10, 19), // segunda
            LocalDate(2026, 10, 21),
            LocalDate(2026, 10, 25), // domingo
            LocalDate(2026, 10, 26)  // segunda da semana que vem
        )
        assertEquals(3, DashboardCalculator.weeklyCount(dates, quarta))
    }

    @Test fun doisTreinosNoMesmoDiaContamDois() {
        assertEquals(2, DashboardCalculator.weeklyCount(listOf(quarta, quarta), quarta))
    }

    @Test fun streakVazio() = assertEquals(0, DashboardCalculator.streak(emptyList(), quarta))

    @Test fun streakTerminandoHoje() {
        val d = listOf(quarta, LocalDate(2026, 10, 20), LocalDate(2026, 10, 19))
        assertEquals(3, DashboardCalculator.streak(d, quarta))
    }

    @Test fun streakTerminandoOntemAindaVale() {
        val d = listOf(LocalDate(2026, 10, 20), LocalDate(2026, 10, 19))
        assertEquals(2, DashboardCalculator.streak(d, quarta))
    }

    @Test fun streakQuebraComBuraco() {
        val d = listOf(quarta, LocalDate(2026, 10, 19)) // faltou dia 20
        assertEquals(1, DashboardCalculator.streak(d, quarta))
    }

    @Test fun streakZeradaSeUltimoTreinoFoiAntesDeOntem() {
        assertEquals(0, DashboardCalculator.streak(listOf(LocalDate(2026, 10, 18)), quarta))
    }

    @Test fun streakIgnoraDuplicadosNoMesmoDia() {
        val d = listOf(quarta, quarta, LocalDate(2026, 10, 20))
        assertEquals(2, DashboardCalculator.streak(d, quarta))
    }

    @Test fun streakIgnoraDataFutura() {
        assertEquals(0, DashboardCalculator.streak(listOf(LocalDate(2026, 10, 30)), quarta))
    }

    @Test fun bucketsSemanaisIncluemSemanasVazias() {
        val d = listOf(quarta, LocalDate(2026, 10, 5), LocalDate(2026, 10, 6))
        val b = DashboardCalculator.weeklyBuckets(d, quarta, weeks = 4)
        assertEquals(4, b.size)
        assertEquals(LocalDate(2026, 9, 28), b.first().weekStart)
        assertEquals(LocalDate(2026, 10, 19), b.last().weekStart)
        assertEquals(listOf(0, 2, 0, 1), b.map { it.count })
    }
}
