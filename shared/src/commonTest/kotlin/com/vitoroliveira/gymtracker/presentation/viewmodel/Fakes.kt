package com.vitoroliveira.gymtracker.presentation.viewmodel

import com.vitoroliveira.gymtracker.data.model.Exercise
import com.vitoroliveira.gymtracker.data.model.ExerciseBaseline
import com.vitoroliveira.gymtracker.data.model.ExerciseProgression
import com.vitoroliveira.gymtracker.data.model.RecordSummary
import com.vitoroliveira.gymtracker.data.model.Workout
import com.vitoroliveira.gymtracker.data.model.WorkoutSet
import com.vitoroliveira.gymtracker.domain.repository.ExerciseRepository
import com.vitoroliveira.gymtracker.domain.repository.WorkoutRepository
import kotlin.time.Clock
import kotlin.time.Instant

class FixedClock(private var instant: Instant) : Clock {
    override fun now(): Instant = instant
    fun set(i: Instant) { instant = i }
}

class FakeExerciseRepository(
    var exercises: List<Exercise> = emptyList()
) : ExerciseRepository {
    override suspend fun getAll() = exercises
    override suspend fun getByMuscleGroup(muscleGroupId: String) =
        exercises.filter { muscleGroupId in it.groupIds }
    override suspend fun insert(exercise: Exercise) = exercise
    override suspend fun update(exercise: Exercise) {}
    override suspend fun delete(id: String) {}
}

class FakeWorkoutRepository : WorkoutRepository {
    var activeWorkout: Workout? = null
    var failWith: Throwable? = null
    var baselines: Map<String, ExerciseBaseline> = emptyMap()

    var startCalls = 0
    var finished = false
    val sets = mutableListOf<WorkoutSet>()
    private var nextId = 1

    private fun maybeFail() { failWith?.let { throw it } }

    override suspend fun startWorkout(muscleGroupId: String?): Workout {
        maybeFail()
        startCalls++
        return Workout(id = "w1", muscleGroupId = muscleGroupId, startedAt = "2026-10-20T10:00:00Z")
            .also { activeWorkout = it }
    }

    override suspend fun finishWorkout(workoutId: String) { maybeFail(); finished = true }
    override suspend fun createWorkout(workoutId: String, muscleGroupId: String?, startedAtIso: String) {
        maybeFail()
        activeWorkout = Workout(id = workoutId, muscleGroupId = muscleGroupId, startedAt = startedAtIso)
    }
    var finishedAt: String? = null
    override suspend fun finishWorkoutAt(workoutId: String, finishedAtIso: String) {
        maybeFail()
        finishedAt = finishedAtIso
        activeWorkout?.takeIf { it.id == workoutId }?.let { history += it.copy(finishedAt = finishedAtIso); activeWorkout = null }
    }
    override suspend fun addSets(sets: List<WorkoutSet>) {
        maybeFail()
        sets.forEach { this.sets += it.copy(id = "s${nextId++}") }
    }
    var failDelete: Throwable? = null
    val history = mutableListOf<Workout>()
    override suspend fun deleteWorkout(workoutId: String) {
        failDelete?.let { throw it }
        activeWorkout = null
        history.removeAll { it.id == workoutId }
    }
    override suspend fun getActiveWorkout(): Workout? { maybeFail(); return activeWorkout }
    override suspend fun updateFinishedExercises(workoutId: String, finishedExerciseIds: List<String>) {}
    override suspend fun getHistory(): List<Workout> = history.toList()
    var failNotes: Throwable? = null
    override suspend fun getWorkout(workoutId: String): Workout? =
        (history + listOfNotNull(activeWorkout)).firstOrNull { it.id == workoutId }
    override suspend fun updateNotes(workoutId: String, notes: String?) {
        failNotes?.let { throw it }
        val clean = notes?.trim()?.takeIf { it.isNotEmpty() }
        history.replaceAll { if (it.id == workoutId) it.copy(notes = clean) else it }
    }

    /** Quando definido, a gravação da série espera por ele (simula a rede lenta). */
    var addSetGate: kotlinx.coroutines.CompletableDeferred<Unit>? = null

    override suspend fun addSet(workoutSet: WorkoutSet): WorkoutSet {
        addSetGate?.await()
        maybeFail()
        return workoutSet.copy(id = "s${nextId++}").also { sets += it }
    }

    override suspend fun getSetsForWorkout(workoutId: String) = sets.toList()
    var progression: Map<String, List<ExerciseProgression>> = emptyMap()
    var progressionCalls = 0
    override suspend fun getProgressionForExercise(exerciseId: String): List<ExerciseProgression> {
        progressionCalls++
        return progression[exerciseId].orEmpty()
    }

    /** null = a função `records_summary()` não existe no banco (a chamada falha, como no Supabase sem a migração 008). */
    var recentSets: List<com.vitoroliveira.gymtracker.data.model.RecentSet> = emptyList()
    override suspend fun getSetsSince(sinceIso: String) = recentSets
    var exportSets: List<com.vitoroliveira.gymtracker.data.model.ExportSet> = emptyList()
    override suspend fun getAllSetsForExport() = exportSets

    var recordsSummary: List<RecordSummary>? = null
    var summaryCalls = 0
    override suspend fun getRecordsSummary(): List<RecordSummary> {
        summaryCalls++
        return recordsSummary ?: throw UnsupportedOperationException("records_summary indisponível")
    }

    override suspend fun updateSet(setId: String, reps: Int, weightKg: Double): WorkoutSet {
        maybeFail()
        val i = sets.indexOfFirst { it.id == setId }
        sets[i] = sets[i].copy(reps = reps, weightKg = weightKg)
        return sets[i]
    }

    override suspend fun deleteSet(setId: String) { maybeFail(); sets.removeAll { it.id == setId } }

    override suspend fun getExerciseBaseline(exerciseId: String, excludeWorkoutId: String) =
        baselines[exerciseId] ?: ExerciseBaseline()
}
