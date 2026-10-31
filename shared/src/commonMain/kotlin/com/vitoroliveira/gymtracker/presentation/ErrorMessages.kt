package com.vitoroliveira.gymtracker.presentation

import kotlin.coroutines.cancellation.CancellationException

/**
 * Converte qualquer erro técnico (rede, Postgrest, Auth) numa mensagem
 * curta em português para o usuário. Nunca devolve stack trace nem texto
 * cru de exceção. [fallback] é usado quando o erro não é reconhecido.
 */
fun Throwable.toUserMessage(fallback: String): String {
    if (this is CancellationException) return fallback
    val text = buildString {
        append(this@toUserMessage::class.simpleName.orEmpty()).append(' ')
        append(message.orEmpty()).append(' ')
        append(cause?.message.orEmpty())
    }.lowercase()

    return when {
        // Auth
        "invalid login credentials" in text || "invalid_credentials" in text ->
            "E-mail ou senha incorretos."
        "user already registered" in text || "already been registered" in text || "user_already_exists" in text ->
            "Já existe uma conta com esse e-mail."
        "email not confirmed" in text || "email_not_confirmed" in text ->
            "Confirme seu e-mail antes de entrar."
        "password should be at least" in text || "weak_password" in text ->
            "A senha é muito curta. Use pelo menos 6 caracteres."
        "unable to validate email" in text || "invalid format" in text && "email" in text ->
            "E-mail inválido."
        "over_email_send_rate_limit" in text || "rate limit" in text || "too many requests" in text ->
            "Muitas tentativas. Aguarde um pouco e tente de novo."
        "jwt expired" in text || "refresh_token" in text || "not authenticated" in text ||
            "não autenticado" in text -> "Sua sessão expirou. Entre novamente."

        // Função do servidor que ainda não foi criada (migração não rodada)
        ("could not find the function" in text || "pgrst202" in text) && "delete_my_account" in text ->
            "A exclusão de conta ainda não está ativada no servidor (falta rodar a migração 016)."

        // Coluna de uma migração que ainda não rodou (PostgREST: "Could not find the 'cover' column of 'groups'...")
        "could not find the" in text && "column" in text && listOf("cover", "accent_hex", "icon", "pinned_message", "weekly_goal", "prize").any { "'$it'" in text } ->
            "A personalização de grupos e desafios ainda não está ativada no servidor (falta rodar a migração 017)."

        // Ranqueada: tabelas/funções da migração 018 que ainda não existem
        "ranked_" in text && ("could not find" in text || "does not exist" in text || "schema cache" in text) ->
            "A ranqueada ainda não está ativada no servidor (falta rodar a migração 018)."

        // Banco
        "duplicate key" in text || "23505" in text || "already exists" in text ->
            "Já existe um item com esse nome."
        "violates foreign key" in text || "23503" in text ->
            "Não dá pra concluir: este item está sendo usado em outro lugar."
        "violates check constraint" in text || "23514" in text ->
            "Algum valor informado é inválido."
        "row-level security" in text || "permission denied" in text || "42501" in text ->
            "Você não tem permissão para fazer isso."

        // Rede
        "unresolvedaddress" in text || "unknownhost" in text || "unable to resolve host" in text ||
            "no address associated" in text || "network is unreachable" in text ||
            "failed to connect" in text || "connectexception" in text || "sockettimeout" in text ||
            "timeout" in text || "timed out" in text || "connection reset" in text ||
            "software caused connection abort" in text || "ioexception" in text ->
            "Sem conexão com a internet. Verifique sua rede e tente de novo."

        else -> fallback
    }
}
