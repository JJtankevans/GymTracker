package com.vitoroliveira.gymtracker.presentation

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
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

    // ---- sequência de semanas com a meta ----
    // quarta = 21/10/2026; semanas começam em 19/10, 12/10, 5/10, 28/9...
    private fun d(day: Int, month: Int = 10) = LocalDate(2026, month, day)
    private val tresNaSemana = { start: LocalDate -> listOf(start, start.plus(1, DateTimeUnit.DAY), start.plus(2, DateTimeUnit.DAY)) }

    @Test fun semanasSeguidasSemTreinoEhZero() = assertEquals(0, DashboardCalculator.weekStreak(emptyList(), quarta, 3))

    @Test fun semanaAtualEmAndamentoNaoQuebraASequencia() {
        val dates = tresNaSemana(d(12)) + tresNaSemana(d(5)) + listOf(d(19)) // atual com 1 de 3
        assertEquals(2, DashboardCalculator.weekStreak(dates, quarta, 3))
    }

    @Test fun semanaAtualQueJaBateuAMetaConta() {
        val dates = tresNaSemana(d(19)) + tresNaSemana(d(12))
        assertEquals(2, DashboardCalculator.weekStreak(dates, quarta, 3))
    }

    @Test fun semanaFracaNoMeioQuebra() {
        val dates = tresNaSemana(d(12)) + listOf(d(7)) + tresNaSemana(d(28, 9)) // 5/10 com 1 treino
        assertEquals(1, DashboardCalculator.weekStreak(dates, quarta, 3))
    }

    @Test fun semanaAnteriorFracaESemanaAtualFracaZera() {
        assertEquals(0, DashboardCalculator.weekStreak(listOf(d(13), d(19)), quarta, 3))
    }

    @Test fun metaZeroOuNegativaViraUmTreinoPorSemana() {
        assertEquals(2, DashboardCalculator.weekStreak(listOf(d(13), d(7)), quarta, 0))
    }

    @Test fun treinoFuturoNaoContaNaSequencia() {
        assertEquals(0, DashboardCalculator.weekStreak(tresNaSemana(d(26)), quarta, 3))
    }
}
