package com.vitoroliveira.gymtracker.presentation.social

import com.vitoroliveira.gymtracker.data.model.Standing

/** Situação do usuário num desafio, para escolher qual mensagem aparece nos detalhes. */
enum class ChallengeMood {
    /** Alguém está na frente dele no placar. */
    BEHIND,
    /** Último colocado, ou desafio encerrado sem bater a meta. */
    FAILED,
    /** Liderando o placar (com pontuação). */
    LEADING,
    /** Já bateu a meta do desafio. */
    GOAL_REACHED
}

/** Layouts visuais das mensagens (desenhados em `ui/social/ChallengeMessageCard.kt`). */
enum class MessageLayout {
    /** Cartão de alerta com ícone. */
    ALERT,
    /** Faixa larga em destaque. */
    BANNER,
    /** Carimbo inclinado. */
    STAMP
}

data class ChallengeMessage(val text: String, val mood: ChallengeMood, val layout: MessageLayout)

object ChallengeMessages {
    /** Frases do Thiago para quem está ficando para trás. */
    val behind = listOf(
        "Você foi moggado!",
        "Faltou suor, sobrou desculpa.",
        "Faltou foco, sobrou mico.",
        "Ficar pra trás dói, né? Amanhã tem mais.",
        "Catabolizou só de olhar o gráfico dos seus amigos."
    )

    /** Frases do Thiago para o último colocado / meta não batida. */
    val failed = listOf(
        "-10000 de Aura. A meta te amassou.",
        "Sem aura, sem meta, sem respeito no ranking.",
        "Alpha de verdade bate a meta. Betinha arruma desculpa.",
        "Perdeu aura, levou mogg e virou o betinha do grupo."
    )

    /** Sugestões minhas no mesmo tom, para quem lidera. Troque à vontade. */
    val leading = listOf(
        "Aura em alta. O ranking te respeita.",
        "Mogou geral. Agora segura a liderança.",
        "O grupo está te olhando de baixo."
    )

    /** Sugestões minhas no mesmo tom, para quem bateu a meta. Troque à vontade. */
    val goalReached = listOf(
        "Meta batida. Alpha de verdade.",
        "Meta no bolso, aura no teto.",
        "Cumpriu a meta. Zero desculpa."
    )

    fun pool(mood: ChallengeMood): List<String> = when (mood) {
        ChallengeMood.BEHIND -> behind
        ChallengeMood.FAILED -> failed
        ChallengeMood.LEADING -> leading
        ChallengeMood.GOAL_REACHED -> goalReached
    }

    fun layoutFor(mood: ChallengeMood): MessageLayout = when (mood) {
        ChallengeMood.BEHIND -> MessageLayout.ALERT
        ChallengeMood.FAILED -> MessageLayout.STAMP
        ChallengeMood.LEADING, ChallengeMood.GOAL_REACHED -> MessageLayout.BANNER
    }

    /** Escolhe a frase pelo [seed] (ex.: desafio + usuário + dia): muda uma vez por dia e não pula a cada recomposição. */
    fun pick(mood: ChallengeMood, seed: Int): ChallengeMessage {
        val pool = pool(mood)
        return ChallengeMessage(pool[seed.mod(pool.size)], mood, layoutFor(mood))
    }
}

/**
 * Qual mensagem mostrar para [me] nesse desafio, ou null se não há motivo (desafio que ainda não começou, usuário fora
 * do placar, ninguém pontuou). Ordem: meta batida > meta perdida/último lugar > liderança > ficando para trás.
 */
fun challengeMood(status: ChallengeStatus, target: Double?, standings: List<Standing>?, me: String?): ChallengeMood? {
    if (status == ChallengeStatus.UPCOMING || standings.isNullOrEmpty() || me == null) return null
    val mine = standings.firstOrNull { it.profileId == me } ?: return null

    if (target != null && mine.score >= target) return ChallengeMood.GOAL_REACHED
    if (target != null && status == ChallengeStatus.ENDED) return ChallengeMood.FAILED

    val top = standings.maxOf { it.score }
    val lowest = standings.minOf { it.score }
    return when {
        standings.size < 2 || top <= 0.0 -> null              // sozinho ou ninguém treinou: nada a zoar nem a celebrar
        mine.score >= top -> ChallengeMood.LEADING            // empate no topo também é liderança
        mine.score <= lowest -> ChallengeMood.FAILED          // lanterna do placar
        else -> ChallengeMood.BEHIND
    }
}
