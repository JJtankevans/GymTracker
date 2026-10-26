package com.vitoroliveira.gymtracker.presentation.workout

import com.vitoroliveira.gymtracker.data.model.Workout
import com.vitoroliveira.gymtracker.presentation.social.formatInt
import com.vitoroliveira.gymtracker.presentation.viewmodel.ExerciseSetGroup
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.math.roundToLong
import kotlin.time.Instant

/** Números de um treino já registrado (cabeçalho do detalhe e texto de compartilhar). */
data class WorkoutSummary(
    /** "26/10/2026", ou null se a data não veio. */
    val date: String?,
    val durationMinutes: Long?,
    val sets: Int,
    val reps: Int,
    /** Soma de reps × carga de todas as séries, em kg. */
    val volumeKg: Double
)

/** Carga sem casas desnecessárias e com vírgula: 40.0 -> "40", 42.5 -> "42,5". */
fun formatKgBr(kg: Double): String {
    val cents = (kg * 100).roundToLong()
    return if (cents % 100 == 0L) (cents / 100).toString()
    else (cents / 100.0).toString().trimEnd('0').replace('.', ',')
}

fun summarize(workout: Workout?, groups: List<ExerciseSetGroup>, zone: TimeZone = TimeZone.currentSystemDefault()): WorkoutSummary {
    val start = workout?.startedAt?.let { runCatching { Instant.parse(it) }.getOrNull() }
    val end = workout?.finishedAt?.let { runCatching { Instant.parse(it) }.getOrNull() }
    val date = start?.toLocalDateTime(zone)?.date?.let {
        "${it.dayOfMonth.toString().padStart(2, '0')}/${it.monthNumber.toString().padStart(2, '0')}/${it.year}"
    }
    val sets = groups.flatMap { it.sets }
    return WorkoutSummary(
        date = date,
        durationMinutes = if (start != null && end != null && end > start) (end - start).inWholeMinutes else null,
        sets = sets.size,
        reps = sets.sumOf { it.reps },
        volumeKg = sets.sumOf { it.reps * it.weightKg }
    )
}

/**
 * Texto para mandar no WhatsApp/Instagram etc.:
 *   Treino de Pernas · 26/10/2026 · 62 min
 *   • Cadeira Extensora: 12×40, 10×42,5 kg
 *   ...
 *   12 séries · 4.320 kg de volume
 */
fun shareText(groupName: String?, workout: Workout?, groups: List<ExerciseSetGroup>, zone: TimeZone = TimeZone.currentSystemDefault()): String {
    val s = summarize(workout, groups, zone)
    val title = buildString {
        append(groupName?.takeIf { it.isNotBlank() && it != "Sem grupo" }?.let { "Treino de $it" } ?: "Treino")
        s.date?.let { append(" · $it") }
        s.durationMinutes?.let { append(" · $it min") }
    }
    val lines = groups.map { g ->
        val sets = g.sets.joinToString(", ") { set ->
            if (set.weightKg > 0) "${set.reps}×${formatKgBr(set.weightKg)}" else "${set.reps} reps"
        }
        val unit = if (g.sets.any { it.weightKg > 0 }) " kg" else ""
        "• ${g.exerciseName}: $sets$unit"
    }
    val total = "${s.sets} ${if (s.sets == 1) "série" else "séries"}" +
        if (s.volumeKg > 0) " · ${formatInt(s.volumeKg)} kg de volume" else ""
    return buildString {
        appendLine(title)
        lines.forEach { appendLine(it) }
        appendLine(total)
        workout?.notes?.takeIf { it.isNotBlank() }?.let { appendLine("Nota: ${it.trim()}") }
        append("Registrado no GymTracker")
    }
}
