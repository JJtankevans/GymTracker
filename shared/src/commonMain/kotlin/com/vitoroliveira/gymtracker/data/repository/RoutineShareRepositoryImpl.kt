package com.vitoroliveira.gymtracker.data.repository

import com.vitoroliveira.gymtracker.data.model.NewSharedRoutine
import com.vitoroliveira.gymtracker.data.model.SharedRoutine
import com.vitoroliveira.gymtracker.domain.repository.RoutineShareRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@Serializable
private data class CodeRow(val code: String)

class RoutineShareRepositoryImpl(private val supabase: SupabaseClient) : RoutineShareRepository {

    override suspend fun share(routine: NewSharedRoutine): String =
        supabase.postgrest["shared_routines"]
            .insert(routine) { select(Columns.list("code")) }
            .decodeSingle<CodeRow>()
            .code

    override suspend fun find(code: String, countImport: Boolean): SharedRoutine? =
        supabase.postgrest.rpc(
            "get_shared_routine",
            buildJsonObject {
                put("p_code", code)
                put("p_count_import", countImport)
            }
        ).decodeList<SharedRoutine>().firstOrNull()
}
