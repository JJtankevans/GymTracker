package com.vitoroliveira.gymtracker.presentation.workout

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GymMathTest {
    @Test fun cemQuilosNaBarraOlimpica() {
        val r = PlateCalculator.plates(100.0)
        assertEquals(listOf(25.0, 15.0), r.perSide)
        assertEquals(100.0, r.achievedKg)
        assertEquals(0.0, r.missingKg)
    }

    @Test fun usaAnilhaDeUmQuiloEUmQuarto() {
        val r = PlateCalculator.plates(62.5)
        assertEquals(listOf(20.0, 1.25), r.perSide)
        assertEquals(62.5, r.achievedKg)
    }

    @Test fun cargaQueNaoFechaMostraOQueFalta() {
        val r = PlateCalculator.plates(61.0)
        assertEquals(60.0, r.achievedKg)
        assertEquals(1.0, r.missingKg)
    }

    @Test fun abaixoDaBarra() {
        val r = PlateCalculator.plates(15.0, barKg = 20.0)
        assertTrue(r.belowBar)
        assertTrue(r.perSide.isEmpty())
    }

    @Test fun soABarra() {
        val r = PlateCalculator.plates(20.0)
        assertTrue(r.perSide.isEmpty())
        assertEquals(20.0, r.achievedKg)
        assertTrue(!r.belowBar)
    }

    @Test fun barraWEAnilhasLimitadas() {
        val r = PlateCalculator.plates(40.0, barKg = 10.0, available = listOf(10.0, 5.0))
        assertEquals(listOf(10.0, 5.0), r.perSide)
        assertEquals(40.0, r.achievedKg)
    }

    @Test fun tabelaDe1RMComecaEm100PorCentoEDesce() {
        val t = repMaxTable(100.0)
        assertEquals(1, t.first().reps)
        assertEquals(100.0, t.first().weightKg)
        assertEquals(100, t.first().percent)
        assertTrue(t.zipWithNext().all { (a, b) -> b.weightKg <= a.weightKg })
        // Epley: 10 reps ≈ 75% do 1RM -> 75 kg
        assertEquals(75.0, t.first { it.reps == 10 }.weightKg)
        assertTrue(t.all { it.weightKg % 2.5 == 0.0 })
    }

    @Test fun tabelaVaziaSem1RM() {
        assertTrue(repMaxTable(0.0).isEmpty())
    }
}
