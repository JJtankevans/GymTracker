package com.vitoroliveira.gymtracker.presentation.body

import com.vitoroliveira.gymtracker.data.catalog.CatalogMuscle
import com.vitoroliveira.gymtracker.data.catalog.ExerciseCatalog
import com.vitoroliveira.gymtracker.data.model.RecentSet

/** Séries de um músculo na janela (ex.: últimos 7 dias). [muscle] null = exercícios fora do catálogo ("Outros"). */
data class MuscleSets(val muscle: CatalogMuscle?, val label: String, val sets: Int)

/** Faixa comum de séries semanais por músculo para hipertrofia (Schoenfeld et al., 2017: mais séries, até ~20, mais ganho). */
val WEEKLY_SETS_RANGE = 10..20

/**
 * Conta as séries (com pelo menos 1 repetição) por músculo, achando o músculo pelo nome do exercício no catálogo
 * (nome ou apelido). Todos os músculos do catálogo aparecem, mesmo com 0, para ver o que ficou de fora;
 * "Outros" só aparece se houver séries de exercícios fora do catálogo. Mais séries primeiro.
 */
fun weeklySetsByMuscle(sets: List<RecentSet>, exerciseNames: Map<String, String>): List<MuscleSets> {
    val counts = sets.filter { it.reps > 0 }
        .groupingBy { s -> exerciseNames[s.exerciseId]?.let { ExerciseCatalog.find(it)?.muscle } }
        .eachCount()
    val muscles = CatalogMuscle.entries.map { m -> MuscleSets(m, m.label, counts[m] ?: 0) }
    val others = counts[null]?.let { listOf(MuscleSets(null, "Outros", it)) }.orEmpty()
    return (muscles + others).sortedWith(compareByDescending<MuscleSets> { it.sets }.thenBy { it.label })
}
