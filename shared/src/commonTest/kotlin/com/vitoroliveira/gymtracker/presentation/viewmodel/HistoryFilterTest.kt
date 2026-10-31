package com.vitoroliveira.gymtracker.presentation.viewmodel

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class HistoryFilterTest {
    private fun item(id: String, group: String) =
        WorkoutHistoryItem(id, group, "#FFFFFF", Instant.fromEpochSeconds(1_700_000_000), null, null)

    private val items = listOf(item("1", "Peito"), item("2", "Costas"), item("3", "Peito"), item("4", "Pernas"), item("5", "Costas"), item("6", "Peito"))

    @Test fun gruposDoMaisTreinadoAoMenos() {
        assertEquals(listOf("Peito", "Costas", "Pernas"), historyGroupNames(items))
    }

    @Test fun empateFicaEmOrdemAlfabetica() {
        assertEquals(listOf("Costas", "Pernas"), historyGroupNames(listOf(item("1", "Pernas"), item("2", "Costas"))))
    }

    @Test fun semTreinosNaoTemGrupos() = assertEquals(emptyList(), historyGroupNames(emptyList()))

    @Test fun filtraPeloGrupoEMantemAOrdem() {
        assertEquals(listOf("1", "3", "6"), filterHistory(items, "Peito").map { it.workoutId })
    }

    @Test fun semFiltroMostraTudo() = assertEquals(items, filterHistory(items, null))

    @Test fun grupoQueSumiuMostraTudo() {
        // depois de apagar o último treino do grupo, o filtro guardado na tela não deixa a lista vazia
        assertEquals(items, filterHistory(items, "Ombros"))
    }
}
