package com.vitoroliveira.gymtracker.ui.utils

import org.junit.Assert.assertEquals
import org.junit.Test

/** Conta dos botões "−/+" de reps e carga (pura, roda na JVM sem Android). */
class StepTextTest {
    @Test fun repsSobemEDescem() {
        assertEquals("13", stepRepsText("12", 1))
        assertEquals("11", stepRepsText("12", -1))
    }

    @Test fun repsVazioContaComoZeroENuncaFicaNegativo() {
        assertEquals("1", stepRepsText("", 1))
        assertEquals("0", stepRepsText("0", -1))
        assertEquals("0", stepRepsText("", -1))
    }

    @Test fun repsParamNoLimiteDeTresDigitos() {
        assertEquals("999", stepRepsText("999", 1))
    }

    @Test fun cargaSomaMeioPassoComVirgula() {
        assertEquals("82,5", stepKgText("80", 2.5))
        assertEquals("80", stepKgText("82,5", -2.5))
        assertEquals("85", stepKgText("82,5", 2.5))
    }

    @Test fun cargaAceitaPontoEVazio() {
        assertEquals("22,5", stepKgText("20.0", 2.5))
        assertEquals("2,5", stepKgText("", 2.5))
    }

    @Test fun cargaNuncaFicaNegativa() {
        assertEquals("0", stepKgText("1", -2.5))
        assertEquals("0", stepKgText("", -2.5))
    }
}
