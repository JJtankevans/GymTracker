package com.vitoroliveira.gymtracker.presentation.workout

/**
 * Supersérie / bi-set (#575) e cadência com tempo sob tensão (#581): porte de `web/src/superset.ts` e `web/src/tempo.ts`.
 * Puras (sem rede nem preferências) para testar no :shared.
 */

// ───────── supersérie ─────────
/** Ids dos exercícios ligados ao PRÓXIMO da lista. Exercícios consecutivos ligados formam um grupo. */
typealias Links = Set<String>

/** Grupos de exercícios em ordem; os não ligados ficam em grupos de 1. */
fun supersetGroups(ids: List<String>, links: Links): List<List<String>> {
    val groups = ArrayList<MutableList<String>>()
    ids.forEachIndexed { i, id ->
        if (i > 0 && ids[i - 1] in links) groups.last().add(id) else groups.add(mutableListOf(id))
    }
    return groups
}

fun groupOf(groups: List<List<String>>, id: String): List<String> = groups.firstOrNull { id in it } ?: listOf(id)

/** 2 exercícios = Bi-set, 3 = Tri-set, 4+ = Circuito; 1 = nenhum. */
fun groupLabel(size: Int): String? = when {
    size < 2 -> null
    size == 2 -> "Bi-set"
    size == 3 -> "Tri-set"
    else -> "Circuito"
}

/**
 * Depois de registrar uma série de [id], quem vem agora? Em grupo, o próximo exercício da rodada
 * (null se foi o último do grupo: hora de descansar). Fora de grupo, sempre null.
 */
fun nextInRound(groups: List<List<String>>, id: String): String? {
    val g = groupOf(groups, id)
    val i = g.indexOf(id)
    return if (g.size > 1 && i < g.size - 1) g[i + 1] else null
}

/** Liga/desliga o exercício [id] ao próximo. O último da lista não pode ligar a nada. */
fun toggleLink(ids: List<String>, links: Links, id: String): Links {
    val i = ids.indexOf(id)
    if (i < 0 || i == ids.size - 1) return links
    return if (id in links) links - id else links + id
}

// ───────── cadência e TUT ─────────
/** Cadência "3-1-2-0": descida, pausa embaixo, subida, pausa em cima (segundos, um dígito 0–9 cada). */
fun parseTempo(text: String): List<Int>? {
    val t = text.trim()
    if (t.isEmpty()) return null
    val parts = if (t.length == 4 && t.all { it.isDigit() }) t.map { it.toString() } else t.split(' ', '-', 'x', '.').filter { it.isNotEmpty() }
    if (parts.size != 4 || !parts.all { it.length == 1 && it[0].isDigit() }) return null
    return parts.map { it.toInt() }
}

fun tempoText(t: List<Int>): String = t.joinToString("-")

/** Segundos sob tensão da série: repetições × soma da cadência. */
fun tutSeconds(reps: Int, tempo: List<Int>): Int = maxOf(0, reps) * tempo.sum()

fun formatTut(seconds: Int): String =
    if (seconds >= 60) "${seconds / 60}min${(seconds % 60).toString().padStart(2, '0')}" else "${seconds}s"
