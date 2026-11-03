package com.vitoroliveira.gymtracker.watch

import com.vitoroliveira.gymtracker.presentation.body.estimateOneRepMax
import kotlinx.serialization.Serializable
import kotlin.math.roundToInt

/** Id do exercício do treino rápido (sem rotina): as séries ficam só no relógio. */
const val QUICK_EXERCISE_ID = ""

/**
 * Treino em andamento no relógio. Regras puras (sem Android) para ficarem testadas no :shared;
 * o relógio guarda o estado em JSON e só desenha.
 */
@Serializable
data class WatchSession(
    val id: String,
    val groupId: String?,
    val title: String,
    val startedAt: Long,
    val exercises: List<WatchExercise>,
    val sets: List<WatchSet> = emptyList(),
    /** Exercício em foco (índice em [exercises]). */
    val currentIndex: Int = 0,
    val hrSum: Long = 0,
    val hrCount: Int = 0,
    val hrMax: Int = 0,
    /** Recordes batidos neste treino. */
    val records: Int = 0,
    /** Amostras de batimento por zona (índice = [HrZone.ordinal]; ~1 por segundo). */
    val hrZones: List<Int> = emptyList()
) {
    val isQuick: Boolean get() = exercises.all { it.id == QUICK_EXERCISE_ID }
    val current: WatchExercise get() = exercises[currentIndex.coerceIn(0, exercises.lastIndex)]
    val totalSets: Int get() = sets.size
    val volumeKg: Double get() = sets.sumOf { it.reps * it.weightKg }
    val avgHeartRate: Int? get() = if (hrCount == 0) null else (hrSum.toDouble() / hrCount).roundToInt()
    val maxHeartRate: Int? get() = hrMax.takeIf { it > 0 }

    fun setsDone(exerciseId: String): Int = sets.count { it.exerciseId == exerciseId }
    fun isDone(ex: WatchExercise): Boolean = !isQuick && setsDone(ex.id) >= ex.sets
    val doneExercises: Int get() = exercises.count { isDone(it) }
    val allDone: Boolean get() = !isQuick && exercises.all { isDone(it) }

    /** Repetições e carga sugeridas para a próxima série: a série anterior deste treino, senão a última vez. */
    fun suggestion(ex: WatchExercise): Pair<Int, Double> {
        val prev = sets.lastOrNull { it.exerciseId == ex.id }
        if (prev != null) return prev.reps to prev.weightKg
        return (ex.lastReps ?: ex.reps) to (ex.lastKg ?: 0.0)
    }

    /** Maior carga já registrada do exercício: recorde do histórico ou deste treino. */
    fun bestKg(ex: WatchExercise): Double? {
        val here = sets.filter { it.exerciseId == ex.id }.maxOfOrNull { it.weightKg }
        return listOfNotNull(ex.bestKg, here).maxOrNull()
    }

    fun select(index: Int): WatchSession = copy(currentIndex = index.coerceIn(0, exercises.lastIndex))

    /** Põe um exercício fora do grupo no treino (ou vai até ele, se já está) e deixa o foco nele. */
    fun addExercise(ex: WatchExercise): WatchSession {
        if (isQuick) return this
        val existing = exercises.indexOfFirst { it.id == ex.id }
        if (existing >= 0) return copy(currentIndex = existing)
        return copy(exercises = exercises + ex, currentIndex = exercises.size)
    }

    /** Registra uma série do exercício em foco. Passa sozinho ao próximo exercício quando as séries planejadas acabam. */
    fun addSet(reps: Int, weightKg: Double, nowMs: Long): AddSetResult {
        val ex = current
        val previousBest = bestKg(ex)
        val set = WatchSet(ex.id, setsDone(ex.id) + 1, reps.coerceIn(1, 999), weightKg.coerceIn(0.0, 1000.0), nowMs)
        // recorde só conta quando já havia uma referência (a primeira vez de um exercício não é "recorde")
        val record = !isQuick && previousBest != null && previousBest > 0.0 && set.weightKg > previousBest
        var next = copy(sets = sets + set, records = records + if (record) 1 else 0)
        val finishedExercise = next.isDone(ex)
        if (finishedExercise) next = next.copy(currentIndex = next.nextPendingIndex(currentIndex))
        return AddSetResult(next, record, finishedExercise)
    }

    /** Desfaz a última série (e volta o foco para o exercício dela). */
    fun undoLastSet(): WatchSession {
        val last = sets.lastOrNull() ?: return this
        val idx = exercises.indexOfFirst { it.id == last.exerciseId }.takeIf { it >= 0 } ?: currentIndex
        return copy(sets = sets.dropLast(1), currentIndex = idx)
    }

    /** Próximo exercício que ainda tem séries a fazer, procurando depois de [from] e dando a volta; [from] se todos acabaram. */
    fun nextPendingIndex(from: Int): Int {
        if (exercises.isEmpty()) return 0
        for (step in 1..exercises.size) {
            val i = (from + step) % exercises.size
            if (!isDone(exercises[i])) return i
        }
        return from
    }

    /** Melhor série do treino pelo 1RM estimado (Epley, até 12 repetições): exercício e 1RM. null no treino rápido/sem carga. */
    fun bestLift(): Pair<WatchExercise, Double>? {
        if (isQuick) return null
        val byId = exercises.associateBy { it.id }
        return sets.filter { it.weightKg > 0.0 && it.exerciseId in byId }
            .map { byId.getValue(it.exerciseId) to estimateOneRepMax(it.weightKg, it.reps.coerceAtMost(12)) }
            .maxByOrNull { it.second }
    }

    fun withHeartRate(bpm: Int, maxHr: Int = DEFAULT_MAX_HR): WatchSession =
        if (bpm !in 30..240) this
        else copy(
            hrSum = hrSum + bpm, hrCount = hrCount + 1, hrMax = maxOf(hrMax, bpm),
            hrZones = hrZones.addZoneSample(HrZone.of(bpm, maxHr))
        )

    /** O que vai para o celular; null se não há série de exercício de verdade (treino rápido ou vazio). */
    fun toWorkout(finishedAt: Long): WatchWorkout? {
        val real = sets.filter { it.exerciseId != QUICK_EXERCISE_ID }
        if (real.isEmpty()) return null
        return WatchWorkout(id, groupId, title, startedAt, maxOf(finishedAt, startedAt), real, avgHeartRate, maxHeartRate)
    }

    companion object {
        fun start(id: String, routine: WatchRoutine, nowMs: Long): WatchSession =
            WatchSession(id, routine.groupId, routine.name, nowMs, routine.exercises)

        fun quick(id: String, reps: Int, kg: Double, nowMs: Long): WatchSession =
            WatchSession(
                id, null, "Treino rápido", nowMs,
                listOf(WatchExercise(QUICK_EXERCISE_ID, "Treino rápido", sets = 99, reps = reps, lastKg = kg, lastReps = reps))
            )
    }
}

data class AddSetResult(val session: WatchSession, val newRecord: Boolean, val finishedExercise: Boolean)

/** "40", "42,5" — carga sem zeros inúteis, com vírgula. */
fun formatKg(kg: Double): String {
    val r = (kg * 10).roundToInt() / 10.0
    return if (r % 1.0 == 0.0) r.toInt().toString() else r.toString().replace('.', ',')
}

/** "850 kg" ou "3,3 t". */
fun formatVolume(kg: Double): String =
    if (kg >= 1000) formatKg((kg / 100).roundToInt() / 10.0) + " t" else "${kg.roundToInt()} kg"

/**
 * Dica de progressão: se da última vez fez todas as repetições planejadas, sugere subir a carga
 * (+2,5 kg até 40 kg, +5 kg acima disso). null quando não há o que sugerir.
 */
fun progressionHint(ex: WatchExercise): Double? {
    val kg = ex.lastKg ?: return null
    val reps = ex.lastReps ?: return null
    if (kg <= 0.0 || reps < ex.reps) return null
    return kg + if (kg <= 40.0) 2.5 else 5.0
}
