package com.vitoroliveira.gymtracker.presentation.records

import com.vitoroliveira.gymtracker.data.model.ExerciseProgression
import com.vitoroliveira.gymtracker.data.model.RecordSummary
import com.vitoroliveira.gymtracker.presentation.body.estimateOneRepMax
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil

/** Recorde de carga de um exercício, calculado a partir da view de progressão (1 linha por dia de treino). */
data class RecordRow(
    val exerciseId: String,
    val exerciseName: String,
    val bestWeightKg: Double,
    /** Dia (yyyy-MM-dd) em que a carga recorde foi batida pela primeira vez. */
    val bestDate: String,
    /** Melhor 1RM estimado (Epley) entre as sessões. É uma estimativa. */
    val bestOneRmKg: Double,
    /** Recorde anterior ao atual; null se o recorde veio na primeira sessão. */
    val previousBestKg: Double?,
    val sessions: Int
) {
    /** Quanto o recorde subiu em relação ao anterior. */
    val improvementKg: Double? get() = previousBestKg?.let { bestWeightKg - it }

    /** Bateu o recorde nos últimos [days] dias? */
    fun isRecent(today: LocalDate, days: Int = 30): Boolean {
        val d = runCatching { LocalDate.parse(bestDate) }.getOrNull() ?: return false
        val ago = d.daysUntil(today)
        return ago in 0..days
    }
}

/**
 * Monta o recorde de um exercício. Devolve null se não há carga registrada (ex.: exercícios só com peso do corpo
 * ficam com 0 kg e não entram na lista de recordes de carga).
 */
fun buildRecordRow(exerciseId: String, exerciseName: String, progression: List<ExerciseProgression>): RecordRow? {
    val sessions = progression.filter { it.maxWeightKg > 0 }.sortedBy { it.sessionDate }
    if (sessions.isEmpty()) return null

    val best = sessions.maxOf { it.maxWeightKg }
    // o recorde "nasce" na primeira sessão que chegou nessa carga
    val bestIndex = sessions.indexOfFirst { it.maxWeightKg == best }
    val previousBest = sessions.take(bestIndex).maxOfOrNull { it.maxWeightKg }

    val oneRm = sessions.maxOf {
        val avgReps = (it.totalReps.toDouble() / it.totalSets.coerceAtLeast(1)).toInt().coerceIn(1, 12)
        estimateOneRepMax(it.maxWeightKg, avgReps)
    }
    return RecordRow(exerciseId, exerciseName, best, sessions[bestIndex].sessionDate, oneRm, previousBest, sessions.size)
}

/** O servidor (`records_summary()`) já fez a mesma conta de [buildRecordRow]; aqui só muda o formato. */
fun RecordSummary.toRecordRow() =
    RecordRow(exerciseId, exerciseName, bestWeightKg, bestDate, bestOneRmKg, previousBestKg, sessions)

/** Mais recentes primeiro; empate resolvido pelo maior recorde. */
fun List<RecordRow>.sortedForDisplay(): List<RecordRow> =
    sortedWith(compareByDescending<RecordRow> { it.bestDate }.thenByDescending { it.bestWeightKg })
