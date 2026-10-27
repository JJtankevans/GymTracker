package com.vitoroliveira.gymtracker.wear

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.vitoroliveira.gymtracker.watch.WatchOutbox
import com.vitoroliveira.gymtracker.watch.WatchRoutine
import com.vitoroliveira.gymtracker.watch.WatchRoutines
import com.vitoroliveira.gymtracker.watch.WatchSession
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
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
    val workoutId: String?
)

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

    fun setAmbient(on: Boolean) {
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
                setOutbox(outbox.markPosted(id))
            } else if (!sync.isQueued(id)) {
                setOutbox(outbox.remove(id))     // o celular gravou e apagou o item
            }
        }
    }

    private fun onSaved(workoutId: String) = setOutbox(outbox.remove(workoutId))

    private fun setOutbox(o: WatchOutbox) {
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
        setSession(s)
        loadDraft()
        OngoingWorkout.show(app, s.title, s.startedAt)
        updateHeartRate()
    }

    fun select(index: Int) {
        val s = session ?: return
        setSession(s.select(index))
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
        setSession(result.session)
        if (s.isQuick) { store.quickReps = draftReps; store.quickKg = draftKg.toFloat() }
        app.buzz(if (result.newRecord) Buzz.RECORD else Buzz.TAP)
        recordFlash = result.newRecord
        loadDraft()
        if (autoRest && !result.session.allDone) startRest(restSeconds)
    }

    fun undoLastSet() {
        val s = session ?: return
        setSession(s.undoLastSet())
        loadDraft()
    }

    /** Termina: guarda o resumo, põe o treino na fila do celular e limpa o treino em andamento. */
    fun finish() {
        val s = session ?: return
        val now = System.currentTimeMillis()
        val workout = s.toWorkout(now)
        lastFinish = FinishSummary(
            s.title, now - s.startedAt, s.totalSets, s.volumeKg,
            s.doneExercises, if (s.isQuick) 0 else s.exercises.size,
            s.avgHeartRate, s.maxHeartRate, s.records, workout?.id
        )
        if (workout != null) setOutbox(outbox.add(workout))
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
        setSession(null)
        OngoingWorkout.hide(app)
        heartRate = null
        updateHeartRate()
    }

    private fun setSession(s: WatchSession?) {
        session = s
        store.session = s
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
        setRest(r)
        warned = false
        exactAlarm = RestAlarm.schedule(app, seconds * 1000L)
    }

    /** Soma/tira segundos do descanso em andamento (mínimo de 5 s restantes). */
    fun addRest(deltaSec: Int) {
        val r = rest ?: return
        val now = System.currentTimeMillis()
        val ends = maxOf(now + 5_000, r.endsAt + deltaSec * 1000L)
        setRest(RestState(ends, (r.totalSec + deltaSec).coerceAtLeast(5)))
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
            setRest(null)
            recordFlash = false
        }
    }

    private fun checkRestFromAlarm() {
        val r = rest ?: return
        if (store.rest == null || r.endsAt <= System.currentTimeMillis() - 60_000) {
            setRest(null)
            recordFlash = false
            RestAlarm.cancel(app)
        }
    }

    private fun cancelRest() {
        RestAlarm.cancel(app)
        setRest(null)
    }

    private fun setRest(r: RestState?) {
        rest = r
        store.rest = r
    }

    // ───────────── batimentos ─────────────

    fun onPermissionsAnswered() {
        store.askedPermissions = true
        updateHeartRate()
    }

    val shouldAskPermissions: Boolean get() = !store.askedPermissions

    private fun updateHeartRate() {
        val on = heartRateOn && session != null && resumed && !ambient && app.hasHeartRatePermission()
        if (on) heart.start(::onHeartRate) else heart.stop()
        if (!on) heartRate = null
    }

    private fun onHeartRate(bpm: Int) {
        heartRate = bpm
        val s = session ?: return
        session = s.withHeartRate(bpm)
        if (++hrSamples % 20 == 0) store.session = session   // grava de vez em quando (chega ~1 por segundo)
    }

    // ───────────── ajustes ─────────────

    fun setAccentIndex(i: Int) { accent = i; store.accent = i }
    fun setRestDefault(sec: Int) { restSeconds = sec; store.restSeconds = sec }
    fun setKgStepValue(v: Float) { kgStep = v; store.kgStep = v }
    fun setAutoRestOn(on: Boolean) { autoRest = on; store.autoRest = on }
    fun setHeartRateEnabled(on: Boolean) { heartRateOn = on; store.heartRateOn = on; updateHeartRate() }
}
