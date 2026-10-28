package com.vitoroliveira.gymtracker.watch

import com.vitoroliveira.gymtracker.data.model.Exercise
import com.vitoroliveira.gymtracker.data.model.MuscleGroup
import com.vitoroliveira.gymtracker.data.model.RecentSet
import com.vitoroliveira.gymtracker.data.model.RecordSummary
import com.vitoroliveira.gymtracker.presentation.DashboardCalculator
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.time.Instant

/**
 * Conversa entre o celular e o relógio pela Data Layer do Wear OS (play-services-wearable).
 * O celular é o "hub": só ele fala com o Supabase. O relógio recebe as rotinas e devolve os treinos feitos.
 *
 * - [ROUTINES]: DataItem que o celular publica (grupos, exercícios e última carga de cada um).
 * - [REQUEST_ROUTINES]: mensagem do relógio pedindo rotinas atualizadas (ao abrir o app ou no "Sincronizar").
 * - [WORKOUT_PREFIX]`<id>`: DataItem que o relógio publica ao terminar um treino. O celular grava no Supabase
 *   e apaga o item; o item sumir é a confirmação de que o treino foi salvo (sobrevive a celular longe/desligado).
 * - [WORKOUT_SAVED]: mensagem do celular com o id salvo (aviso imediato; a confirmação de verdade é o item sumir).
 */
object WatchPaths {
    const val ROUTINES = "/gymtracker/rotinas"
    const val REQUEST_ROUTINES = "/gymtracker/pedir-rotinas"
    const val WORKOUT_PREFIX = "/gymtracker/treino/"
    const val WORKOUT_SAVED = "/gymtracker/treino-salvo"
    /** Chave do JSON dentro do DataMap. */
    const val KEY_JSON = "json"
}

@Serializable
data class WatchExercise(
    val id: String,
    val name: String,
    val sets: Int = 3,
    val reps: Int = 12,
    /** Maior carga da última vez que o exercício foi feito (null = nunca feito). */
    val lastKg: Double? = null,
    /** Repetições da série com [lastKg] na última vez. */
    val lastReps: Int? = null,
    /** Recorde de carga (null = sem recorde). */
    val bestKg: Double? = null
)

@Serializable
data class WatchRoutine(
    /** null = "Todos os exercícios" (treino sem grupo). */
    val groupId: String?,
    val name: String,
    val colorHex: String = "#C6FF00",
    val exercises: List<WatchExercise>
)

@Serializable
data class WatchRoutines(
    /** false = o celular está sem conta logada (o relógio mostra "Entre no app do celular"). */
    val loggedIn: Boolean = true,
    /** Quando o celular montou as rotinas (epoch ms). */
    val generatedAt: Long = 0L,
    val routines: List<WatchRoutine> = emptyList(),
    /** Treinos nesta semana (segunda a domingo) e a meta semanal do perfil (0 = sem meta conhecida). */
    val weekDone: Int = 0,
    val weekGoal: Int = 0,
    /** Dias seguidos com treino (como na Home do celular). */
    val streakDays: Int = 0,
    /** Grupo sugerido para hoje (rotação ABC, o mesmo "Sugerido" do celular). */
    val suggestedGroupId: String? = null
)

@Serializable
data class WatchSet(
    /** Vazio no treino rápido (sem exercício): essas séries não vão para o celular. */
    val exerciseId: String,
    val setNumber: Int,
    val reps: Int,
    val weightKg: Double,
    /** Epoch ms. */
    val completedAt: Long
)

/** Treino feito no relógio, mandado ao celular. O [id] (UUID gerado no relógio) vira o id do treino no Supabase. */
@Serializable
data class WatchWorkout(
    val id: String,
    val groupId: String?,
    val title: String,
    val startedAt: Long,
    val finishedAt: Long,
    val sets: List<WatchSet>,
    val avgHeartRate: Int? = null,
    val maxHeartRate: Int? = null
)

object WatchJson {
    val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    fun encodeRoutines(r: WatchRoutines): String = json.encodeToString(WatchRoutines.serializer(), r)
    fun decodeRoutines(s: String): WatchRoutines? = runCatching { json.decodeFromString(WatchRoutines.serializer(), s) }.getOrNull()
    fun encodeWorkout(w: WatchWorkout): String = json.encodeToString(WatchWorkout.serializer(), w)
    fun decodeWorkout(s: String): WatchWorkout? = runCatching { json.decodeFromString(WatchWorkout.serializer(), s) }.getOrNull()
    fun encodeSession(s: WatchSession): String = json.encodeToString(WatchSession.serializer(), s)
    fun decodeSession(s: String): WatchSession? = runCatching { json.decodeFromString(WatchSession.serializer(), s) }.getOrNull()
    fun encodeOutbox(o: WatchOutbox): String = json.encodeToString(WatchOutbox.serializer(), o)
    fun decodeOutbox(s: String): WatchOutbox? = runCatching { json.decodeFromString(WatchOutbox.serializer(), s) }.getOrNull()
}

/** Treinos terminados no relógio que o celular ainda não confirmou. [posted] = já entregue à Data Layer. */
@Serializable
data class WatchOutbox(val items: List<WatchOutboxItem> = emptyList()) {
    fun add(w: WatchWorkout): WatchOutbox = copy(items = items.filterNot { it.workout.id == w.id } + WatchOutboxItem(w))
    fun markPosted(id: String): WatchOutbox = copy(items = items.map { if (it.workout.id == id) it.copy(posted = true) else it })
    fun remove(id: String): WatchOutbox = copy(items = items.filterNot { it.workout.id == id })
}

@Serializable
data class WatchOutboxItem(val workout: WatchWorkout, val posted: Boolean = false)

/**
 * Monta as rotinas que vão para o relógio: um item por grupo (com os exercícios do grupo) e, no fim,
 * "Todos os exercícios". [recentSets] são as séries recentes (com a data do treino) e dão a última carga;
 * [records] dão o recorde.
 */
fun buildWatchRoutines(
    groups: List<MuscleGroup>,
    exercises: List<Exercise>,
    recentSets: List<RecentSet>,
    records: List<RecordSummary>,
    nowMs: Long,
    /** Dias (no fuso do usuário) dos treinos terminados, para a semana e a sequência. */
    workoutDates: List<LocalDate> = emptyList(),
    today: LocalDate? = null,
    weeklyGoal: Int = 0,
    suggestedGroupId: String? = null
): WatchRoutines {
    val best = records.associate { it.exerciseId to it.bestWeightKg }
    val last = lastLoads(recentSets)
    fun Exercise.toWatch(): WatchExercise? {
        val id = id ?: return null
        val l = last[id]
        return WatchExercise(
            id = id, name = name,
            sets = defaultSets.coerceIn(1, 20), reps = defaultReps.coerceIn(1, 99),
            lastKg = l?.first, lastReps = l?.second,
            bestKg = best[id]?.takeIf { it > 0.0 }
        )
    }
    val routines = groups.filter { it.id != null }.sortedBy { it.name.lowercase() }.mapNotNull { g ->
        val list = exercises.filter { g.id in it.groupIds }.mapNotNull { it.toWatch() }
        if (list.isEmpty()) null else WatchRoutine(g.id, g.name, g.colorHex, list)
    }
    val all = exercises.sortedBy { it.name.lowercase() }.mapNotNull { it.toWatch() }
    val withAll = if (all.isEmpty()) routines else routines + WatchRoutine(null, "Todos os exercícios", "#9E9E9E", all)
    // a sugerida vai primeiro na lista do relógio
    val ordered = suggestedGroupId?.let { id -> withAll.sortedByDescending { it.groupId == id } } ?: withAll
    return WatchRoutines(
        loggedIn = true, generatedAt = nowMs, routines = ordered,
        weekDone = today?.let { DashboardCalculator.weeklyCount(workoutDates, it) } ?: 0,
        weekGoal = weeklyGoal.coerceAtLeast(0),
        streakDays = today?.let { DashboardCalculator.streak(workoutDates, it) } ?: 0,
        suggestedGroupId = suggestedGroupId?.takeIf { id -> ordered.any { it.groupId == id } }
    )
}

/** Para cada exercício: (maior carga, repetições dessa série) no treino mais recente em que ele apareceu. */
internal fun lastLoads(recentSets: List<RecentSet>): Map<String, Pair<Double, Int>> =
    recentSets.groupBy { it.exerciseId }.mapValues { (_, sets) ->
        val latestDay = sets.mapNotNull { it.workouts?.startedAt }.maxOrNull()
        val ofLatest = if (latestDay == null) sets else sets.filter { it.workouts?.startedAt == latestDay }
        val top = ofLatest.maxWith(compareBy<RecentSet> { it.weightKg }.thenBy { it.reps })
        top.weightKg to top.reps
    }

/** Epoch ms → ISO 8601 (o formato que o Supabase grava em `timestamptz`). */
fun epochMsToIso(ms: Long): String = Instant.fromEpochMilliseconds(ms).toString()
