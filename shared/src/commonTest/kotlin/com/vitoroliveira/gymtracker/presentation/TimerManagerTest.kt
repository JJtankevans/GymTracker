package com.vitoroliveira.gymtracker.presentation

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class TimerManagerTest {

    @Test fun contaRegressivaAteZero() = runTest {
        val t = TimerManager(backgroundScope) { currentTime }
        t.start(3)
        assertEquals(3, t.timerState.value.remainingSeconds)
        assertTrue(t.timerState.value.isRunning)

        advanceTimeBy(1_100)
        assertEquals(2, t.timerState.value.remainingSeconds)
        advanceTimeBy(1_000)
        assertEquals(1, t.timerState.value.remainingSeconds)
        advanceTimeBy(1_000)
        val s = t.timerState.value
        assertEquals(0, s.remainingSeconds)
        assertTrue(s.isFinished)
        assertFalse(s.isRunning)
    }

    @Test fun pausarCongelaERetomarContinuaDeOndeParou() = runTest {
        val t = TimerManager(backgroundScope) { currentTime }
        t.start(10)
        advanceTimeBy(4_000)
        t.pause()
        val parado = t.timerState.value.remainingSeconds
        assertEquals(6, parado)
        assertFalse(t.timerState.value.isRunning)

        advanceTimeBy(30_000) // tempo passa pausado: nada muda
        assertEquals(parado, t.timerState.value.remainingSeconds)

        t.resume()
        assertTrue(t.timerState.value.isRunning)
        advanceTimeBy(6_100)
        assertTrue(t.timerState.value.isFinished)
    }

    @Test fun cancelarZeraTudo() = runTest {
        val t = TimerManager(backgroundScope) { currentTime }
        t.start(30)
        advanceTimeBy(2_000)
        t.cancel()
        assertEquals(TimerState(), t.timerState.value)
    }

    @Test fun novoStartSubstituiOAnterior() = runTest {
        val t = TimerManager(backgroundScope) { currentTime }
        t.start(60)
        advanceTimeBy(5_000)
        t.start(30)
        assertEquals(30, t.timerState.value.totalSeconds)
        assertEquals(30, t.timerState.value.remainingSeconds)
        advanceTimeBy(30_100)
        assertTrue(t.timerState.value.isFinished)
    }

    @Test fun duracaoInvalidaNaoInicia() = runTest {
        val t = TimerManager(backgroundScope) { currentTime }
        t.start(0)
        assertEquals(TimerState(), t.timerState.value)
    }

    @Test fun formataTempoEProgresso() {
        val s = TimerState(totalSeconds = 90, remainingSeconds = 65, isRunning = true)
        assertEquals("01:05", s.formattedTime)
        assertEquals(65f / 90f, s.progress)
        assertEquals(0f, TimerState().progress)
    }

    @Test fun refreshRecalculaDoPrazoSemDependerDaContagemEmSegundoPlano() = runTest {
        var agora = 0L
        // escopo sem avanço de tempo virtual: simula a contagem em segundo plano parada (tela bloqueada)
        val t = TimerManager(backgroundScope) { agora }
        t.start(60)
        agora += 25_000 // 25 s passaram, mas a coroutine não rodou
        assertEquals(60, t.timerState.value.remainingSeconds)
        t.refresh()
        assertEquals(35, t.timerState.value.remainingSeconds)
        assertTrue(t.timerState.value.isRunning)
    }

    @Test fun refreshDepoisDoPrazoMarcaComoTerminado() = runTest {
        var agora = 0L
        val t = TimerManager(backgroundScope) { agora }
        t.start(30)
        agora += 45_000
        t.refresh()
        val s = t.timerState.value
        assertEquals(0, s.remainingSeconds)
        assertTrue(s.isFinished)
        assertFalse(s.isRunning)
    }

    @Test fun refreshNaoMexeEmTimerPausadoOuParado() = runTest {
        var agora = 0L
        val t = TimerManager(backgroundScope) { agora }
        t.refresh() // parado
        assertEquals(TimerState(), t.timerState.value)
        t.start(20)
        agora += 5_000
        t.pause()
        val pausado = t.timerState.value
        agora += 10_000
        t.refresh()
        assertEquals(pausado, t.timerState.value)
    }
}
