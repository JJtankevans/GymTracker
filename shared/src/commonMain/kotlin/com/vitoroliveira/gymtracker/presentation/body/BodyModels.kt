package com.vitoroliveira.gymtracker.presentation.body

import kotlinx.serialization.Serializable

@Serializable
enum class Sex(val label: String) { MALE("Masculino"), FEMALE("Feminino") }

/** Dados opcionais do usuário. Tudo é opcional: sem eles o app só não personaliza as sugestões. */
@Serializable
data class BodyProfile(
    val heightCm: Int? = null,
    val birthYear: Int? = null,
    val sex: Sex? = null,
    /** Se true, o app pede o peso corporal ao iniciar cada treino. */
    val askWeightEachWorkout: Boolean = false
) {
    fun ageAt(year: Int): Int? = birthYear?.let { (year - it).takeIf { a -> a in 10..110 } }
}

/** Um registro de peso corporal. [epochDay] = dias desde 1970-01-01. */
@Serializable
data class BodyWeightEntry(val epochDay: Int, val kg: Double)

/** Onde a fita métrica foi passada. */
@Serializable
enum class MeasureSite(val label: String) {
    WAIST("Cintura"), HIPS("Quadril"), CHEST("Peito"), ARM("Braço"), THIGH("Coxa"), CALF("Panturrilha")
}

/** Uma medida em cm num dia. Um registro por dia e local. */
@Serializable
data class BodyMeasurement(val epochDay: Int, val site: MeasureSite, val cm: Double)

/** Resumo de um local: valor mais recente e quanto mudou desde a primeira medida. */
data class MeasureSummary(val site: MeasureSite, val latestCm: Double, val latestDay: Int, val firstDay: Int, val changeCm: Double)

/** Só os locais com pelo menos uma medida, na ordem de [MeasureSite]. */
fun summarizeMeasurements(list: List<BodyMeasurement>): List<MeasureSummary> =
    MeasureSite.entries.mapNotNull { site ->
        val ms = list.filter { it.site == site }.sortedBy { it.epochDay }
        if (ms.isEmpty()) null
        else MeasureSummary(site, ms.last().cm, ms.last().epochDay, ms.first().epochDay, roundWeight(ms.last().cm - ms.first().cm))
    }

@Serializable
data class BodyData(
    val profile: BodyProfile = BodyProfile(),
    val weights: List<BodyWeightEntry> = emptyList(),
    /** Dias apagados aqui que ainda não foram apagados na nuvem (sem isso a sincronização "ressuscitaria" o registro). */
    val pendingDeletes: Set<Int> = emptySet(),
    /** Medidas com fita (só no aparelho por enquanto). */
    val measurements: List<BodyMeasurement> = emptyList()
) {
    val latestKg: Double? get() = weights.maxByOrNull { it.epochDay }?.kg
}

/** Cópia dos dados corporais no Supabase (tabela body_weights e colunas de profiles, migração 003). */
interface BodyCloud {
    suspend fun fetchWeights(): List<BodyWeightEntry>
    /** Um registro por dia: substitui o do mesmo dia. */
    suspend fun upsertWeight(entry: BodyWeightEntry)
    suspend fun deleteWeight(epochDay: Int)
    suspend fun fetchProfile(): BodyProfile?
    suspend fun saveProfile(profile: BodyProfile)
}

/** Peso como o banco guarda (numeric(5,1)): uma casa decimal. */
fun roundWeight(kg: Double): Double = kotlin.math.round(kg * 10) / 10.0

/**
 * Junta os pesos do aparelho com os da nuvem: o aparelho vence quando os dois têm o mesmo dia; os dias só da nuvem
 * (outro celular, reinstalação) entram; os apagados aqui e ainda pendentes não voltam.
 */
fun mergeWeights(local: List<BodyWeightEntry>, remote: List<BodyWeightEntry>, deleted: Set<Int>): List<BodyWeightEntry> {
    val localDays = local.map { it.epochDay }.toSet()
    return (local + remote.filter { it.epochDay !in localDays && it.epochDay !in deleted }).sortedBy { it.epochDay }
}

interface BodyStore {
    fun load(): BodyData
    fun save(data: BodyData)
}

class InMemoryBodyStore(private var data: BodyData = BodyData()) : BodyStore {
    override fun load() = data
    override fun save(data: BodyData) { this.data = data }
}

enum class BmiCategory(val label: String) {
    UNDER("Abaixo do peso"), NORMAL("Peso normal"), OVER("Sobrepeso"), OBESE("Obesidade")
}

object Bmi {
    /** Faixas da OMS para adultos. Não distingue massa muscular de gordura: é só uma referência. */
    fun value(kg: Double, heightCm: Int): Double? =
        if (kg <= 0 || heightCm < 100) null else kg / ((heightCm / 100.0) * (heightCm / 100.0))

    fun category(bmi: Double) = when {
        bmi < 18.5 -> BmiCategory.UNDER
        bmi < 25.0 -> BmiCategory.NORMAL
        bmi < 30.0 -> BmiCategory.OVER
        else -> BmiCategory.OBESE
    }

    /** Faixa de peso "normal" (IMC 18,5–24,9) para a altura. */
    fun normalRangeKg(heightCm: Int): ClosedFloatingPointRange<Double>? =
        if (heightCm < 100) null else {
            val h2 = (heightCm / 100.0) * (heightCm / 100.0)
            18.5 * h2..24.9 * h2
        }
}
