package com.vitoroliveira.gymtracker.presentation

import kotlin.test.Test
import kotlin.test.assertEquals

class ErrorMessagesTest {
    private val fb = "Erro genérico"

    @Test fun credenciaisInvalidas() =
        assertEquals("E-mail ou senha incorretos.", Exception("Invalid login credentials").toUserMessage(fb))

    @Test fun usuarioJaExiste() =
        assertEquals("Já existe uma conta com esse e-mail.", Exception("User already registered").toUserMessage(fb))

    @Test fun chaveDuplicada() =
        assertEquals(
            "Já existe um item com esse nome.",
            Exception("duplicate key value violates unique constraint \"uq_exercise_name_per_user\"").toUserMessage(fb)
        )

    @Test fun rls() =
        assertEquals(
            "Você não tem permissão para fazer isso.",
            Exception("new row violates row-level security policy for table \"workouts\"").toUserMessage(fb)
        )

    @Test fun semInternet() =
        assertEquals(
            "Sem conexão com a internet. Verifique sua rede e tente de novo.",
            Exception("Unable to resolve host \"abc.supabase.co\"").toUserMessage(fb)
        )

    @Test fun timeout() =
        assertEquals(
            "Sem conexão com a internet. Verifique sua rede e tente de novo.",
            Exception("Request timeout has expired").toUserMessage(fb)
        )

    @Test fun sessaoExpirada() =
        assertEquals("Sua sessão expirou. Entre novamente.", IllegalStateException("Usuário não autenticado").toUserMessage(fb))

    @Test fun desconhecidoUsaFallback() =
        assertEquals(fb, Exception("algo totalmente inesperado").toUserMessage(fb))

    @Test fun nuncaVazaTextoTecnico() {
        val msg = RuntimeException("java.lang.NullPointerException at Foo.kt:12").toUserMessage(fb)
        assertEquals(fb, msg)
    }

    @Test fun migracaoDeExcluirContaFaltando() =
        assertEquals(
            "A exclusão de conta ainda não está ativada no servidor (falta rodar a migração 016).",
            RuntimeException("Could not find the function public.delete_my_account without parameters in the schema cache").toUserMessage(fb)
        )

    @Test fun timeoutNaChamadaDeExcluirContaContinuaSendoErroDeRede() =
        assertEquals(
            "Sem conexão com a internet. Verifique sua rede e tente de novo.",
            RuntimeException("Request timeout has expired [url=https://x.supabase.co/rest/v1/rpc/delete_my_account, request_timeout=30000 ms]").toUserMessage(fb)
        )

    @Test fun migracaoDePersonalizacaoFaltando() =
        assertEquals(
            "A personalização de grupos e desafios ainda não está ativada no servidor (falta rodar a migração 017).",
            RuntimeException("Could not find the 'accent_hex' column of 'groups' in the schema cache").toUserMessage(fb)
        )

    @Test fun migracaoDaRanqueadaFaltando() =
        assertEquals(
            "A ranqueada ainda não está ativada no servidor (falta rodar a migração 018).",
            RuntimeException("Could not find the table 'public.ranked_players' in the schema cache").toUserMessage(fb)
        )

    @Test fun timeoutNaRanqueadaContinuaSendoErroDeRede() =
        assertEquals(
            "Sem conexão com a internet. Verifique sua rede e tente de novo.",
            RuntimeException("Request timeout has expired [url=https://x.supabase.co/rest/v1/rpc/ranked_board, request_timeout=30000 ms]").toUserMessage(fb)
        )

    @Test fun migracaoDoMuralFaltando() =
        assertEquals(
            "O mural e o ranking do grupo ainda não estão ativados no servidor (falta rodar a migração 019).",
            RuntimeException("Could not find the table 'public.group_messages' in the schema cache").toUserMessage(fb)
        )
}
