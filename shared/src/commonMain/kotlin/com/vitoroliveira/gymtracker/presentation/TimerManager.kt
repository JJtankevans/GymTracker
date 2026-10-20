package com.vitoroliveira.gymtracker.presentation

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.time.TimeSource

data class TimerState(
    val totalSeconds: Int = 0,
    val remainingSeconds: Int = 0,
    val isRunning: Boolean = false,
    val isFinished: Boolean = false
) {
    val progress: Float
        get() = if (totalSeconds == 0) 0f
        else remainingSeconds.toFloat() / totalSeconds.toFloat()

    val formattedTime: String
        get() {
            val minutes = remainingSeconds / 60
            val seconds = remainingSeconds % 60
            return "${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
        }
}

private val processStart = TimeSource.Monotonic.markNow()
private fun monotonicMillis(): Long = processStart.elapsedNow().inWholeMilliseconds

/**
 * Cronômetro regressivo do descanso entre séries.
 *
 * Calcula o tempo restante a partir de um **prazo (deadline)** em relógio
 * monotônico, e não contando ticks: assim não acumula atraso e continua
 * certo se a coroutine atrasar (tela apagada, app em segundo plano).
 *
 * [nowMillis] é injetável para os testes controlarem o tempo.
 */
class TimerManager(
    private val scope: CoroutineScope,
    private val nowMillis: () -> Long = ::monotonicMillis
) {

    private val _timerState = MutableStateFlow(TimerState())
    val timerState: StateFlow<TimerState> = _timerState.asStateFlow()

    private var timerJob: Job? = null
    private var deadline = 0L
    private var pausedLeftMs = 0L

    fun start(seconds: Int) {
        timerJob?.cancel()
        if (seconds <= 0) {
            _timerState.value = TimerState()
            return
        }
        _timerState.value = TimerState(
            totalSeconds = seconds,
            remainingSeconds = seconds,
            isRunning = true
        )
        deadline = nowMillis() + seconds * 1000L
        runCountdown()
    }

    private fun runCountdown() {
        timerJob = scope.launch {
            while (true) {
                val leftMs = deadline - nowMillis()
                // arredonda pra cima: com 0,2s sobrando ainda mostra 1s
                val left = if (leftMs <= 0) 0 else ((leftMs + 999) / 1000).toInt()
                if (left != _timerState.value.remainingSeconds) {
                    _timerState.value = _timerState.value.copy(
                        remainingSeconds = left,
                        isRunning = left > 0,
                        isFinished = left == 0
                    )
                }
                if (left == 0) break
                delay((leftMs % 1000).let { if (it == 0L) 1000L else it }.coerceAtLeast(20L))
            }
        }
    }

    fun pause() {
        val s = _timerState.value
        if (!s.isRunning) return
        timerJob?.cancel()
        pausedLeftMs = (deadline - nowMillis()).coerceAtLeast(0L)
        // mostra o tempo exato do momento da pausa, não o do último tick
        val left = ((pausedLeftMs + 999) / 1000).toInt()
        _timerState.value = s.copy(remainingSeconds = left, isRunning = false, isFinished = left == 0)
    }

    fun resume() {
        val s = _timerState.value
        if (s.remainingSeconds > 0 && !s.isRunning && !s.isFinished) {
            timerJob?.cancel()
            deadline = nowMillis() + pausedLeftMs
            _timerState.value = s.copy(isRunning = true)
            runCountdown()
        }
    }

    fun reset() = cancel()

    fun cancel() {
        timerJob?.cancel()
        _timerState.value = TimerState()
    }
}
