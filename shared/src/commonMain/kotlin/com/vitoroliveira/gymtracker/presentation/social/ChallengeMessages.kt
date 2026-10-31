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

    // ───────────────────────── Segunda leva (31/10/2026): fatalismo biológico e mais zoeira ─────────────────────────
    // A primeira leva (acima) é do Thiago e não muda: testes e títulos de perfil dependem dela. Estas entram no sorteio.
    // Marcadas "DNA" as do tom "já estava determinado, é mera biologia"; as outras são zoeira comum no mesmo tom.

    /** Ficando para trás. DNA/destino primeiro, zoeira comum depois. */
    val behindMore = listOf(
        // DNA / destino
        "Não é culpa sua. É mera biologia.",
        "Estava escrito no seu DNA. Literalmente.",
        "O placar só confirmou o que a genética já sabia.",
        "Isso já estava determinado antes de você nascer.",
        "Seleção natural em ação. Estamos presenciando.",
        "Os astros, a genética e o placar concordam. Só você que não.",
        "Não foi o treino, foi o destino.",
        "O placar não mente: só confirma o que o DNA já avisou.",
        "Estava no código desde o primeiro bit de DNA.",
        "A biologia já tinha dado o spoiler.",
        "O universo só te colocou onde você já estava predestinado.",
        "Cientistas confirmam: nasceu pra acompanhar de longe.",
        // zoeira comum
        "Seu aquecimento já acabou ou ainda está carregando?",
        "O placar te chamou e você deixou no visto.",
        "Troca o \"amanhã eu treino\" por \"hoje eu treino\".",
        "O pessoal está treinando e você está... pesquisando treino?",
        "Aura em modo economia de energia.",
        "Falta pouco pra virar o jogo, só precisa levantar do sofá."
    )

    /** Último colocado / meta não batida. */
    val failedMore = listOf(
        // DNA / destino
        "Último lugar. Mera biologia: já estava determinado.",
        "Você não perdeu: só cumpriu o roteiro que a genética escreveu.",
        "O destino pesou e você ficou na bagagem.",
        "A natureza fez a escolha dela. O placar só publicou.",
        "Estudo com 1 pessoa conclui: era inevitável.",
        "Já estava tudo determinado. O treino foi só formalidade.",
        "Mera biologia: alguns nascem pra liderar, outros pra torcer.",
        "Darwin olhou o ranking e fez que sim com a cabeça.",
        "O DNA assinou a derrota antes do primeiro treino.",
        "Não precisa se culpar: foi o código genético, não você.",
        "Resultado previsto pelo horóscopo, pela genética e pela sua própria sombra.",
        "Isso não é derrota, é determinismo biológico.",
        // zoeira comum
        "Aura zerada. Reinicie o sistema e tente de novo.",
        "O sofá agradece a sua fidelidade.",
        "A meta pediu desculpa por você.",
        "Foi um bom descanso... de 30 dias.",
        "Chamem o suporte: a motivação não responde.",
        "Sentou no último lugar e ainda ajeitou a almofada."
    )

    /** Liderando. */
    val leadingMore = listOf(
        // DNA / destino
        "Mera biologia: já estava no seu DNA liderar.",
        "Nasceu pra isso. A genética só deixou o placar provar.",
        "Estava determinado desde o berço: você ia estar aqui.",
        "Darwin aprova. O topo é seu por seleção natural.",
        "A genética falou e o ranking obedeceu.",
        "Isso não é sorte, é predestinação.",
        // zoeira comum
        "Segura o trono, o povo está de olho.",
        "Aura nas alturas. Respeito é pouco.",
        "Primeiro lugar tem vista boa, né?",
        "Quem corre na frente leva a coroa (e os memes dos outros)."
    )

    /** Meta batida. */
    val goalReachedMore = listOf(
        // DNA / destino
        "Meta batida. Estava escrito nos seus genes.",
        "Mera biologia: quem nasceu pra bater meta, bate.",
        "O destino cobrou, o DNA pagou: meta no bolso.",
        "Cientistas já previam. Meta batida, sem surpresa.",
        // zoeira comum
        "Meta batida e ainda sobrou energia.",
        "Zero desculpa, 100% resultado.",
        "Missão cumprida. Amanhã tem mais.",
        "Treinou até o relógio pedir arrego."
    )

    /** Todas as frases do clima: as do Thiago primeiro, depois a segunda leva. */
    fun pool(mood: ChallengeMood): List<String> = when (mood) {
        ChallengeMood.BEHIND -> behind + behindMore
        ChallengeMood.FAILED -> failed + failedMore
        ChallengeMood.LEADING -> leading + leadingMore
        ChallengeMood.GOAL_REACHED -> goalReached + goalReachedMore
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
