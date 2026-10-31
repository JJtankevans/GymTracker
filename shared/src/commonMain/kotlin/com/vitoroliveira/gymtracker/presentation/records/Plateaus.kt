package com.vitoroliveira.gymtracker.presentation.records

import com.vitoroliveira.gymtracker.data.model.RecentSet
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

/** Janela olhada para achar estagnação: 6 semanas. */
const val PLATEAU_DAYS = 42

/**
 * Exercício estagnado: o recorde de carga tem [weeksSince] semanas e, desde então, você treinou ele
 * [sessionsSince] vezes sem passar. [topRecentKg] é a maior carga nesses treinos.
 */
data class Plateau(
    val exerciseId: String,
    val exerciseName: String,
    val bestKg: Double,
    val bestDate: String,
    val weeksSince: Int,
    val sessionsSince: Int,
    val topRecentKg: Double,
    val tip: String
)

/**
 * Acha os exercícios parados: recorde com mais de [PLATEAU_DAYS] dias e pelo menos [minSessions] treinos dele
 * nos últimos [PLATEAU_DAYS] dias (quem parou de fazer o exercício não está estagnado, só parou).
 * [recentSets] são as séries dos últimos [PLATEAU_DAYS] dias. Os mais antigos primeiro, no máximo [limit].
 */
fun findPlateaus(
    records: List<RecordRow>,
    recentSets: List<RecentSet>,
    today: LocalDate,
    zone: TimeZone,
    minSessions: Int = 3,
    limit: Int = 5
): List<Plateau> {
    val byExercise = recentSets.groupBy { it.exerciseId }
    return records.mapNotNull { r ->
        val best = runCatching { LocalDate.parse(r.bestDate) }.getOrNull() ?: return@mapNotNull null
        val days = best.daysUntil(today)
        if (days < PLATEAU_DAYS) return@mapNotNull null
        val sets = byExercise[r.exerciseId].orEmpty().filter { it.weightKg > 0 }
        val sessions = sets.mapNotNull { s ->
            s.workouts?.startedAt?.let { runCatching { Instant.parse(it).toLocalDateTime(zone).date }.getOrNull() }
        }.filter { it > best }.toSet().size
        if (sessions < minSessions) return@mapNotNull null
        val top = sets.maxOf { it.weightKg }
        Plateau(r.exerciseId, r.exerciseName, r.bestWeightKg, r.bestDate, days / 7, sessions, top, plateauTip(r.bestWeightKg, top, sessions))
    }
        .sortedByDescending { it.weeksSince }
        .take(limit)
}

/** Uma sugestão curta do que mudar, pela distância do recorde e por quanto tempo está parado. */
fun plateauTip(bestKg: Double, topRecentKg: Double, sessions: Int): String = when {
    topRecentKg < bestKg * 0.9 ->
        "Você está bem abaixo do recorde: confira sono e descanso, ou faça uma semana mais leve (deload, ~60% da carga) e volte."
    sessions >= 6 ->
        "Troque a faixa de repetições por 4 semanas (ex.: de 8–12 para 5–6) ou use uma variação do exercício."
    else ->
        "Antes de subir a carga, tente 1 repetição a mais em cada série (dupla progressão)."
}
