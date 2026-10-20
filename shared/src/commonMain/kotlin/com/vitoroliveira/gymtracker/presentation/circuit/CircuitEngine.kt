package com.vitoroliveira.gymtracker.presentation.circuit

enum class CircuitPhase { IDLE, SET, REST, DONE }

/** Um passo do circuito = uma série de um exercício. */
data class CircuitStep(
    val exerciseId: String,
    val exerciseName: String,
    val setNumber: Int,
    val setsTotal: Int,
    val reps: Int,
    val weightKg: Double,
    /** Descanso DEPOIS desta série (já inclui troca de aparelho, se for o caso). */
    val restSeconds: Int
)

/** Série concluída, pronta para ser gravada no treino. */
data class CommittedSet(val step: CircuitStep, val durationSec: Int, val corrected: Boolean)

sealed interface CircuitEvent {
    data class SetStarted(val step: CircuitStep, val predictedSec: Int) : CircuitEvent
    /** A série acabou pela previsão (o usuário não tocou). */
    data class SetAutoEnded(val step: CircuitStep) : CircuitEvent
    data class RestEnded(val next: CircuitStep?) : CircuitEvent
    data class SetCommitted(val set: CommittedSet) : CircuitEvent
    data object Finished : CircuitEvent
}

data class CircuitState(
    val phase: CircuitPhase = CircuitPhase.IDLE,
    val steps: List<CircuitStep> = emptyList(),
    val stepIndex: Int = 0,
    val predictedSetSec: Int = 0,
    val setElapsedMs: Long = 0,
    val restTotalSec: Int = 0,
    val restLeftMs: Long = 0,
    /** Duração da série que está "pendente" durante o descanso (pode ser corrigida). */
    val pendingDurationSec: Int = 0,
    val completed: List<CommittedSet> = emptyList(),
    val autoEnded: Boolean = false
) {
    val step: CircuitStep? get() = steps.getOrNull(stepIndex)
    val next: CircuitStep? get() = steps.getOrNull(stepIndex + 1)
    val isLast: Boolean get() = stepIndex >= steps.lastIndex
}

/**
 * Máquina de estados do circuito. Sem relógio próprio: quem usa chama [tick]
 * (passando o tempo atual em ms) várias vezes por segundo.
 *
 * Fluxo sem tocar em nada: SÉRIE (conta até o tempo previsto) → DESCANSO →
 * próxima SÉRIE → ... Toques só pra corrigir:
 *  - [tapDone] na série: terminei antes; grava o tempo real.
 *  - [tapStillGoing] no descanso: ainda não tinha terminado; o tempo que passou
 *    no descanso vira parte da série, e o descanso recomeça.
 *  - [tapDone] no descanso: pular o resto e começar a próxima.
 */
class CircuitEngine(
    steps: List<CircuitStep>,
    private val learner: SetTimingLearner,
    /** Descanso depois da última série: só uma janela pra corrigir. */
    private val lastGraceSec: Int = 15
) {
    var state = CircuitState(steps = steps)
        private set

    private var setStartedAt = 0L
    private var restStartedAt = 0L
    private var restExtraMs = 0L
    private var corrected = false

    fun start(now: Long): List<CircuitEvent> {
        if (state.steps.isEmpty()) return emptyList()
        return beginSet(0, now, emptyList())
    }

    fun tick(now: Long): List<CircuitEvent> {
        val s = state
        val events = mutableListOf<CircuitEvent>()
        when (s.phase) {
            CircuitPhase.SET -> {
                val elapsed = now - setStartedAt
                state = s.copy(setElapsedMs = elapsed)
                if (elapsed >= s.predictedSetSec * 1000L) {
                    events += CircuitEvent.SetAutoEnded(s.step!!)
                    events += enterRest(now, s.predictedSetSec, auto = true)
                }
            }
            CircuitPhase.REST -> {
                val left = (restDurationMs() - (now - restStartedAt)).coerceAtLeast(0)
                state = s.copy(restLeftMs = left)
                if (left == 0L) events += finishRest(now)
            }
            else -> {}
        }
        return events
    }

    /** SÉRIE: terminei agora. DESCANSO: pular o descanso. */
    fun tapDone(now: Long): List<CircuitEvent> = when (state.phase) {
        CircuitPhase.SET -> enterRest(now, ((now - setStartedAt) / 1000).toInt().coerceAtLeast(1), auto = false)
        CircuitPhase.REST -> finishRest(now)
        else -> emptyList()
    }

    /** DESCANSO: "ainda não terminei". Soma o tempo de descanso decorrido à série e reinicia o descanso. */
    fun tapStillGoing(now: Long): List<CircuitEvent> {
        if (state.phase != CircuitPhase.REST) return emptyList()
        val restElapsedSec = ((now - restStartedAt) / 1000).toInt()
        corrected = true
        restStartedAt = now
        restExtraMs = 0
        state = state.copy(
            pendingDurationSec = state.pendingDurationSec + restElapsedSec,
            restLeftMs = restDurationMs(),
            autoEnded = false
        )
        return emptyList()
    }

    /** DESCANSO: +[seconds] no descanso atual. */
    fun extendRest(seconds: Int = 15) {
        if (state.phase != CircuitPhase.REST) return
        restExtraMs += seconds * 1000L
        state = state.copy(restTotalSec = state.restTotalSec + seconds)
    }

    /** Muda reps/carga da série atual e das próximas do mesmo exercício. */
    fun adjustCurrent(reps: Int? = null, weightKg: Double? = null) {
        val cur = state.step ?: return
        val updated = state.steps.mapIndexed { i, st ->
            if (i >= state.stepIndex && st.exerciseId == cur.exerciseId)
                st.copy(reps = reps?.coerceIn(1, 99) ?: st.reps, weightKg = weightKg?.coerceIn(0.0, 500.0) ?: st.weightKg)
            else st
        }
        state = state.copy(steps = updated)
        if (state.phase == CircuitPhase.SET && reps != null) {
            state = state.copy(predictedSetSec = learner.predictSeconds(cur.exerciseId, updated[state.stepIndex].reps).coerceAtLeast(((state.setElapsedMs / 1000) + 5).toInt()))
        }
    }

    private fun restDurationMs() = state.restTotalSec * 1000L

    private fun beginSet(index: Int, now: Long, events: List<CircuitEvent>): List<CircuitEvent> {
        val step = state.steps[index]
        val predicted = learner.predictSeconds(step.exerciseId, step.reps)
        setStartedAt = now
        corrected = false
        state = state.copy(
            phase = CircuitPhase.SET, stepIndex = index, predictedSetSec = predicted,
            setElapsedMs = 0, restLeftMs = 0, restTotalSec = 0, pendingDurationSec = 0, autoEnded = false
        )
        return events + CircuitEvent.SetStarted(step, predicted)
    }

    private fun enterRest(now: Long, durationSec: Int, auto: Boolean): List<CircuitEvent> {
        val step = state.step!!
        val rest = if (state.isLast) lastGraceSec else step.restSeconds
        restStartedAt = now
        restExtraMs = 0
        corrected = false
        state = state.copy(
            phase = CircuitPhase.REST, restTotalSec = rest, restLeftMs = rest * 1000L,
            pendingDurationSec = durationSec, autoEnded = auto
        )
        return emptyList()
    }

    /** Fecha a série pendente (aprende com ela) e vai pra próxima, ou termina. */
    private fun finishRest(now: Long): List<CircuitEvent> {
        val s = state
        val step = s.step!!
        val committed = CommittedSet(step, s.pendingDurationSec, corrected)
        learner.record(step.exerciseId, s.pendingDurationSec, step.reps)
        val events = mutableListOf<CircuitEvent>(CircuitEvent.SetCommitted(committed))
        state = s.copy(completed = s.completed + committed)
        return if (s.isLast) {
            state = state.copy(phase = CircuitPhase.DONE, restLeftMs = 0)
            events += CircuitEvent.Finished
            events
        } else {
            events += CircuitEvent.RestEnded(s.next)
            beginSet(s.stepIndex + 1, now, events)
        }
    }
}

/** Monta os passos do circuito a partir dos exercícios. */
object CircuitPlanner {
    enum class Order { SEQUENTIAL, ROUND_ROBIN }

    data class Input(val exerciseId: String, val name: String, val sets: Int, val reps: Int, val weightKg: Double)

    fun build(
        exercises: List<Input>,
        order: Order = Order.SEQUENTIAL,
        restSeconds: Int = 90,
        /** Tempo extra pra trocar de aparelho (só no modo sequencial, na última série do exercício). */
        transitionSeconds: Int = 30
    ): List<CircuitStep> {
        fun step(e: Input, n: Int, rest: Int) = CircuitStep(e.exerciseId, e.name, n, e.sets, e.reps, e.weightKg, rest)
        return when (order) {
            Order.SEQUENTIAL -> exercises.flatMapIndexed { ei, e ->
                (1..e.sets).map { n ->
                    val endsExercise = n == e.sets && ei < exercises.lastIndex
                    step(e, n, restSeconds + if (endsExercise) transitionSeconds else 0)
                }
            }
            Order.ROUND_ROBIN -> {
                val max = exercises.maxOfOrNull { it.sets } ?: 0
                (1..max).flatMap { round -> exercises.filter { it.sets >= round }.map { step(it, round, restSeconds) } }
            }
        }
    }
}
