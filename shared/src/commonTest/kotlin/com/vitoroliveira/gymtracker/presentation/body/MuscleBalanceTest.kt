package com.vitoroliveira.gymtracker.presentation.body

import com.vitoroliveira.gymtracker.data.catalog.CatalogMuscle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MuscleBalanceTest {
    private fun week(vararg p: Pair<CatalogMuscle, Int>) =
        weeklySetsByMuscleOf(p.toMap())

    private fun weeklySetsByMuscleOf(m: Map<CatalogMuscle, Int>) =
        CatalogMuscle.entries.map { MuscleSets(it, it.label, m[it] ?: 0) }

    @Test fun poucasSeriesNaoComparam() {
        assertNull(muscleBalance(week(CatalogMuscle.PEITO to 3, CatalogMuscle.COSTAS to 2)))
    }

    @Test fun cardioEMobilidadeNaoEntram() {
        assertNull(muscleBalance(week(CatalogMuscle.CARDIO to 30, CatalogMuscle.MOBILIDADE to 20)))
    }

    @Test fun treinoEquilibradoNaoTemAviso() {
        val b = assertNotNull(muscleBalance(week(
            CatalogMuscle.PEITO to 6, CatalogMuscle.TRICEPS to 3, CatalogMuscle.COSTAS to 7, CatalogMuscle.BICEPS to 3,
            CatalogMuscle.QUADRICEPS to 6, CatalogMuscle.POSTERIOR to 4
        )))
        assertTrue(b.balanced)
        assertNull(b.hint)
        assertEquals(29, b.total)
    }

    @Test fun mostraAFatiaDeCadaBloco() {
        val b = assertNotNull(muscleBalance(week(CatalogMuscle.PEITO to 5, CatalogMuscle.COSTAS to 5, CatalogMuscle.QUADRICEPS to 5, CatalogMuscle.ABDOMEN to 5)))
        assertEquals(4, b.rows.size)
        assertTrue(b.rows.all { it.share == 0.25f })
    }

    @Test fun avisaQuandoEmpurraMuitoMaisQuePuxa() {
        val b = assertNotNull(muscleBalance(week(CatalogMuscle.PEITO to 10, CatalogMuscle.OMBROS to 4, CatalogMuscle.COSTAS to 4, CatalogMuscle.QUADRICEPS to 6)))
        assertTrue(b.hint!!.startsWith("Você empurrou"))
        assertTrue("14 x 4" in b.hint!!)
        assertEquals(false, b.balanced)
    }

    @Test fun avisaQuandoPuxaMuitoMaisQueEmpurra() {
        val b = assertNotNull(muscleBalance(week(CatalogMuscle.COSTAS to 12, CatalogMuscle.PEITO to 4, CatalogMuscle.QUADRICEPS to 6)))
        assertTrue(b.hint!!.startsWith("Você puxou"))
    }

    @Test fun avisaDasPernasEsquecidas() {
        val b = assertNotNull(muscleBalance(week(CatalogMuscle.PEITO to 8, CatalogMuscle.COSTAS to 8, CatalogMuscle.QUADRICEPS to 2)))
        assertTrue(b.hint!!.startsWith("Pernas ficaram com 11%"))
    }

    @Test fun soPernasNaoDisparaDesequilibrioDeEmpurrarEPuxar() {
        val b = assertNotNull(muscleBalance(week(CatalogMuscle.QUADRICEPS to 8, CatalogMuscle.GLUTEOS to 4)))
        assertTrue(b.balanced)
    }

    @Test fun limiteExatoDe70PorCentoNaoAvisa() {
        // puxar = 7, empurrar = 10: exatamente 0,7 -> ainda ok
        val b = assertNotNull(muscleBalance(week(CatalogMuscle.PEITO to 10, CatalogMuscle.COSTAS to 7, CatalogMuscle.QUADRICEPS to 6)))
        assertTrue(b.balanced)
    }
}
