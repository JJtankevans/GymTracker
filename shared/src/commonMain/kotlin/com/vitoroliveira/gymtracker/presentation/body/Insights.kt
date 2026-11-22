package com.vitoroliveira.gymtracker.presentation.body

import com.vitoroliveira.gymtracker.data.catalog.CatalogMuscle
import com.vitoroliveira.gymtracker.data.catalog.ExerciseCatalog
import kotlin.math.roundToInt
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.daysUntil
import kotlinx.datetime.minus
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

/**
 * Análises (porte de `web/src/insights.ts`): recordes por faixa, desequilíbrio, platôs, sobrecarga progressiva e calorias.
 * Todas puras (sem relógio nem rede) para testar no :shared.
 */
data class AnalysisSet(val exercise: String, val reps: Int, val kg: Double, val at: Instant, val workoutId: String? = null, val workoutLabel: String? = null)

// ───────── recordes por faixa de repetições ─────────
val RM_BANDS = listOf(1, 5, 10)
data class BandRecord(val exercise: String, val band: Int, val kg: Double, val at: Instant)

/** Maior carga feita com pelo menos [band] repetições (nRM) em cada exercício. */
fun bandRecords(sets: List<AnalysisSet>, bands: List<Int> = RM_BANDS): List<BandRecord> {
    val best = HashMap<Pair<String, Int>, BandRecord>()
    for (s in sets) {
        if (s.kg <= 0.0 || s.reps < 1) continue
        for (b in bands) {
            if (s.reps < b) continue
            val key = s.exercise to b
            val cur = best[key]
            if (cur == null || s.kg > cur.kg || (s.kg == cur.kg && s.at < cur.at)) best[key] = BandRecord(s.exercise, b, s.kg, s.at)
        }
    }
    return best.values.sortedWith(compareBy<BandRecord> { it.exercise.lowercase() }.thenBy { it.band })
}

// ───────── calorias ─────────
/** Estimativa grosseira: musculação ≈ 5 MET. kcal = MET × kg × horas. */
const val MET_WEIGHT_TRAINING = 5.0
fun estimateKcal(minutes: Double, bodyKg: Double): Int = (MET_WEIGHT_TRAINING * bodyKg * (maxOf(0.0, minutes) / 60.0)).roundToInt()

// ───────── desequilíbrio muscular ─────────
data class Imbalance(val label: String, val a: String, val b: String, val setsA: Int, val setsB: Int, val warn: Boolean)

private val PUSH = setOf(CatalogMuscle.PEITO, CatalogMuscle.OMBROS, CatalogMuscle.TRICEPS)
private val PULL = setOf(CatalogMuscle.COSTAS, CatalogMuscle.BICEPS)

/** Empurrar × puxar e quadríceps × posterior nos últimos [days] dias. Aviso quando um lado tem mais que o dobro do outro (e ≥ 8 séries). */
fun imbalances(sets: List<AnalysisSet>, now: Instant, days: Int = 28): List<Imbalance> {
    val from = now - days.days
    val by = HashMap<CatalogMuscle, Int>()
    for (s in sets) {
        if (s.reps <= 0 || s.at < from || s.at > now) continue
        ExerciseCatalog.find(s.exercise)?.muscle?.let { by[it] = (by[it] ?: 0) + 1 }
    }
    fun sum(ms: Set<CatalogMuscle>) = ms.sumOf { by[it] ?: 0 }
    fun mk(label: String, a: String, b: String, sa: Int, sb: Int): Imbalance {
        val hi = maxOf(sa, sb); val lo = minOf(sa, sb)
        return Imbalance(label, a, b, sa, sb, hi >= 8 && hi > lo * 2)
    }
    return listOf(
        mk("Empurrar × puxar", "Empurrar", "Puxar", sum(PUSH), sum(PULL)),
        mk("Quadríceps × posterior", "Quadríceps", "Posterior", by[CatalogMuscle.QUADRICEPS] ?: 0, by[CatalogMuscle.POSTERIOR] ?: 0)
    )
}

// ───────── platô ─────────
data class Plateau(val exercise: String, val weeks: Int, val bestOneRm: Double)

/** Exercícios cujo melhor 1RM estimado não sobe há [minWeeks] semanas, mas que foram treinados nas últimas 3 semanas. */
fun plateaus(sets: List<AnalysisSet>, now: Instant, minWeeks: Int = 4): List<Plateau> {
    val out = ArrayList<Plateau>()
    for ((exercise, list) in sets.filter { it.kg > 0 && it.reps >= 1 }.groupBy { it.exercise }) {
        var best = 0.0; var bestAt = list.first().at; var lastAt = list.first().at
        for (s in list) {
            val rm = estimateOneRepMax(s.kg, s.reps)
            if (rm > best + 1e-9) { best = rm; bestAt = s.at }
            if (s.at > lastAt) lastAt = s.at
        }
        val weeks = ((now - bestAt) / 7.days).toInt()
        if (weeks >= minWeeks && now - lastAt < 21.days) out.add(Plateau(exercise, weeks, best))
    }
    return out.sortedByDescending { it.weeks }
}

// ───────── pacote para a tela ─────────
data class Analysis(val imbalances: List<Imbalance>, val plateaus: List<Plateau>, val records: List<BandRecord>, val points: List<AnalysisSet> = emptyList()) {
    val isEmpty: Boolean get() = plateaus.isEmpty() && records.isEmpty() && imbalances.none { it.warn }
}

/** Monta as análises a partir das séries do histórico; [names] traduz exerciseId → nome. Séries sem data ou nome são ignoradas. */
fun buildAnalysis(sets: List<com.vitoroliveira.gymtracker.data.model.ExportSet>, names: Map<String, String>, now: Instant): Analysis {
    val points = sets.mapNotNull { s ->
        val name = names[s.exerciseId] ?: return@mapNotNull null
        val at = (s.completedAt ?: s.workouts?.startedAt)?.let { runCatching { Instant.parse(it) }.getOrNull() } ?: return@mapNotNull null
        AnalysisSet(name, s.reps, s.weightKg, at, s.workouts?.id, s.workouts?.muscleGroupName)
    }
    return Analysis(imbalances(points, now), plateaus(points, now), bandRecords(points), points)
}

// ───────── painel "Hoje" (Home): carga da semana e semana de descarga ─────────
private fun mondayOf(d: kotlinx.datetime.LocalDate): kotlinx.datetime.LocalDate =
    d.minus(d.dayOfWeek.ordinal, kotlinx.datetime.DateTimeUnit.DAY)

/** Volume (kg) por semana nas últimas [n] semanas (a atual por último); [points] = (dia, volume da série). */
fun weeklyVolumes(points: List<Pair<kotlinx.datetime.LocalDate, Double>>, today: kotlinx.datetime.LocalDate, n: Int = 12): List<Double> {
    val cur = mondayOf(today)
    val out = MutableList(n) { 0.0 }
    for ((day, kg) in points) {
        val weeksAgo = mondayOf(day).daysUntil(cur) / 7
        val i = n - 1 - weeksAgo
        if (mondayOf(day) <= cur && i in 0 until n) out[i] += kg
    }
    return out
}

/** Sugere descarga quando as 4 últimas semanas ficaram todas ≥ 5% acima da mediana das semanas com treino (mín. 6 semanas e 8 de histórico). */
fun needsDeload(volumes: List<Double>): Boolean {
    if (volumes.size < 8) return false
    val sorted = volumes.filter { it > 0 }.sorted()
    if (sorted.size < 6) return false
    val median = sorted[sorted.size / 2]
    return volumes.takeLast(4).all { it >= median * 1.05 }
}

// ───────── pesquisa no histórico ─────────
private const val ACCENTS = "áàâãäéèêëíìîïóòôõöúùûüç"
private const val PLAIN = "aaaaaeeeeiiiiooooouuuuc"
/** Minúsculas e sem acento do português (o common não tem normalizador). */
fun foldPt(s: String): String = buildString { for (c in s.lowercase()) { val i = ACCENTS.indexOf(c); append(if (i >= 0) PLAIN[i] else c) } }

/** Procura por nome de exercício (sem acento) ou carga ("80", "80kg"); mais recentes primeiro, até [limit]. */
fun searchSets(sets: List<AnalysisSet>, query: String, limit: Int = 40): List<AnalysisSet> {
    val q = foldPt(query.trim())
    if (q.isEmpty()) return emptyList()
    val kgQ = q.removeSuffix("kg").trim().replace(',', '.').toDoubleOrNull()
    return sets.filter { s -> if (kgQ != null) s.kg == kgQ else foldPt(s.exercise).contains(q) }
        .sortedByDescending { it.at }.take(limit)
}

// ───────── comparar dois treinos ─────────
data class WorkoutRef(val id: String, val at: Instant, val label: String?)
data class ExerciseDiff(val exercise: String, val a: Pair<Double, Int>?, val b: Pair<Double, Int>?)

/** Treinos distintos (mais recentes primeiro) para escolher na comparação. */
fun workoutRefs(sets: List<AnalysisSet>, limit: Int = 12): List<WorkoutRef> =
    sets.filter { it.workoutId != null }
        .groupBy { it.workoutId!! }
        .map { (id, ss) -> WorkoutRef(id, ss.minOf { it.at }, ss.firstNotNullOfOrNull { it.workoutLabel }) }
        .sortedByDescending { it.at }.take(limit)

/** Melhor série (maior 1RM estimado) de cada exercício nos dois treinos, em ordem alfabética. */
fun compareWorkouts(sets: List<AnalysisSet>, idA: String, idB: String): List<ExerciseDiff> {
    fun top(id: String): Map<String, Pair<Double, Int>> =
        sets.filter { it.workoutId == id }.groupBy { it.exercise }
            .mapValues { (_, ss) -> ss.maxBy { estimateOneRepMax(it.kg, it.reps) }.let { it.kg to it.reps } }
    val a = top(idA); val b = top(idB)
    return (a.keys + b.keys).toSortedSet().map { ExerciseDiff(it, a[it], b[it]) }
}
