package com.vitoroliveira.gymtracker.wear

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.vitoroliveira.gymtracker.watch.DEFAULT_MAX_HR
import com.vitoroliveira.gymtracker.watch.HrZone
import com.vitoroliveira.gymtracker.watch.IntervalPhase
import com.vitoroliveira.gymtracker.watch.IntervalPlan
import com.vitoroliveira.gymtracker.watch.IntervalState
import com.vitoroliveira.gymtracker.watch.WatchOutbox
import com.vitoroliveira.gymtracker.watch.WatchRoutine
import com.vitoroliveira.gymtracker.watch.WatchRoutines
import com.vitoroliveira.gymtracker.watch.WatchSession
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID

/** Resumo mostrado ao terminar. [workoutId] null = treino rápido (não vai para o celular). */
data class FinishSummary(
    val title: String,
    val durationMs: Long,
    val sets: Int,
    val volumeKg: Double,
    val exercisesDone: Int,
    val exercisesTotal: Int,
    val avgHeartRate: Int?,
    val maxHeartRate: Int?,
    val records: Int,
    val workoutId: String?,
    /** Melhor série pelo 1RM estimado: nome do exercício e 1RM (null no treino rápido). */
    val bestLiftName: String? = null,
    val bestLiftKg: Double? = null,
    /** Amostras (~segundos) por zona de batimento; vazio sem batimentos. */
    val zones: List<Int> = emptyList()
)

/** Timer de intervalos rodando. [pausedElapsed] != null = pausado com esse tanto já decorrido. */
data class IntervalRun(val plan: IntervalPlan, val startedAt: Long, val pausedElapsed: Long? = null) {
    val paused: Boolean get() = pausedElapsed != null
    /** Terminou (fica parado no fim para a tela mostrar "FIM!"). */
    val done: Boolean get() = (pausedElapsed ?: 0L) >= plan.totalMs
    fun elapsed(now: Long): Long = pausedElapsed ?: (now - startedAt)
    fun endsAt(): Long = startedAt + plan.totalMs
}

/** Situação da conversa com o celular, para a tela inicial. */
enum class PhoneStatus { UNKNOWN, CHECKING, CONNECTED, OUT_OF_RANGE, ERROR }

/**
 * Estado do app do relógio. As regras do treino ficam em [WatchSession] (no :shared, testadas);
 * aqui só se junta armazenamento, Data Layer, alarme do descanso, vibração e batimentos.
 */
class WatchController(
    private val app: Context,
    private val store: WatchStore,
    private val sync: WatchSync,
    private val heart: HeartRateMonitor,
    private val scope: CoroutineScope
) {
    var routines by mutableStateOf(store.routines); private set
    var routinesAt by mutableLongStateOf(store.routinesReceivedAt); private set
    var session by mutableStateOf(store.session); private set
    var rest by mutableStateOf(store.rest); private set
    var outbox by mutableStateOf(store.outbox); private set
    var phone by mutableStateOf(PhoneStatus.UNKNOWN); private set
    var heartRate by mutableStateOf<Int?>(null); private set
    var ambient by mutableStateOf(false); private set
    var lastFinish by mutableStateOf<FinishSummary?>(null); private set
    /** "RECORDE!" na tela do descanso logo depois de bater um recorde. */
    var recordFlash by mutableStateOf(false); private set

    // ajustes
    var accent by mutableIntStateOf(store.accent); private set
    var restSeconds by mutableIntStateOf(store.restSeconds); private set
    var kgStep by mutableFloatStateOf(store.kgStep); private set
    var autoRest by mutableStateOf(store.autoRest); private set
    var heartRateOn by mutableStateOf(store.heartRateOn); private set

    // timer de intervalos
    var intervalPlan by mutableStateOf(store.intervalPlan); private set
    var intervalRun by mutableStateOf<IntervalRun?>(null); private set

    // série em edição (repetições e carga que a coroa mexe)
    var draftReps by mutableIntStateOf(10); private set
    var draftKg by mutableDoubleStateOf(0.0); private set

    private var resumed = false
    private var stopListening: (() -> Unit)? = null
    private var warned = false
    private var hrSamples = 0

    init {
        loadDraft()
    }

    // ───────────── ciclo de vida da tela ─────────────

    fun onResume() {
        resumed = true
        checkRestFromAlarm()
        session?.let { OngoingWorkout.show(app, it.title, it.startedAt) }
        if (stopListening == null) {
            stopListening = runCatching {
                sync.listen(onRoutines = ::applyRoutines, onWorkoutSaved = ::onSaved)
            }.getOrNull()
        }
        refresh()
        updateHeartRate()
    }

    fun onPause() {
        resumed = false
        stopListening?.invoke()
        stopListening = null
        updateHeartRate()
        session?.let { store.session = it }
    }

    fun onAmbientChanged(on: Boolean) {
        ambient = on
        if (!on) checkRestFromAlarm()
        updateHeartRate()
    }

    // ───────────── celular ─────────────

    /** Lê as rotinas que já estão no relógio, pede novas ao celular e tenta mandar os treinos da fila. */
    fun refresh() {
        scope.launch {
            phone = PhoneStatus.CHECKING
            try {
                sync.readRoutines()?.let(::applyRoutines)
                phone = if (sync.requestRoutines()) PhoneStatus.CONNECTED else PhoneStatus.OUT_OF_RANGE
                flushOutbox()
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                phone = PhoneStatus.ERROR
            }
        }
    }

    private fun applyRoutines(r: WatchRoutines) {
        val current = routines
        if (current != null && r.generatedAt < current.generatedAt) return
        routines = r
        store.routines = r
        WorkoutTileService.refresh(app)
        if (current == null || r.generatedAt > current.generatedAt) {
            routinesAt = System.currentTimeMillis()
            store.routinesReceivedAt = routinesAt
        }
    }

    private suspend fun flushOutbox() {
        for (item in outbox.items) {
            val id = item.workout.id
            if (!item.posted) {
                sync.sendWorkout(item.workout)
                saveOutbox(outbox.markPosted(id))
            } else if (!sync.isQueued(id)) {
                saveOutbox(outbox.remove(id))     // o celular gravou e apagou o item
            }
        }
    }

    private fun onSaved(workoutId: String) = saveOutbox(outbox.remove(workoutId))

    private fun saveOutbox(o: WatchOutbox) {
        outbox = o
        store.outbox = o
    }

    // ───────────── treino ─────────────

    fun startRoutine(routine: WatchRoutine) {
        if (routine.exercises.isEmpty()) return
        begin(WatchSession.start(UUID.randomUUID().toString(), routine, System.currentTimeMillis()))
    }

    fun startQuick() {
        begin(WatchSession.quick(UUID.randomUUID().toString(), store.quickReps, store.quickKg.toDouble(), System.currentTimeMillis()))
    }

    private fun begin(s: WatchSession) {
        cancelRest()
        lastFinish = null
        saveSession(s)
        loadDraft()
        OngoingWorkout.show(app, s.title, s.startedAt)
        updateHeartRate()
    }

    fun select(index: Int) {
        val s = session ?: return
        saveSession(s.select(index))
        loadDraft()
    }

    /** Exercícios da conta que ainda não estão no treino (da rotina "Todos os exercícios"). */
    val extraExercises: List<com.vitoroliveira.gymtracker.watch.WatchExercise>
        get() {
            val s = session ?: return emptyList()
            val inSession = s.exercises.map { it.id }.toSet()
            return routines?.routines?.firstOrNull { it.groupId == null }?.exercises.orEmpty().filter { it.id !in inSession }
        }

    fun addExercise(ex: com.vitoroliveira.gymtracker.watch.WatchExercise) {
        val s = session ?: return
        saveSession(s.addExercise(ex))
        loadDraft()
    }

    fun changeReps(delta: Int) { draftReps = (draftReps + delta).coerceIn(1, 99) }

    fun changeKg(steps: Int) {
        val v = draftKg + steps * kgStep
        // arredonda ao passo para não acumular 0,1 de erro de ponto flutuante
        draftKg = (Math.round(v / kgStep) * kgStep.toDouble()).coerceIn(0.0, 500.0)
    }

    fun registerSet() {
        val s = session ?: return
        val result = s.addSet(draftReps, draftKg, System.currentTimeMillis())
        saveSession(result.session)
        if (s.isQuick) { store.quickReps = draftReps; store.quickKg = draftKg.toFloat() }
        app.buzz(if (result.newRecord) Buzz.RECORD else Buzz.TAP)
        recordFlash = result.newRecord
        loadDraft()
        if (autoRest && !result.session.allDone) startRest(restSeconds)
    }

    fun undoLastSet() {
        val s = session ?: return
        saveSession(s.undoLastSet())
        loadDraft()
    }

    /** Termina: guarda o resumo, põe o treino na fila do celular e limpa o treino em andamento. */
    fun finish() {
        val s = session ?: return
        val now = System.currentTimeMillis()
        val workout = s.toWorkout(now)
        val best = s.bestLift()
        lastFinish = FinishSummary(
            s.title, now - s.startedAt, s.totalSets, s.volumeKg,
            s.doneExercises, if (s.isQuick) 0 else s.exercises.size,
            s.avgHeartRate, s.maxHeartRate, s.records, workout?.id,
            bestLiftName = best?.first?.name, bestLiftKg = best?.second,
            zones = s.hrZones
        )
        if (workout != null) saveOutbox(outbox.add(workout))
        close()
        if (workout != null) {
            scope.launch {
                try {
                    flushOutbox()
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                }
            }
        }
    }

    fun discard() {
        lastFinish = null
        close()
    }

    private fun close() {
        cancelRest()
        saveSession(null)
        OngoingWorkout.hide(app)
        heartRate = null
        updateHeartRate()
    }

    private fun saveSession(s: WatchSession?) {
        val changedTile = (session == null) != (s == null) || session?.totalSets != s?.totalSets
        session = s
        store.session = s
        if (changedTile) WorkoutTileService.refresh(app)
    }

    // ───────────── atalho do tile ─────────────

    /** Grupo que o tile pediu para iniciar ("" = treino rápido); a tela inicial consome e limpa. */
    var pendingStart by mutableStateOf<String?>(null); private set

    fun requestStart(groupId: String?) { pendingStart = groupId }

    /** Inicia o que o tile pediu. true = começou um treino novo (a tela navega para ele). */
    fun consumePendingStart(): Boolean {
        val id = pendingStart ?: return false
        pendingStart = null
        if (session != null) return false            // já tem treino: o tile só abre o app
        if (id.isEmpty()) { startQuick(); return true }
        val routine = routines?.routines?.firstOrNull { it.groupId == id } ?: return false
        startRoutine(routine)
        return true
    }

    private fun loadDraft() {
        val s = session ?: return
        val (reps, kg) = s.suggestion(s.current)
        draftReps = reps.coerceIn(1, 99)
        draftKg = kg
    }

    // ───────────── descanso ─────────────

    private var exactAlarm = true

    fun startRest(seconds: Int) {
        val r = RestState(System.currentTimeMillis() + seconds * 1000L, seconds)
        saveRest(r)
        warned = false
        exactAlarm = RestAlarm.schedule(app, seconds * 1000L)
    }

    /** Soma/tira segundos do descanso em andamento (mínimo de 5 s restantes). */
    fun addRest(deltaSec: Int) {
        val r = rest ?: return
        val now = System.currentTimeMillis()
        val ends = maxOf(now + 5_000, r.endsAt + deltaSec * 1000L)
        saveRest(RestState(ends, (r.totalSec + deltaSec).coerceAtLeast(5)))
        if (ends - now > 10_000) warned = false
        exactAlarm = RestAlarm.schedule(app, ends - now)
    }

    fun skipRest() {
        cancelRest()
        recordFlash = false
    }

    /** Chamado pela tela a cada fração de segundo enquanto o descanso aparece. */
    fun onRestTick(now: Long) {
        val r = rest ?: return
        if (store.rest == null) { rest = null; recordFlash = false; return }   // o alarme já tratou
        val left = r.endsAt - now
        if (left in 1..10_000 && !warned && r.totalSec > 20) { warned = true; app.buzz(Buzz.WARN) }
        if (left <= 0) {
            // tela ligada: vibra daqui e desarma o alarme (senão vibraria duas vezes)
            RestAlarm.cancel(app)
            app.buzz(Buzz.REST_DONE)
            saveRest(null)
            recordFlash = false
        }
    }

    private fun checkRestFromAlarm() {
        val r = rest ?: return
        if (store.rest == null || r.endsAt <= System.currentTimeMillis() - 60_000) {
            saveRest(null)
            recordFlash = false
            RestAlarm.cancel(app)
        }
    }

    private fun cancelRest() {
        RestAlarm.cancel(app)
        saveRest(null)
    }

    private fun saveRest(r: RestState?) {
        rest = r
        store.rest = r
    }

    // ───────────── batimentos ─────────────

    fun onPermissionsAnswered() {
        store.askedPermissions = true
        updateHeartRate()
    }

    val shouldAskPermissions: Boolean get() = !store.askedPermissions

    /** FC máxima para as zonas: a que o celular calculou pela idade, ou a padrão. */
    val maxHr: Int get() = routines?.maxHeartRate?.takeIf { it > 0 } ?: DEFAULT_MAX_HR

    /** Zona do batimento agora (null sem batimento). */
    val heartZone: HrZone? get() = heartRate?.let { HrZone.of(it, maxHr) }

    private fun updateHeartRate() {
        val on = heartRateOn && (session != null || intervalRun?.done == false) && resumed && !ambient && app.hasHeartRatePermission()
        if (on) heart.start(::onHeartRate) else heart.stop()
        if (!on) heartRate = null
    }

    private fun onHeartRate(bpm: Int) {
        heartRate = bpm
        val s = session ?: return
        session = s.withHeartRate(bpm, maxHr)
        if (++hrSamples % 20 == 0) store.session = session   // grava de vez em quando (chega ~1 por segundo)
    }

    // ───────────── timer de intervalos ─────────────

    /** Agora (ms), atualizado pelo laço do timer: a tela do timer recompõe por ele. */
    var intervalNow by mutableLongStateOf(System.currentTimeMillis()); private set
    private var intervalJob: Job? = null

    fun startInterval(plan: IntervalPlan) {
        intervalRun = IntervalRun(plan.normalized(), System.currentTimeMillis())
        runIntervalLoop()
        updateHeartRate()
    }

    /**
     * O laço do timer fica aqui (e não na tela) para continuar vibrando quando a pessoa sai da tela do timer
     * e vai para o início ou para outro lugar do app. Com o app fechado ele para (sem alarme).
     */
    private fun runIntervalLoop() {
        intervalJob?.cancel()
        intervalJob = scope.launch {
            var last: IntervalState? = null
            var lastSecond = -1L
            while (true) {
                val r = intervalRun ?: break
                val now = System.currentTimeMillis()
                intervalNow = now
                val st = r.plan.stateAt(r.elapsed(now))
                if (!r.paused) {
                    val prev = last
                    if (prev != null && !st.samePhaseAs(prev)) {
                        when (st.phase) {
                            IntervalPhase.WORK -> app.buzz(Buzz.GO)
                            IntervalPhase.REST -> app.buzz(Buzz.PAUSE)
                            IntervalPhase.DONE -> app.buzz(Buzz.REST_DONE)
                            IntervalPhase.PREP -> Unit
                        }
                    } else {
                        // 3, 2, 1 antes de começar o esforço
                        val sec = (st.remainingMs + 999) / 1000
                        val beforeWork = st.phase == IntervalPhase.PREP || st.phase == IntervalPhase.REST
                        if (beforeWork && sec in 1..3 && sec != lastSecond) app.buzz(Buzz.TICK)
                        lastSecond = sec
                    }
                }
                last = st
                if (st.phase == IntervalPhase.DONE) { onIntervalDone(); break }
                delay(200)
            }
        }
    }

    fun toggleIntervalPause() {
        val r = intervalRun ?: return
        val now = System.currentTimeMillis()
        intervalRun = if (r.pausedElapsed != null) r.copy(startedAt = now - r.pausedElapsed, pausedElapsed = null)
        else r.copy(pausedElapsed = now - r.startedAt)
    }

    /** O timer chegou ao fim: congela no fim e desliga os batimentos (se não houver treino). */
    fun onIntervalDone() {
        val r = intervalRun ?: return
        if (r.done) return
        intervalRun = r.copy(pausedElapsed = r.plan.totalMs)
        updateHeartRate()
    }

    fun stopInterval() {
        intervalJob?.cancel()
        intervalJob = null
        intervalRun = null
        updateHeartRate()
    }

    fun saveIntervalPlan(plan: IntervalPlan) {
        intervalPlan = plan.normalized()
        store.intervalPlan = intervalPlan
    }

    // ───────────── ajustes ─────────────

    fun setAccentIndex(i: Int) { accent = i; store.accent = i }
    fun setRestDefault(sec: Int) { restSeconds = sec; store.restSeconds = sec }
    fun setKgStepValue(v: Float) { kgStep = v; store.kgStep = v }
    fun setAutoRestOn(on: Boolean) { autoRest = on; store.autoRest = on }
    fun setHeartRateEnabled(on: Boolean) { heartRateOn = on; store.heartRateOn = on; updateHeartRate() }
}
