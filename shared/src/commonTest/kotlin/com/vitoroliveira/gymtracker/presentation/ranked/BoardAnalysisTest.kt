package com.vitoroliveira.gymtracker.presentation.ranked

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class BoardAnalysisTest {
    // a mesma turma do teste em SQL (migração 018): 12 pessoas
    private val turma = listOf(550, 450, 350, 340, 260, 240, 200, 170, 130, 100, 0, 0)

    @Test fun quemEstaNoTopoSobe() {
        val a = analyzeBoard(turma, 550)
        assertEquals(1, a.position); assertEquals(Zone.PROMOTE, a.zone); assertEquals(40, a.projectedDelta)
        assertNull(a.pointsToPromotion); assertNull(a.pointsToSafety)
        assertEquals(3, a.promoteCount); assertEquals(3, a.demoteCount)
    }

    @Test fun quartoLugarFaltaPoucoParaSubir() {
        val a = analyzeBoard(turma, 340)
        assertEquals(4, a.position); assertEquals(Zone.SAFE, a.zone); assertEquals(10, a.projectedDelta)
        assertEquals(10, a.pointsToPromotion) // igualar os 350 do terceiro
        assertNull(a.pointsToSafety)
    }

    @Test fun nonoLugarEstaSeguroMasLongeDoTopo() {
        val a = analyzeBoard(turma, 130)
        assertEquals(9, a.position); assertEquals(Zone.SAFE, a.zone); assertEquals(-10, a.projectedDelta)
        assertEquals(220, a.pointsToPromotion)
    }

    @Test fun decimoLugarEstaNaZonaDeDescida() {
        val a = analyzeBoard(turma, 100)
        assertEquals(10, a.position); assertEquals(Zone.DEMOTE, a.zone); assertEquals(-25, a.projectedDelta)
        assertEquals(30, a.pointsToSafety) // igualar os 130 do nono
    }

    @Test fun quemNaoTreinouPrecisaDeUmTreinoValido() {
        val a = analyzeBoard(turma, 0)
        assertEquals(11, a.position); assertEquals(Zone.DEMOTE, a.zone); assertEquals(-35, a.projectedDelta)
        assertEquals(130, a.pointsToSafety)
        // turma toda zerada: qualquer treino válido (100) tira da descida
        val zeros = analyzeBoard(List(10) { 0 }, 0)
        assertEquals(Zone.DEMOTE, zeros.zone); assertEquals(BASE_POINTS, zeros.pointsToSafety); assertEquals(BASE_POINTS, zeros.pointsToPromotion)
    }

    @Test fun turmaPequenaNaoTemFaixas() {
        val a = analyzeBoard(listOf(300, 200, 100), 200)
        assertEquals(Zone.SAFE, a.zone); assertEquals(0, a.promoteCount); assertNull(a.pointsToPromotion)
        assertEquals(10, a.projectedDelta)
    }

    @Test fun empateDividePosicao() {
        val a = analyzeBoard(listOf(400, 300, 300, 100, 50, 20), 300)
        assertEquals(2, a.position)
    }

    @Test fun diasQueFaltamParaFecharASemana() {
        assertEquals(7, weekDaysLeft(LocalDate(2026, 11, 2)))  // segunda
        assertEquals(4, weekDaysLeft(LocalDate(2026, 11, 5)))  // quinta
        assertEquals(1, weekDaysLeft(LocalDate(2026, 11, 8)))  // domingo
    }
}
