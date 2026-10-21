package com.vitoroliveira.gymtracker.presentation.social

import com.vitoroliveira.gymtracker.data.model.ProfileTitle

/** Texto dos títulos de desafio. O banco só diz o tipo e o nível; os nomes e as frases ficam aqui para ajustar fácil. */
object ProfileTitles {
    /** Escada de honra: x dias seguidos = Alpha, 2x = Chad, 3x = Giga Chad, 4x = Mr. Olimpia (máximo). */
    val honorNames = listOf("Alpha", "Chad", "Giga Chad", "Mr. Olimpia")
    const val MAX_LEVEL = 4

    fun isHonor(t: ProfileTitle) = t.kind == "honor"
    fun isMax(t: ProfileTitle) = isHonor(t) && t.level >= MAX_LEVEL

    /** Honra: o nome do nível. Falha: uma das frases do Thiago, fixa para o mesmo perfil e desafio (não muda a cada abertura). */
    fun text(t: ProfileTitle): String =
        if (isHonor(t)) honorNames[(t.level - 1).coerceIn(0, honorNames.lastIndex)] else failurePhrase(t)

    fun failurePhrase(t: ProfileTitle): String {
        val pool = ChallengeMessages.behind + ChallengeMessages.failed
        return pool[(t.profileId + "|" + (t.challengeId ?: "")).hashCode().mod(pool.size)]
    }

    /** Explica a escada para o criador do desafio, ex.: "Alpha 7 dias · Chad 14 · Giga Chad 21 · Mr. Olimpia 28". */
    fun ladder(streakGoal: Int): String =
        honorNames.mapIndexed { i, n -> if (i == 0) "$n $streakGoal dias" else "$n ${streakGoal * (i + 1)}" }.joinToString(" · ")
}
