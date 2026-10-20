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

@Serializable
data class BodyData(
    val profile: BodyProfile = BodyProfile(),
    val weights: List<BodyWeightEntry> = emptyList()
) {
    val latestKg: Double? get() = weights.maxByOrNull { it.epochDay }?.kg
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
