package com.vitoroliveira.gymtracker.presentation

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RestAlertClaimTest {
    @Test fun soUmDosDoisAvisaDoMesmoFim() {
        val c = RestAlertClaim()
        assertTrue(c.claim(1_000))   // o primeiro avisa
        assertFalse(c.claim(1_000))  // o segundo fica quieto
    }

    @Test fun descansoNovoAvisaDeNovo() {
        val c = RestAlertClaim()
        assertTrue(c.claim(1_000)); assertFalse(c.claim(1_000))
        assertTrue(c.claim(2_000))   // outro descanso, outro aviso
        assertFalse(c.claim(2_000))
    }

    @Test fun semHorarioSempreAvisa() {
        val c = RestAlertClaim()
        assertTrue(c.claim(0)); assertTrue(c.claim(0))
    }

    @Test fun reivindicarOutroFimNaoApagaOAnteriorParaSempre() {
        val c = RestAlertClaim()
        c.claim(1_000); c.claim(2_000)
        // o 1_000 antigo reaparecendo (alarme atrasado de um descanso já substituído) conta como novo: quem agenda cancela o anterior
        assertTrue(c.claim(1_000))
    }
}
