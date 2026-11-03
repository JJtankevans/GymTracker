package com.vitoroliveira.gymtracker.presentation.body

import com.vitoroliveira.gymtracker.data.catalog.CatalogMuscle

/** Os quatro blocos de movimento usados para olhar o equilíbrio do treino. Cardio e mobilidade ficam de fora. */
enum class MovementGroup(val label: String, val muscles: Set<CatalogMuscle>) {
    PUSH("Empurrar", setOf(CatalogMuscle.PEITO, CatalogMuscle.OMBROS, CatalogMuscle.TRICEPS)),
    PULL("Puxar", setOf(CatalogMuscle.COSTAS, CatalogMuscle.BICEPS)),
    LEGS("Pernas", setOf(CatalogMuscle.QUADRICEPS, CatalogMuscle.POSTERIOR, CatalogMuscle.GLUTEOS, CatalogMuscle.PANTURRILHA)),
    CORE("Core", setOf(CatalogMuscle.ABDOMEN))
}

/** Séries de um bloco e a fatia dele no total (0 a 1). */
data class BalanceRow(val group: MovementGroup, val sets: Int, val share: Float)

/** [hint] é a observação principal (null quando o equilíbrio está bom); [balanced] diz se não há nada a ajustar. */
data class MuscleBalance(val rows: List<BalanceRow>, val total: Int, val hint: String?, val balanced: Boolean)

/** Menos séries que isso na janela e a comparação não diz nada. */
const val MIN_SETS_FOR_BALANCE = 8

// Contas em inteiros (sem ponto flutuante) para o limite exato não depender de arredondamento.
/** Puxar abaixo de 70% de empurrar (ou o contrário) conta como desequilíbrio. */
private fun lopsided(small: Int, big: Int) = big > 0 && small * 10 < big * 7

/** Pernas abaixo de 20% das séries de força (sem core) contam como atrasadas. */
private fun legsBehind(legs: Int, strength: Int) = legs * 5 < strength

/**
 * Agrupa as séries por bloco de movimento e aponta o desequilíbrio mais relevante: empurrar x puxar (ombros e postura)
 * ou pernas esquecidas. Null quando há poucas séries para comparar. É uma referência, não um diagnóstico.
 */
fun muscleBalance(weekly: List<MuscleSets>): MuscleBalance? {
    val bySets = MovementGroup.entries.associateWith { g -> weekly.filter { it.muscle in g.muscles }.sumOf { it.sets } }
    val total = bySets.values.sum()
    if (total < MIN_SETS_FOR_BALANCE) return null
    val rows = MovementGroup.entries.map { g -> BalanceRow(g, bySets.getValue(g), bySets.getValue(g).toFloat() / total) }
    val push = bySets.getValue(MovementGroup.PUSH)
    val pull = bySets.getValue(MovementGroup.PULL)
    val legs = bySets.getValue(MovementGroup.LEGS)
    val strength = push + pull + legs
    val hint = when {
        lopsided(pull, push) ->
            "Você empurrou bem mais do que puxou ($push x $pull séries). Remadas e puxadas ajudam o ombro e a postura."
        lopsided(push, pull) ->
            "Você puxou bem mais do que empurrou ($pull x $push séries). Um pouco mais de supino ou desenvolvimento equilibra."
        strength >= MIN_SETS_FOR_BALANCE && legsBehind(legs, strength) ->
            "Pernas ficaram com ${(legs * 100f / strength).toInt()}% das séries. Elas são os maiores músculos do corpo: vale dar mais atenção."
        else -> null
    }
    return MuscleBalance(rows, total, hint, balanced = hint == null)
}
