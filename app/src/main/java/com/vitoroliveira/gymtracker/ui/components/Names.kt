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

/** "há 12 min", "há 2 h 15 min" ou "há 3 dias": quanto tempo faz que o treino aberto foi iniciado. */
fun timeAgoText(startedAt: String?): String {
    val start = runCatching { java.time.OffsetDateTime.parse(startedAt).toInstant() }.getOrNull()
        ?: return "anteriormente"
    val minutes = java.time.Duration.between(start, java.time.Instant.now()).toMinutes().coerceAtLeast(0)
    return when {
        minutes < 1 -> "agora há pouco"
        minutes < 60 -> "há $minutes min"
        minutes < 24 * 60 -> "há ${minutes / 60} h" + (if (minutes % 60 > 0) " ${minutes % 60} min" else "")
        else -> "há ${minutes / (24 * 60)} dia(s)"
    }
}

