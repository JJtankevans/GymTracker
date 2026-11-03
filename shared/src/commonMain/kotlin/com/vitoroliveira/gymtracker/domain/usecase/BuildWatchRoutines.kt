package com.vitoroliveira.gymtracker.domain.usecase

import com.vitoroliveira.gymtracker.domain.repository.AuthRepository
import com.vitoroliveira.gymtracker.domain.repository.ExerciseRepository
import com.vitoroliveira.gymtracker.domain.repository.MuscleGroupRepository
import com.vitoroliveira.gymtracker.domain.repository.ProfileRepository
import com.vitoroliveira.gymtracker.domain.repository.WorkoutRepository
import com.vitoroliveira.gymtracker.presentation.workout.suggestNextGroup
import com.vitoroliveira.gymtracker.domain.resolveGroupName
import com.vitoroliveira.gymtracker.watch.WatchRecent
import com.vitoroliveira.gymtracker.watch.WatchRoutines
import com.vitoroliveira.gymtracker.watch.buildWatchRoutines
import com.vitoroliveira.gymtracker.watch.epochMsToIso
import com.vitoroliveira.gymtracker.watch.estimatedMaxHr
import kotlinx.coroutines.CancellationException
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.todayIn
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * Monta o que o celular manda para o relógio: grupos, exercícios com a última carga e o recorde,
 * a semana (treinos x meta), a sequência de dias e o grupo sugerido. Só grupos/exercícios são obrigatórios;
 * o resto, se falhar, só não aparece no relógio.
 */
class BuildWatchRoutines(
    private val auth: AuthRepository,
    private val groups: MuscleGroupRepository,
    private val exercises: ExerciseRepository,
    private val workouts: WorkoutRepository,
    private val profiles: ProfileRepository,
    private val clock: Clock = Clock.System,
    private val zone: TimeZone = TimeZone.currentSystemDefault(),
    /** Ano de nascimento de "Meus dados" (fica no aparelho), para a FC máxima das zonas no relógio. */
    private val birthYear: () -> Int? = { null }
) {
    suspend operator fun invoke(): WatchRoutines {
        val now = clock.now()
        val nowMs = now.toEpochMilliseconds()
        if (!auth.awaitSession()) return WatchRoutines(loggedIn = false, generatedAt = nowMs)
        val gs = groups.getAll()
        val ex = exercises.getAll()
        // 45 dias: o Supabase devolve no máximo 1000 séries por chamada
        val recent = optional { workouts.getSetsSince(epochMsToIso(nowMs - 45L * 24 * 60 * 60 * 1000)) }.orEmpty()
        val records = optional { workouts.getRecordsSummary() }.orEmpty()
        val history = optional { workouts.getHistory() }.orEmpty()
        val goal = optional { profiles.getCurrentProfile().weeklyGoal } ?: 0
        val dates = history.mapNotNull { w ->
            w.startedAt?.let { s -> runCatching { Instant.parse(s).toLocalDateTime(zone).date }.getOrNull() }
        }
        val groupsById = gs.associateBy { it.id }
        val lastWorkouts = history.take(5).mapNotNull { w ->
            val start = w.startedAt?.let { runCatching { Instant.parse(it) }.getOrNull() } ?: return@mapNotNull null
            val end = w.finishedAt?.let { runCatching { Instant.parse(it) }.getOrNull() }
            val d = start.toLocalDateTime(zone).date
            WatchRecent(
                day = "${d.dayOfMonth.toString().padStart(2, '0')}/${d.monthNumber.toString().padStart(2, '0')}",
                name = w.resolveGroupName(groupsById) ?: "Treino livre",
                minutes = end?.let { (it - start).inWholeMinutes.toInt() }
            )
        }
        return buildWatchRoutines(
            gs, ex, recent, records, nowMs,
            workoutDates = dates, today = clock.todayIn(zone), weeklyGoal = goal,
            suggestedGroupId = suggestNextGroup(gs, history),
            recent = lastWorkouts,
            maxHeartRate = optional { birthYear() }?.let { estimatedMaxHr(clock.todayIn(zone).year - it) } ?: 0
        )
    }

    private suspend fun <T> optional(block: suspend () -> T): T? = try {
        block()
    } catch (e: Exception) {
        if (e is CancellationException) throw e
        null
    }
}
