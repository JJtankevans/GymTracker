package com.vitoroliveira.gymtracker.data.repository

import com.vitoroliveira.gymtracker.presentation.body.BodyCloud
import com.vitoroliveira.gymtracker.presentation.body.BodyProfile
import com.vitoroliveira.gymtracker.presentation.body.BodyWeightEntry
import com.vitoroliveira.gymtracker.presentation.body.Sex
import com.vitoroliveira.gymtracker.presentation.body.roundWeight
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.datetime.LocalDate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
private data class WeightRow(
    @SerialName("measured_on") val measuredOn: String,
    @SerialName("weight_kg") val weightKg: Double
)

@Serializable
private data class NewWeightRow(
    @SerialName("profile_id") val profileId: String,
    @SerialName("measured_on") val measuredOn: String,
    @SerialName("weight_kg") val weightKg: Double
)

@Serializable
private data class BodyProfileRow(
    @SerialName("height_cm") val heightCm: Int? = null,
    @SerialName("birth_year") val birthYear: Int? = null,
    val sex: String? = null,
    @SerialName("ask_weight_each_workout") val askWeightEachWorkout: Boolean = false
)

/** Dados corporais no Supabase: tabela body_weights e colunas do perfil (migração 003; RLS: só o dono). */
class BodyCloudImpl(private val supabase: SupabaseClient) : BodyCloud {

    private fun userId(): String = supabase.auth.currentUserOrNull()?.id ?: throw IllegalStateException("Usuário não autenticado")

    private fun dateOf(epochDay: Int) = LocalDate.fromEpochDays(epochDay).toString()

    override suspend fun fetchWeights(): List<BodyWeightEntry> =
        supabase.postgrest["body_weights"]
            .select(Columns.list("measured_on", "weight_kg"))
            .decodeList<WeightRow>()
            .mapNotNull { r ->
                runCatching { BodyWeightEntry(LocalDate.parse(r.measuredOn).toEpochDays().toInt(), r.weightKg) }.getOrNull()
            }

    override suspend fun upsertWeight(entry: BodyWeightEntry) {
        val me = userId()
        val day = dateOf(entry.epochDay)
        // um por dia (unique profile_id + measured_on): upsert numa chamada só (antes era apaga + insere, e uma
        // falha no meio deixava o dia sem peso na nuvem)
        supabase.postgrest["body_weights"].upsert(NewWeightRow(me, day, roundWeight(entry.kg))) {
            onConflict = "profile_id,measured_on"
        }
    }

    override suspend fun deleteWeight(epochDay: Int) {
        supabase.postgrest["body_weights"].delete { filter { eq("measured_on", dateOf(epochDay)) } }
    }

    override suspend fun fetchProfile(): BodyProfile? {
        val row = supabase.postgrest["profiles"]
            .select(Columns.list("height_cm", "birth_year", "sex", "ask_weight_each_workout")) { filter { eq("id", userId()) } }
            .decodeSingleOrNull<BodyProfileRow>() ?: return null
        return BodyProfile(
            heightCm = row.heightCm,
            birthYear = row.birthYear,
            sex = row.sex?.let { s -> Sex.entries.firstOrNull { it.name == s } },
            askWeightEachWorkout = row.askWeightEachWorkout
        )
    }

    override suspend fun saveProfile(profile: BodyProfile) {
        // respeita os CHECKs do banco: fora da faixa vai como vazio em vez de falhar a sincronização inteira
        val height = profile.heightCm?.takeIf { it in 100..250 }
        val birth = profile.birthYear?.takeIf { it in 1900..2100 }
        supabase.postgrest["profiles"].update({
            set<Int?>("height_cm", height)
            set<Int?>("birth_year", birth)
            set<String?>("sex", profile.sex?.name)
            set("ask_weight_each_workout", profile.askWeightEachWorkout)
        }) { filter { eq("id", userId()) } }
    }
}
