package com.vitoroliveira.gymtracker.presentation.body

import com.vitoroliveira.gymtracker.data.model.ExportSet
import com.vitoroliveira.gymtracker.data.model.Workout
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

/** Retrospectiva de um mês (#354): números do período e o grupo que mais apareceu. */
data class MonthlyRecap(
    val year: Int,
    val month: Int,
    val workouts: Int,
    val daysTrained: Int,
    val sets: Int,
    val reps: Int,
    val volumeKg: Double,
    val topGroup: String?,
    val topGroupWorkouts: Int
) {
    val isEmpty: Boolean get() = workouts == 0
}

private fun dateOf(iso: String?, zone: TimeZone): LocalDate? =
    iso?.let { runCatching { Instant.parse(it).toLocalDateTime(zone).date }.getOrNull() }

/**
 * Monta a retrospectiva de [year]/[month] a partir dos treinos finalizados e de todas as séries.
 * [groupNames] traduz `muscle_group_id` em nome; treino sem grupo conta como "Treino livre".
 */
fun buildMonthlyRecap(
    workouts: List<Workout>,
    sets: List<ExportSet>,
    groupNames: Map<String, String>,
    year: Int,
    month: Int,
    zone: TimeZone = TimeZone.currentSystemDefault()
): MonthlyRecap = buildRecap(workouts, sets, groupNames, year, month, zone)

/** Retrospectiva do ano inteiro (#354): mesmos números, somando os 12 meses; `month` vem 0. */
fun buildYearlyRecap(
    workouts: List<Workout>,
    sets: List<ExportSet>,
    groupNames: Map<String, String>,
    year: Int,
    zone: TimeZone = TimeZone.currentSystemDefault()
): MonthlyRecap = buildRecap(workouts, sets, groupNames, year, null, zone)

private fun buildRecap(
    workouts: List<Workout>,
    sets: List<ExportSet>,
    groupNames: Map<String, String>,
    year: Int,
    month: Int?,
    zone: TimeZone
): MonthlyRecap {
    fun inMonth(d: LocalDate?) = d != null && d.year == year && (month == null || d.monthNumber == month)
    val ws = workouts.filter { inMonth(dateOf(it.startedAt, zone)) }
    val days = ws.mapNotNull { dateOf(it.startedAt, zone) }.distinct().size
    val ss = sets.filter { inMonth(dateOf(it.workouts?.startedAt, zone)) }
    val top = ws
        .groupingBy { w -> w.muscleGroupName ?: w.muscleGroupId?.let { groupNames[it] } ?: "Treino livre" }
        .eachCount()
        .maxByOrNull { it.value }
    return MonthlyRecap(
        year = year, month = month ?: 0, workouts = ws.size, daysTrained = days,
        sets = ss.size, reps = ss.sumOf { it.reps }, volumeKg = ss.sumOf { it.reps * it.weightKg },
        topGroup = top?.key, topGroupWorkouts = top?.value ?: 0
    )
}
