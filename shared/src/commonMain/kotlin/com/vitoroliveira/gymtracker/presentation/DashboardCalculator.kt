package com.vitoroliveira.gymtracker.presentation

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus

/** Quantidade de treinos em uma semana (segunda a domingo). */
data class WeekBucket(val weekStart: LocalDate, val count: Int)

/**
 * Regras do dashboard como funções puras (sem relógio, sem rede),
 * pra poderem ser testadas com datas fixas.
 */
object DashboardCalculator {

    /** Segunda-feira da semana de [date] (mesmo critério do DATE_TRUNC('week') do Postgres). */
    fun weekStart(date: LocalDate): LocalDate =
        date.minus(date.dayOfWeek.isoDayNumber - 1, DateTimeUnit.DAY)

    /** Treinos na semana atual. Múltiplos treinos no mesmo dia contam todos. */
    fun weeklyCount(workoutDates: List<LocalDate>, today: LocalDate): Int {
        val start = weekStart(today)
        val end = start.plus(6, DateTimeUnit.DAY)
        return workoutDates.count { it in start..end }
    }

    /**
     * Dias consecutivos com treino terminando hoje ou ontem
     * (quem ainda não treinou hoje não perde a sequência até o fim do dia).
     */
    fun streak(workoutDates: List<LocalDate>, today: LocalDate): Int {
        if (workoutDates.isEmpty()) return 0
        val days = workoutDates.distinct().sortedDescending()
        val mostRecent = days.first()
        if (mostRecent < today.minus(1, DateTimeUnit.DAY)) return 0
        if (mostRecent > today) return 0 // data futura = dado inválido

        var streak = 1
        var expected = mostRecent.minus(1, DateTimeUnit.DAY)
        for (d in days.drop(1)) {
            if (d == expected) {
                streak++
                expected = expected.minus(1, DateTimeUnit.DAY)
            } else if (d < expected) break
        }
        return streak
    }

    /** As últimas [weeks] semanas (a mais antiga primeiro), incluindo semanas sem treino. */
    fun weeklyBuckets(workoutDates: List<LocalDate>, today: LocalDate, weeks: Int = 8): List<WeekBucket> {
        require(weeks > 0)
        val thisWeek = weekStart(today)
        return (weeks - 1 downTo 0).map { back ->
            val start = thisWeek.minus(back * 7, DateTimeUnit.DAY)
            val end = start.plus(6, DateTimeUnit.DAY)
            WeekBucket(start, workoutDates.count { it in start..end })
        }
    }
}
