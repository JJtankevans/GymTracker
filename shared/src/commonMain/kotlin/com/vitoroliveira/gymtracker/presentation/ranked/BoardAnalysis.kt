package com.vitoroliveira.gymtracker.presentation.ranked

import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber

/**
 * Onde eu estou na turma da semana e o que falta para mudar de faixa. [points] são os pontos de todos (inclusive o meu,
 * [myPoints]); a posição é a de [positions] (empate divide). Segue as mesmas faixas de [zoneFor].
 */
data class BoardAnalysis(
    val size: Int,
    val position: Int,
    val myPoints: Int,
    val zone: Zone,
    val promoteCount: Int,
    val demoteCount: Int,
    /** RP que eu ganharia/perderia se a semana fechasse agora. */
    val projectedDelta: Int,
    /** Pontos que faltam para entrar na faixa de subida; null se já estou nela ou se a turma é pequena demais. */
    val pointsToPromotion: Int?,
    /** Pontos que faltam para sair da faixa de descida; null se não estou nela. */
    val pointsToSafety: Int?
)

fun analyzeBoard(points: List<Int>, myPoints: Int): BoardAnalysis {
    val size = points.size
    val position = 1 + points.count { it > myPoints }
    val promote = promoteCount(size)
    val demote = demoteCount(size)
    val zone = zoneFor(position, size, myPoints)
    // os outros, do maior para o menor (tiro uma ocorrência dos meus pontos)
    val others = points.toMutableList().also { it.remove(myPoints) }.sortedDescending()

    // para subir: igualar quem está na última vaga de subida (empate divide a posição). Sem treino, o mínimo é um treino válido.
    val toPromotion: Int? = if (promote == 0 || zone == Zone.PROMOTE) null else {
        others.getOrNull(promote - 1)?.let { threshold -> (threshold - myPoints).coerceAtLeast(if (myPoints <= 0) BASE_POINTS else 1) }
    }

    // para sair da descida: igualar quem está na última posição que não desce; quem fez 0 precisa de um treino válido
    val toSafety: Int? = if (zone != Zone.DEMOTE) null else {
        val safeSpot = size - demote
        val byPosition = if (size >= MIN_COHORT_FOR_PERCENTILES && safeSpot >= 1) (others.getOrNull(safeSpot - 1) ?: 0) - myPoints else 0
        if (myPoints <= 0) maxOf(BASE_POINTS, byPosition) else byPosition.coerceAtLeast(1)
    }
    return BoardAnalysis(size, position, myPoints, zone, promote, demote, settleDelta(position, size, myPoints), toPromotion, toSafety)
}

/** Dias que faltam para a semana fechar, contando hoje: segunda = 7, domingo = 1. */
fun weekDaysLeft(today: LocalDate): Int = 8 - today.dayOfWeek.isoDayNumber
