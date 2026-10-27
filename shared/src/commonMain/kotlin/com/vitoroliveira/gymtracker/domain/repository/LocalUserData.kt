package com.vitoroliveira.gymtracker.domain.repository

/**
 * Dados do usuário que ficam só no aparelho (peso e medidas, agenda da semana, favoritos do catálogo, ritmo do
 * circuito...). Não são separados por conta, então precisam ser apagados ao sair da conta: senão a próxima conta
 * que entrar no mesmo celular herda esses dados e a sincronização os envia para a nuvem dela.
 */
interface LocalUserData {
    /** Envia para a nuvem o que ainda só está no aparelho. Chamado antes de sair, com a sessão ainda válida. */
    suspend fun flush()

    /** Apaga tudo o que é do usuário no aparelho (o tema e outras preferências do aparelho ficam). */
    fun clear()
}
