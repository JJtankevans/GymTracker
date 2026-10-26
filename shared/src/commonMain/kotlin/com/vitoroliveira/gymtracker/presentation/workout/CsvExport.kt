package com.vitoroliveira.gymtracker.presentation.workout

import com.vitoroliveira.gymtracker.data.model.ExportSet
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

/**
 * CSV do histórico, pensado para abrir direto no Excel/Planilhas em português:
 * separador ";" e decimal com vírgula. Uma linha por série, ordenado por data.
 * Colunas: data;hora;grupo;exercicio;serie;reps;carga_kg;volume_kg
 */
fun historyCsv(
    sets: List<ExportSet>,
    exerciseNames: Map<String, String>,
    groupNames: Map<String, String>,
    zone: TimeZone = TimeZone.currentSystemDefault()
): String = buildString {
    appendLine("data;hora;grupo;exercicio;serie;reps;carga_kg;volume_kg")
    sets.sortedWith(compareBy<ExportSet> { it.workouts?.startedAt.orEmpty() }.thenBy { it.completedAt.orEmpty() }.thenBy { it.setNumber })
        .forEach { s ->
            val started = s.workouts?.startedAt?.let { runCatching { Instant.parse(it).toLocalDateTime(zone) }.getOrNull() }
            val date = started?.date?.let { "${it.dayOfMonth.toString().padStart(2, '0')}/${it.monthNumber.toString().padStart(2, '0')}/${it.year}" }.orEmpty()
            val time = started?.let { "${it.hour.toString().padStart(2, '0')}:${it.minute.toString().padStart(2, '0')}" }.orEmpty()
            val group = s.workouts?.muscleGroupId?.let { groupNames[it] } ?: s.workouts?.muscleGroupName ?: ""
            val exercise = exerciseNames[s.exerciseId] ?: "Exercício removido"
            appendLine(
                listOf(date, time, group, exercise, s.setNumber.toString(), s.reps.toString(), decimal(s.weightKg), decimal(s.reps * s.weightKg))
                    .joinToString(";") { csvCell(it) }
            )
        }
}

private fun decimal(v: Double): String = formatKgBr(v)

/** Aspas só quando precisa (separador, aspas ou quebra de linha), dobrando as aspas internas. */
internal fun csvCell(v: String): String =
    if (v.any { it == ';' || it == '"' || it == '\n' || it == '\r' }) "\"" + v.replace("\"", "\"\"") + "\"" else v
