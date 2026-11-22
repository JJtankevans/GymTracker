package com.vitoroliveira.gymtracker.ui.components

private val smallWords = setOf("e", "de", "do", "da", "dos", "das", "na", "no", "com", "para", "a", "o")

/**
 * Normaliza o nome de um grupo muscular só para exibição: "QUADRICEPS E PANTURRILHA" e
 * "glúteos e posterior" viram "Quadriceps e Panturrilha" e "Glúteos e Posterior".
 * Siglas curtas (até 3 letras, ex.: "ABS", "HIIT" não) ficam como estão.
 */
fun prettyGroupName(raw: String): String {
    val words = raw.trim().split(" ").filter { it.isNotEmpty() }
    return words.mapIndexed { i, w ->
        val lower = w.lowercase()
        when {
            i > 0 && lower in smallWords -> lower
            w.length <= 3 && w == w.uppercase() && w.any { it.isLetter() } -> w
            else -> lower.replaceFirstChar { it.uppercase() }
        }
    }.joinToString(" ")
}
