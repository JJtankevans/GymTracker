package com.vitoroliveira.gymtracker.presentation.circuit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.time.TimeSource

/** Liga o [CircuitEngine] a um relógio e guarda o que o app aprendeu sobre o ritmo do usuário. */
class CircuitViewModel(
    private val store: TimingStore,
    private val nowMillis: () -> Long = defaultNow
) : ViewModel() {

    val learner = SetTimingLearner(store.load())
    private var engine: CircuitEngine? = null
    private var ticker: Job? = null

    private val _state = MutableStateFlow(CircuitState())
    val state: StateFlow<CircuitState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<CircuitEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<CircuitEvent> = _events.asSharedFlow()

    val isRunning: Boolean get() = state.value.phase == CircuitPhase.SET || state.value.phase == CircuitPhase.REST

    fun start(steps: List<CircuitStep>) {
        if (isRunning || steps.isEmpty()) return
        val e = CircuitEngine(steps, learner).also { engine = it }
        dispatch(e.start(nowMillis()))
        _state.value = e.state
        ticker = viewModelScope.launch {
            while (true) {
                delay(100)
                dispatch(e.tick(nowMillis()))
                _state.value = e.state
                if (e.state.phase == CircuitPhase.DONE) break
            }
        }
    }

    fun tapDone() = act { it.tapDone(nowMillis()) }
    fun tapStillGoing() = act { it.tapStillGoing(nowMillis()) }
    fun extendRest(seconds: Int = 15) = act { it.extendRest(seconds); emptyList() }
    fun adjust(reps: Int? = null, weightKg: Double? = null) = act { it.adjustCurrent(reps, weightKg); emptyList() }

    fun stop() {
        ticker?.cancel(); ticker = null
        engine = null
        _state.value = CircuitState()
    }

    private fun act(block: (CircuitEngine) -> List<CircuitEvent>) {
        val e = engine ?: return
        dispatch(block(e))
        _state.value = e.state
    }

    private fun dispatch(events: List<CircuitEvent>) {
        events.forEach {
            _events.tryEmit(it)
            if (it is CircuitEvent.SetCommitted) store.save(learner.snapshot())
        }
    }

    override fun onCleared() { ticker?.cancel() }

    companion object {
        private val origin = TimeSource.Monotonic.markNow()
        val defaultNow: () -> Long = { origin.elapsedNow().inWholeMilliseconds }
    }
}
