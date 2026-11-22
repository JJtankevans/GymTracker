package com.vitoroliveira.gymtracker.presentation.body

import com.vitoroliveira.gymtracker.data.model.Workout
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

/** Um grupo e quantas vezes foi treinado no ano. */
data class GroupShare(val name: String, val workouts: Int)

/**
 * "Meu ano" (#539): o que a retrospectiva do ano mostra além dos totais — treinos por mês, melhor mês,
 * grupos mais treinados, dia da semana favorito e a maior sequência de dias seguidos.
 * [perMonth] sempre tem 12 posições (janeiro = 0).
 */
data class YearReview(
    val year: Int,
    val perMonth: List<Int>,
    val bestMonth: Int?,          // 1..12; null se não houve treino
    val topGroups: List<GroupShare>,
    val favoriteWeekday: DayOfWeek?,
    val longestStreakDays: Int,
    val longestStreakEnd: LocalDate?
) {
    val isEmpty: Boolean get() = perMonth.all { it == 0 }
}

private fun dateOf(iso: String?, zone: TimeZone): LocalDate? =
    iso?.let { runCatching { Instant.parse(it).toLocalDateTime(zone).date }.getOrNull() }

fun buildYearReview(
    workouts: List<Workout>,
    groupNames: Map<String, String>,
    year: Int,
    zone: TimeZone = TimeZone.currentSystemDefault(),
    topN: Int = 3
): YearReview {
    val dated = workouts.mapNotNull { w -> dateOf(w.startedAt, zone)?.takeIf { it.year == year }?.let { w to it } }
    val perMonth = MutableList(12) { 0 }
    dated.forEach { (_, d) -> perMonth[d.monthNumber - 1]++ }
    val best = perMonth.withIndex().maxByOrNull { it.value }?.takeIf { it.value > 0 }?.index?.plus(1)

    val groups = dated
        .groupingBy { (w, _) -> w.muscleGroupName ?: w.muscleGroupId?.let { groupNames[it] } ?: "Treino livre" }
        .eachCount()
        .entries.sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
        .take(topN).map { GroupShare(it.key, it.value) }

    val weekday = dated.map { it.second.dayOfWeek }.groupingBy { it }.eachCount()
        .entries.sortedWith(compareByDescending<Map.Entry<DayOfWeek, Int>> { it.value }.thenBy { it.key.ordinal })
        .firstOrNull()?.key

    // maior sequência de dias seguidos com treino (vários treinos no mesmo dia contam um)
    val days = dated.map { it.second }.distinct().sorted()
    var best1 = 0; var run = 0; var bestEnd: LocalDate? = null
    days.forEachIndexed { i, d ->
        run = if (i > 0 && days[i - 1].daysUntil(d) == 1) run + 1 else 1
        if (run > best1) { best1 = run; bestEnd = d }
    }
    return YearReview(year, perMonth, best, groups, weekday, best1, bestEnd)
}
