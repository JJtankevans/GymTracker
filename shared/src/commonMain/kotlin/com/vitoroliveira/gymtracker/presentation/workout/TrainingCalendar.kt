package com.vitoroliveira.gymtracker.presentation.workout

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus

/** Um quadradinho do calendário: o dia e quantos treinos começaram nele. */
data class CalendarDay(val date: LocalDate, val workouts: Int)

/**
 * Mapa de calor das últimas semanas (estilo GitHub): uma coluna por semana, de segunda (0) a domingo (6).
 * Dias depois de hoje ficam null (a semana atual fica incompleta).
 */
data class TrainingCalendar(
    val weeks: List<List<CalendarDay?>>,
    /** Rótulo do mês em cima da coluna onde o mês começa (e na primeira coluna); null nas outras. */
    val monthLabels: List<String?>,
    val activeDays: Int,
    val totalWorkouts: Int
)

private val MONTHS = listOf("jan", "fev", "mar", "abr", "mai", "jun", "jul", "ago", "set", "out", "nov", "dez")

fun buildTrainingCalendar(days: List<LocalDate>, today: LocalDate, weeks: Int = 18): TrainingCalendar {
    val counts = days.groupingBy { it }.eachCount()
    val thisMonday = today.minus(today.dayOfWeek.isoDayNumber - 1, DateTimeUnit.DAY)
    val firstMonday = thisMonday.minus((weeks - 1) * 7, DateTimeUnit.DAY)
    val grid = (0 until weeks).map { w ->
        (0 until 7).map { d ->
            val date = firstMonday.plus(w * 7 + d, DateTimeUnit.DAY)
            if (date > today) null else CalendarDay(date, counts[date] ?: 0)
        }
    }
    val labels = grid.mapIndexed { i, col ->
        val monday = firstMonday.plus(i * 7, DateTimeUnit.DAY)
        val sunday = monday.plus(6, DateTimeUnit.DAY)
        when {
            i == 0 -> MONTHS[monday.monthNumber - 1]
            sunday.dayOfMonth <= 7 -> MONTHS[sunday.monthNumber - 1]   // o dia 1 cai nesta semana
            else -> null
        }.takeIf { col.isNotEmpty() }
    }
    val inRange = counts.filterKeys { it >= firstMonday && it <= today }
    return TrainingCalendar(grid, labels, inRange.size, inRange.values.sum())
}
