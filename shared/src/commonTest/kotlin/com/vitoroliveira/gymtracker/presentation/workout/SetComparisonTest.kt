package com.vitoroliveira.gymtracker.presentation.workout

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SetComparisonTest {
    @Test fun semHistoricoNaoCompara() = assertNull(compareSet(40.0, 10, null, null, null))

    @Test fun passandoORecordeAvisaDe() {
        val c = compareSet(52.5, 8, 50.0, 8, 50.0)!!
        assertEquals(ComparisonTone.RECORD, c.tone)
        assertEquals("Recorde! +2,5 kg sobre 50 kg", c.text)
    }

    @Test fun igualAoRecordeNaoEhRecorde() {
        assertEquals(ComparisonTone.SAME, compareSet(50.0, 8, 50.0, 8, 50.0)!!.tone)
    }

    @Test fun cargaMaiorQueAUltimaVez() {
        val c = compareSet(42.5, 10, 40.0, 12, 60.0)!!
        assertEquals(ComparisonTone.UP, c.tone)
        assertEquals("+2,5 kg vs. última", c.text)
    }

    @Test fun cargaMenorQueAUltimaVez() {
        val c = compareSet(37.5, 10, 40.0, 10, 60.0)!!
        assertEquals(ComparisonTone.DOWN, c.tone)
        assertEquals("-2,5 kg vs. última", c.text)
    }

    @Test fun mesmaCargaMaisRepeticoes() {
        assertEquals("+1 rep vs. última", compareSet(40.0, 11, 40.0, 10, 60.0)!!.text)
        assertEquals("+3 reps vs. última", compareSet(40.0, 13, 40.0, 10, 60.0)!!.text)
    }

    @Test fun mesmaCargaMenosRepeticoes() {
        val c = compareSet(40.0, 8, 40.0, 10, 60.0)!!
        assertEquals(ComparisonTone.DOWN, c.tone)
        assertEquals("-2 reps vs. última", c.text)
    }

    @Test fun igualEmTudo() {
        assertEquals("Igual à última vez", compareSet(40.0, 10, 40.0, 10, 60.0)!!.text)
    }

    @Test fun pesoDoCorpoComparaSoAsRepeticoes() {
        assertEquals("+2 reps vs. última", compareSet(0.0, 14, 0.0, 12, 0.0)!!.text)
        // carga zero nunca é recorde
        assertEquals(ComparisonTone.SAME, compareSet(0.0, 12, 0.0, 12, 0.0)!!.tone)
    }

    @Test fun diferencaDeCentesimosDePontoFlutuanteNaoAparece() {
        // 0.1 + 0.2 != 0.3 em ponto flutuante; a comparação arredonda
        assertEquals(ComparisonTone.SAME, compareSet(0.1 + 0.2, 10, 0.3, 10, null)!!.tone)
    }
}
