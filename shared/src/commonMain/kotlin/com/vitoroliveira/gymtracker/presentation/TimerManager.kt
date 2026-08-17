package com.vitoroliveira.gymtracker.presentation

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

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

class TimerManager(private val scope: CoroutineScope) {

    private val _timerState = MutableStateFlow(TimerState())
    val timerState: StateFlow<TimerState> = _timerState.asStateFlow()

    private var timerJob: Job? = null

    fun start(seconds: Int) {
        timerJob?.cancel()
        _timerState.value = TimerState(
            totalSeconds = seconds,
            remainingSeconds = seconds,
            isRunning = true,
            isFinished = false
        )
        runCountdown()
    }

    /**
     * Roda a contagem regressiva a partir do estado atual de
     * [_timerState], sem alterar totalSeconds/remainingSeconds.
     * Usado tanto por [start] quanto por [resume], para que o
     * `progress` (remaining/total) continue correto após pausar.
     */
    private fun runCountdown() {
        timerJob = scope.launch {
            while (_timerState.value.remainingSeconds > 0) {
                delay(1000L)
                val remaining = _timerState.value.remainingSeconds - 1
                _timerState.value = _timerState.value.copy(
                    remainingSeconds = remaining,
                    isFinished = remaining == 0,
                    isRunning = remaining > 0
                )
            }
        }
    }

    fun pause() {
        timerJob?.cancel()
        _timerState.value = _timerState.value.copy(isRunning = false)
    }

    fun resume() {
        if (_timerState.value.remainingSeconds > 0 && !_timerState.value.isRunning) {
            timerJob?.cancel()
            _timerState.value = _timerState.value.copy(isRunning = true)
            runCountdown()
        }
    }

    fun reset() {
        timerJob?.cancel()
        _timerState.value = TimerState()
    }

    fun cancel() {
        timerJob?.cancel()
        _timerState.value = TimerState()
    }
}