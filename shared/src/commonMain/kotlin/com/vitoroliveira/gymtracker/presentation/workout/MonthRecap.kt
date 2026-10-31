package com.vitoroliveira.gymtracker.presentation.workout

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus

/** Um treino para o resumo do mês: dia (no fuso do usuário), duração e nome do grupo. */
data class RecapSession(val date: LocalDate, val minutes: Long?, val group: String)

/** Resumo do mês corrente comparado com o anterior (cartão do Histórico). */
data class MonthRecap(
    /** "outubro" */
    val monthName: String,
    val previousMonthName: String,
    val workouts: Int,
    val previousWorkouts: Int,
    val minutes: Long,
    val previousMinutes: Long,
    val activeDays: Int,
    /** Grupo mais treinado no mês e quantas vezes (null sem treinos). */
    val topGroup: String?,
    val topGroupCount: Int,
    val longestMinutes: Long?
) {
    val workoutsDelta: Int get() = workouts - previousWorkouts
}

private val MONTHS = listOf(
    "janeiro", "fevereiro", "março", "abril", "maio", "junho",
    "julho", "agosto", "setembro", "outubro", "novembro", "dezembro"
)

/** Monta o resumo do mês de [today] e do mês anterior a partir dos treinos terminados. */
fun monthRecap(sessions: List<RecapSession>, today: LocalDate): MonthRecap {
    val prev = today.minus(1, DateTimeUnit.MONTH)
    fun inMonth(d: LocalDate, ref: LocalDate) = d.year == ref.year && d.month == ref.month
    val now = sessions.filter { inMonth(it.date, today) }
    val before = sessions.filter { inMonth(it.date, prev) }
    // treinos "sem grupo" não disputam o mais treinado
    val top = now.filter { it.group != "Sem grupo" }.groupingBy { it.group }.eachCount().maxByOrNull { it.value }
    return MonthRecap(
        monthName = MONTHS[today.month.ordinal],
        previousMonthName = MONTHS[prev.month.ordinal],
        workouts = now.size,
        previousWorkouts = before.size,
        minutes = now.sumOf { it.minutes ?: 0L },
        previousMinutes = before.sumOf { it.minutes ?: 0L },
        activeDays = now.map { it.date }.distinct().size,
        topGroup = top?.key,
        topGroupCount = top?.value ?: 0,
        longestMinutes = now.mapNotNull { it.minutes }.maxOrNull()
    )
}

/** "9 h 20 min", "45 min". */
fun formatMinutes(total: Long): String {
    val h = total / 60
    val m = total % 60
    return when {
        h == 0L -> "$m min"
        m == 0L -> "$h h"
        else -> "$h h $m min"
    }
}
