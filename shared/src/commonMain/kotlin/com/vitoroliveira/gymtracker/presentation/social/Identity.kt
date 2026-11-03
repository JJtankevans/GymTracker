package com.vitoroliveira.gymtracker.presentation.social

/** Capa, cor e ícone de um grupo ou desafio. Tudo é opcional e guardado como texto; [Identity.clean] valida. */
data class Look(val cover: String? = null, val accentHex: String? = null, val icon: String? = null)

object Identity {
    /**
     * Capas: ids de tema. O app desenha como capa o fundo animado do tema (Radar, Sakura, Vulcão...), então
     * não há imagem para baixar nem para enviar. Ids que não existem mais no app caem em "sem capa".
     */
    val covers = listOf(
        "aurora", "radar", "vulcao", "sakura", "galaxia", "neon", "floresta", "oceano", "ouro", "arena",
        "masmorra", "placa_mae", "cidade_neon", "abissal", "tempestade", "miragem", "nevasca", "sunset", "carbon", "origami"
    )

    /** Cores de destaque (#RRGGBB). */
    val accents = listOf(
        "#C6F432", "#38BDF8", "#FF4D8D", "#FFB000", "#7B4DFF",
        "#2DD4BF", "#FF3B30", "#F5F5F5", "#FF8A3D", "#4ADE80"
    )

    /** Ícones (ids que o app mapeia para desenhos; sem emojis). */
    val icons = listOf(
        "fitness", "bolt", "fire", "trophy", "shield", "rocket", "star", "heart", "diamond", "run", "mountain", "paw"
    )

    private val hex = Regex("^#[0-9A-F]{6}$")

    /** Descarta o que não é válido (capa/ícone desconhecidos, cor fora de #RRGGBB); a cor volta em maiúsculas. */
    fun clean(look: Look): Look = Look(
        cover = look.cover?.trim()?.takeIf { it in covers },
        accentHex = look.accentHex?.trim()?.uppercase()?.takeIf { hex.matches(it) },
        icon = look.icon?.trim()?.takeIf { it in icons }
    )
}

/** Resumo da semana de um grupo (função `group_week_progress`, migração 017): treinos dos membros de segunda até agora. */
@kotlinx.serialization.Serializable
data class GroupWeekProgress(
    val workouts: Int = 0,
    val members: Int = 0,
    @kotlinx.serialization.SerialName("active_members") val activeMembers: Int = 0
) {
    /** Quanto da meta semanal do grupo foi batido (0 a 1, passa de 1 quando supera); null sem meta. */
    fun fraction(goal: Int?): Float? = goal?.takeIf { it > 0 }?.let { workouts.toFloat() / it }

    /** "12 de 20 treinos · 5 de 8 membros ativos", ou sem a meta: "12 treinos · 5 de 8 membros ativos". */
    fun summary(goal: Int?): String {
        val g = goal?.takeIf { it > 0 }
        val w = if (g != null) "$workouts de $g treinos" else if (workouts == 1) "1 treino" else "$workouts treinos"
        return "$w · $activeMembers de $members ${if (members == 1) "membro ativo" else "membros ativos"}"
    }
}
