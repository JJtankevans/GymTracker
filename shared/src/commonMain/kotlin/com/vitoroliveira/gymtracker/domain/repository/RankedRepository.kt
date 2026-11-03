package com.vitoroliveira.gymtracker.domain.repository

import com.vitoroliveira.gymtracker.data.model.RankedBoardRow
import com.vitoroliveira.gymtracker.data.model.RankedDuel
import com.vitoroliveira.gymtracker.data.model.RankedLastResult
import com.vitoroliveira.gymtracker.data.model.RankedPlayer

/** Ranqueada (migração 018). Os pontos e o RP são do banco; aqui só se pergunta e se pede. */
interface RankedRepository {
    fun currentUserId(): String
    /** Meu RP; null se ainda não entrei na ranqueada. Falha se a migração 018 não rodou. */
    suspend fun player(): RankedPlayer?
    /** Entra na ranqueada (Ferro III, 0 RP) e devolve o RP. */
    suspend fun join(): Int
    /** A turma da semana de quem chama, com os pontos de agora. */
    suspend fun board(): List<RankedBoardRow>
    suspend fun lastResult(): RankedLastResult?
    /** Fecha a semana que começou em [weekStart] (yyyy-MM-dd) se ainda não foi fechada; devolve quantos entraram (0 = já estava). */
    suspend fun settleWeek(weekStart: String): Int
    suspend fun settleDuels(): Int
    suspend fun duels(): List<RankedDuel>
    suspend fun createDuel(opponentId: String, ranked: Boolean): String
    suspend fun respondDuel(duelId: String, accept: Boolean)
}
