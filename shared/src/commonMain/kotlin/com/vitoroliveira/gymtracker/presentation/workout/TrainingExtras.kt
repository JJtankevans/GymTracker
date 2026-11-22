package com.vitoroliveira.gymtracker.presentation.workout

import com.vitoroliveira.gymtracker.presentation.body.estimateOneRepMax
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus

/** Porte de `web/src/training.ts` (22/11/2026): marcas de série, fases, lembrete, cardio e timer ao vivo. Funções puras. */

// ───────── marcas de série (aquecimento, drop-set, rest-pause, falha) ─────────
enum class SetFlag(val label: String) {
    WARMUP("Aquecimento"), DROP("Drop-set"), REST_PAUSE("Rest-pause"), FAILURE("Falha")
}

/** Séries de aquecimento não contam em volume nem recordes. */
fun countsForStats(flag: SetFlag?): Boolean = flag != SetFlag.WARMUP

// ───────── 1RM ao vivo ─────────
/** 1RM estimado (Epley) da série digitada; null se faltar dado. */
fun liveOneRm(kg: Double, reps: Int): Double? = if (kg > 0 && reps >= 1) estimateOneRepMax(kg, reps) else null

// ───────── fase do treino ─────────
enum class TrainingPhase(val label: String, val minReps: Int, val maxReps: Int, val tip: String) {
    AUTO("Automático", 0, 0, "Usa as repetições planejadas de cada exercício."),
    VOLUME("Volume", 8, 12, "Hipertrofia: 8 a 12 repetições, 10 a 20 séries por músculo na semana."),
    STRENGTH("Força", 3, 6, "Força: 3 a 6 repetições com cargas altas e descanso longo (2 a 4 min)."),
    CUT("Definição", 12, 15, "Definição: manter a carga, 12 a 15 repetições e mais cardio.")
}

/** Repetições-alvo da fase; mantém o alvo do exercício quando ele já cabe na faixa da fase. */
fun phaseTargetReps(phase: TrainingPhase, exerciseDefault: Int): Int =
    if (phase == TrainingPhase.AUTO || exerciseDefault in phase.minReps..phase.maxReps) exerciseDefault else phase.maxReps

// ───────── lembrete inteligente ─────────
/**
 * Mensagem quando a meta da semana ainda não foi batida e ainda há dias para cumprir; null caso contrário.
 * [daysLeftInWeek]: hoje conta (segunda = 7, domingo = 1).
 */
fun smartReminder(weeklyDone: Int, goal: Int, trainedToday: Boolean, daysLeftInWeek: Int, nextGroup: String?): String? {
    val missing = goal - weeklyDone
    if (missing <= 0 || trainedToday) return null
    val suffix = nextGroup?.let { " ($it)" } ?: ""
    return when {
        daysLeftInWeek < missing -> "Faltam $missing treinos e só restam $daysLeftInWeek ${if (daysLeftInWeek == 1) "dia" else "dias"}: hoje é dia de treinar$suffix."
        daysLeftInWeek == missing -> "Para bater a meta, treine em todos os dias que restam, inclusive hoje$suffix."
        else -> "Faltam $missing ${if (missing == 1) "treino" else "treinos"} para a meta e $daysLeftInWeek dias pela frente. Que tal hoje${nextGroup?.let { ": $it" } ?: ""}?"
    }
}

/** Dias que restam na semana (segunda a domingo), contando hoje: segunda = 7 … domingo = 1. */
fun daysLeftInWeek(today: LocalDate): Int = 7 - today.dayOfWeek.ordinal

// ───────── cardio ─────────
data class CardioEntry(val id: String, val day: LocalDate, val kind: String, val minutes: Int, val km: Double? = null)
data class CardioWeek(val minutes: Int, val km: Double, val sessions: Int)

/** Soma de cardio da semana corrente (segunda a [today]). */
fun weeklyCardio(entries: List<CardioEntry>, today: LocalDate): CardioWeek {
    val monday = today.minus(today.dayOfWeek.ordinal, DateTimeUnit.DAY)
    val week = entries.filter { it.day >= monday && it.day <= today }
    val km = week.sumOf { it.km ?: 0.0 }
    return CardioWeek(week.sumOf { it.minutes }, kotlin.math.round(km * 10) / 10, week.size)
}

/** Codifica/decodifica a lista para guardar em preferências: uma linha por sessão, campos separados por ';'. */
fun encodeCardio(entries: List<CardioEntry>): String =
    entries.joinToString("\n") { "${it.id};${it.day};${it.kind.replace(';', ',')};${it.minutes};${it.km ?: ""}" }

fun decodeCardio(text: String): List<CardioEntry> = text.lines().mapNotNull { line ->
    val p = line.split(';')
    if (p.size < 5) return@mapNotNull null
    val day = runCatching { LocalDate.parse(p[1]) }.getOrNull() ?: return@mapNotNull null
    val minutes = p[3].toIntOrNull() ?: return@mapNotNull null
    CardioEntry(p[0], day, p[2], minutes, p[4].toDoubleOrNull())
}

// ───────── timer de série ao vivo ─────────
data class LivePhase(val label: String, val seconds: Int, val rep: Int)
private val PHASE_NAMES = listOf("Descendo", "Pausa embaixo", "Subindo", "Pausa em cima")

/** Fases em ordem para [reps] repetições com a cadência dada (fases de 0 s são puladas). */
fun liveSchedule(tempo: List<Int>, reps: Int): List<LivePhase> = buildList {
    for (r in 1..maxOf(0, reps)) tempo.forEachIndexed { i, s -> if (s > 0) add(LivePhase(PHASE_NAMES[i], s, r)) }
}

data class LivePosition(val phase: LivePhase, val left: Int, val index: Int)

/** Fase em andamento após [elapsed] segundos (null = a série terminou) e quantos segundos faltam nela. */
fun phaseAt(schedule: List<LivePhase>, elapsed: Double): LivePosition? {
    var t = 0.0
    schedule.forEachIndexed { i, p ->
        if (elapsed < t + p.seconds) return LivePosition(p, kotlin.math.ceil(t + p.seconds - elapsed).toInt(), i)
        t += p.seconds
    }
    return null
}

fun scheduleSeconds(schedule: List<LivePhase>): Int = schedule.sumOf { it.seconds }
