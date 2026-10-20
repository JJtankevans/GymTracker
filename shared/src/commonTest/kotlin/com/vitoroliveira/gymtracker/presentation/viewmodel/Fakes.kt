package com.vitoroliveira.gymtracker.presentation.viewmodel

import com.vitoroliveira.gymtracker.data.model.Exercise
import com.vitoroliveira.gymtracker.data.model.ExerciseBaseline
import com.vitoroliveira.gymtracker.data.model.ExerciseProgression
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
        exercises.filter { it.muscleGroupId == muscleGroupId }
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
    override suspend fun deleteWorkout(workoutId: String) { activeWorkout = null }
    override suspend fun getActiveWorkout(): Workout? { maybeFail(); return activeWorkout }
    override suspend fun updateFinishedExercises(workoutId: String, finishedExerciseIds: List<String>) {}
    override suspend fun getHistory(): List<Workout> = emptyList()

    override suspend fun addSet(workoutSet: WorkoutSet): WorkoutSet {
        maybeFail()
        return workoutSet.copy(id = "s${nextId++}").also { sets += it }
    }

    override suspend fun getSetsForWorkout(workoutId: String) = sets.toList()
    override suspend fun getProgressionForExercise(exerciseId: String): List<ExerciseProgression> = emptyList()

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
