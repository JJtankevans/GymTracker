package com.vitoroliveira.gymtracker.presentation.ranked

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/*
 * Regras da ranqueada (liga com elos). O banco é quem decide de verdade (migração 018, funções ranked_*), porque os
 * pontos vêm dos treinos reais e ninguém pode mandar o próprio número. Este arquivo é a MESMA conta em Kotlin, para
 * mostrar na tela ("faltam 40 pontos"), simular e testar. Mudou uma regra aqui? Mude a função SQL correspondente.
 * Ver docs/RANQUEADA_DESIGN.md.
 */

// ───────────────────────── Elos ─────────────────────────

enum class RankTier(val label: String, val colorHex: Long) {
    IRON("Ferro", 0xFF8A8F98),
    BRONZE("Bronze", 0xFFCD7F32),
    SILVER("Prata", 0xFFC9D1D9),
    GOLD("Ouro", 0xFFFFC53D),
    PLATINUM("Platina", 0xFF3EE0C5),
    DIAMOND("Diamante", 0xFF5CA8FF),
    LEGEND("Lenda", 0xFFFF3D6E)
}

const val RP_PER_DIVISION = 100
const val DIVISIONS_PER_TIER = 3
const val RP_PER_TIER = RP_PER_DIVISION * DIVISIONS_PER_TIER

/** RP a partir do qual alguém é Lenda (sem divisões): 6 elos x 300. */
const val LEGEND_RP = 6 * RP_PER_TIER

/**
 * Elo de alguém com [rp] pontos de rank. Ferro a Diamante têm 3 divisões (III, II, I) de 100 RP; Lenda é o topo, sem
 * divisões, e os RP continuam subindo. [division] vai de 3 (início do elo) a 1 (fim); 0 em Lenda.
 */
data class Rank(val tier: RankTier, val division: Int, val rp: Int) {
    val isTop: Boolean get() = tier == RankTier.LEGEND
    /** "Ouro II", "Lenda". */
    val label: String get() = if (isTop) tier.label else "${tier.label} ${roman(division)}"
    /** RP dentro da divisão atual (0 a 99); em Lenda, os RP acima de [LEGEND_RP]. */
    val rpInDivision: Int get() = if (isTop) rp - LEGEND_RP else rp % RP_PER_DIVISION
    /** 0 a 1 até a próxima divisão (em Lenda, sempre 1). */
    val progress: Float get() = if (isTop) 1f else rpInDivision / RP_PER_DIVISION.toFloat()
    /** RP que faltam para a próxima divisão ou elo; 0 em Lenda. */
    val rpToNext: Int get() = if (isTop) 0 else RP_PER_DIVISION - rpInDivision
}

private fun roman(n: Int) = when (n) { 1 -> "I"; 2 -> "II"; else -> "III" }

fun rankFor(rp: Int): Rank {
    val safe = max(0, rp)
    if (safe >= LEGEND_RP) return Rank(RankTier.LEGEND, 0, safe)
    val tier = RankTier.entries[safe / RP_PER_TIER]
    val division = DIVISIONS_PER_TIER - (safe % RP_PER_TIER) / RP_PER_DIVISION
    return Rank(tier, division, safe)
}

// ───────────────────────── Pontos de treino da semana ─────────────────────────

/** Treino terminado, para a conta de pontos: dia (fuso de Brasília), minutos e séries com repetições. */
data class RankedWorkout(val day: LocalDate, val minutes: Double, val sets: Int)

const val MIN_WORKOUT_MINUTES = 20.0
/** Acima disso o treino não vale (provavelmente esquecido aberto). */
const val MAX_WORKOUT_MINUTES = 240.0
const val BASE_POINTS = 100
/** 1 ponto a cada 2 minutos além de 20, até 80 min: no máximo +30. */
const val EFFORT_FULL_MINUTES = 80.0
const val MAX_EFFORT_POINTS = 30
const val POINTS_PER_SET = 2
const val MAX_SETS_COUNTED = 20
/** Só o melhor treino de cada dia conta, e no máximo 6 dias por semana. */
const val MAX_DAYS_COUNTED = 6
const val CONSISTENCY_DAYS = 3
const val CONSISTENCY_BONUS = 50
const val GOAL_BONUS = 50

/** Pontos de um treino: 0 se tem menos de 20 min ou mais de 240 min; senão 100 + esforço (até 30) + 2 por série (até 20 séries). */
fun workoutPoints(minutes: Double, sets: Int): Int {
    if (minutes < MIN_WORKOUT_MINUTES || minutes > MAX_WORKOUT_MINUTES) return 0
    val effort = min(MAX_EFFORT_POINTS, floor((min(minutes, EFFORT_FULL_MINUTES) - MIN_WORKOUT_MINUTES) / 2).toInt())
    return BASE_POINTS + effort + POINTS_PER_SET * min(MAX_SETS_COUNTED, max(0, sets))
}

/**
 * Pontos da semana: o melhor treino de cada dia, os 6 melhores dias, +50 por treinar em 3 dias ou mais
 * e +50 por bater a meta semanal do perfil ([weeklyGoal], em dias com treino; 0 = sem meta).
 */
fun weekPoints(workouts: List<RankedWorkout>, weeklyGoal: Int): Int {
    val perDay = workouts.groupBy { it.day }.mapValues { (_, list) -> list.maxOf { workoutPoints(it.minutes, it.sets) } }
        .filterValues { it > 0 }
    if (perDay.isEmpty()) return 0
    val base = perDay.values.sortedDescending().take(MAX_DAYS_COUNTED).sum()
    val days = perDay.size
    return base + (if (days >= CONSISTENCY_DAYS) CONSISTENCY_BONUS else 0) + (if (weeklyGoal > 0 && days >= weeklyGoal) GOAL_BONUS else 0)
}

// ───────────────────────── Fechamento da semana ─────────────────────────

/** Tamanho máximo de uma turma (as pessoas do mesmo elo que disputam a mesma semana). */
const val COHORT_SIZE = 30
/** Turmas menores que isso não têm percentis: só quem treinou ganha. */
const val MIN_COHORT_FOR_PERCENTILES = 5
/** Percentuais do topo que sobe e do fundo que desce (contas em inteiro, para o SQL dar o mesmo resultado). */
const val PROMOTE_PERCENT = 20
const val DEMOTE_PERCENT = 20

const val RP_FIRST = 40
const val RP_PROMOTE = 30
const val RP_UPPER_MIDDLE = 10
const val RP_LOWER_MIDDLE = -10
const val RP_DEMOTE = -25
const val RP_NO_TRAINING = -35
const val RP_SMALL_COHORT_WIN = 10
const val RP_SMALL_COHORT_LOSS = -20

enum class Zone { PROMOTE, SAFE, DEMOTE }

/** Quantas posições do topo sobem e quantas do fundo descem numa turma de [size]. */
fun promoteCount(size: Int): Int = if (size < MIN_COHORT_FOR_PERCENTILES) 0 else (size * PROMOTE_PERCENT + 99) / 100
fun demoteCount(size: Int): Int = if (size < MIN_COHORT_FOR_PERCENTILES) 0 else (size * DEMOTE_PERCENT + 99) / 100

/**
 * Faixa da posição [position] (1 = primeiro; empate divide a posição) numa turma de [size]: o topo (20%) sobe, o fundo
 * (20%) desce. Quem fez 0 pontos nunca fica na faixa de subida.
 */
fun zoneFor(position: Int, size: Int, points: Int): Zone = when {
    points <= 0 -> Zone.DEMOTE
    position <= promoteCount(size) -> Zone.PROMOTE
    size >= MIN_COHORT_FOR_PERCENTILES && position > size - demoteCount(size) -> Zone.DEMOTE
    else -> Zone.SAFE
}

/** Variação de RP no fechamento da semana. */
fun settleDelta(position: Int, size: Int, points: Int): Int {
    if (points <= 0) return RP_NO_TRAINING
    if (size < MIN_COHORT_FOR_PERCENTILES) return if (points >= BASE_POINTS) RP_SMALL_COHORT_WIN else RP_SMALL_COHORT_LOSS
    return when {
        position == 1 -> RP_FIRST
        position <= promoteCount(size) -> RP_PROMOTE
        position > size - demoteCount(size) -> RP_DEMOTE
        position <= (size + 1) / 2 -> RP_UPPER_MIDDLE
        else -> RP_LOWER_MIDDLE
    }
}

/** Posição de cada pontuação numa turma: 1 = maior; empates dividem a posição (1, 1, 3...). */
fun positions(points: List<Int>): List<Int> = points.map { p -> 1 + points.count { it > p } }

// ───────────────────────── x1 ─────────────────────────

const val DUEL_K = 24
const val DUEL_MAX_DELTA = 35
const val DUEL_DAYS = 7

/**
 * Variação de RP de [rpSelf] num x1 contra [rpOpp] (Elo): vitória vale 1, empate 0,5, derrota 0; K = 24,
 * limitado a ±35. Amistoso ([ranked] = false) e duelo em que os dois fizeram 0 pontos não mexem em RP.
 */
fun duelDelta(rpSelf: Int, rpOpp: Int, ptSelf: Int, ptOpp: Int, ranked: Boolean = true): Int {
    if (!ranked || (ptSelf == 0 && ptOpp == 0)) return 0
    val score = when { ptSelf > ptOpp -> 1.0; ptSelf < ptOpp -> 0.0; else -> 0.5 }
    val expected = 1.0 / (1.0 + 10.0.pow((rpOpp - rpSelf) / 400.0))
    val raw = DUEL_K * (score - expected)
    return min(DUEL_MAX_DELTA, max(-DUEL_MAX_DELTA, floor(raw + 0.5).toInt()))
}

/** Duelos valem entre pessoas do mesmo elo (divisões diferentes podem). */
fun canDuel(rpA: Int, rpB: Int): Boolean = rankFor(rpA).tier == rankFor(rpB).tier

// ───────────────────────── Temporadas ─────────────────────────

const val SEASON_WEEKS = 8
/** Segunda-feira em que a temporada 1 começou. */
val SEASON_EPOCH: LocalDate = LocalDate(2026, 10, 26)

data class SeasonInfo(val number: Int, val weekInSeason: Int) {
    val isLastWeek: Boolean get() = weekInSeason == SEASON_WEEKS
}

/** Temporada e semana dentro dela da semana que começa em [weekStart] (uma segunda-feira). */
fun seasonOf(weekStart: LocalDate): SeasonInfo {
    val weeks = max(0, SEASON_EPOCH.daysUntil(weekStart) / 7)
    return SeasonInfo(weeks / SEASON_WEEKS + 1, weeks % SEASON_WEEKS + 1)
}

/** No fim da temporada o RP encolhe 40% (fica com 60%): todo mundo recomeça um pouco mais embaixo. */
fun softReset(rp: Int): Int = max(0, rp) * 6 / 10

/** Segunda-feira da semana de [date]. */
fun mondayOf(date: LocalDate): LocalDate = date.minus(date.dayOfWeek.isoDayNumber - 1, DateTimeUnit.DAY)
