package com.vitoroliveira.gymtracker.data.repository

import com.vitoroliveira.gymtracker.data.model.RankedBoardRow
import com.vitoroliveira.gymtracker.data.model.RankedDuel
import com.vitoroliveira.gymtracker.data.model.RankedLastResult
import com.vitoroliveira.gymtracker.data.model.RankedPlayer
import com.vitoroliveira.gymtracker.domain.repository.RankedRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class RankedRepositoryImpl(private val supabase: SupabaseClient) : RankedRepository {

    private fun me(): String = supabase.auth.currentUserOrNull()?.id
        ?: throw IllegalStateException("Usuário não autenticado")

    override fun currentUserId(): String = me()

    override suspend fun player(): RankedPlayer? =
        supabase.postgrest["ranked_players"].select { filter { eq("profile_id", me()) } }.decodeList<RankedPlayer>().firstOrNull()

    override suspend fun join(): Int =
        supabase.postgrest.rpc("ranked_join").decodeAs<Int>()

    override suspend fun board(): List<RankedBoardRow> =
        supabase.postgrest.rpc("ranked_board").decodeList<RankedBoardRow>()

    override suspend fun lastResult(): RankedLastResult? =
        supabase.postgrest.rpc("ranked_last_result").decodeList<RankedLastResult>().firstOrNull()

    override suspend fun settleWeek(weekStart: String): Int =
        supabase.postgrest.rpc("ranked_settle_week", buildJsonObject { put("p_week", weekStart) }).decodeAs<Int>()

    override suspend fun settleDuels(): Int =
        supabase.postgrest.rpc("ranked_settle_duels").decodeAs<Int>()

    override suspend fun duels(): List<RankedDuel> =
        supabase.postgrest.rpc("ranked_my_duels").decodeList<RankedDuel>()

    override suspend fun createDuel(opponentId: String, ranked: Boolean): String =
        supabase.postgrest.rpc("ranked_duel_create", buildJsonObject {
            put("p_opponent", opponentId)
            put("p_ranked", JsonPrimitive(ranked))
        }).decodeAs<String>()

    override suspend fun respondDuel(duelId: String, accept: Boolean) {
        supabase.postgrest.rpc("ranked_duel_respond", buildJsonObject {
            put("p_duel", duelId)
            put("p_accept", JsonPrimitive(accept))
        })
    }
}
