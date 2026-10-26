// shared/src/commonMain/kotlin/com/vitoroliveira/gymtracker/data/repository/WorkoutRepositoryImpl.kt

package com.vitoroliveira.gymtracker.data.repository

import com.vitoroliveira.gymtracker.data.model.ExerciseBaseline
import com.vitoroliveira.gymtracker.data.model.ExerciseProgression
import com.vitoroliveira.gymtracker.data.model.RecordSummary
import com.vitoroliveira.gymtracker.data.model.Workout
import com.vitoroliveira.gymtracker.data.model.WorkoutSet
import com.vitoroliveira.gymtracker.domain.repository.WorkoutRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import io.github.jan.supabase.postgrest.query.filter.FilterOperator

@Serializable
private data class SetRow(
    @SerialName("weight_kg") val weightKg: Double = 0.0,
    val reps: Int = 0
)

class WorkoutRepositoryImpl(
    private val supabase: SupabaseClient
) : WorkoutRepository {

    override suspend fun startWorkout(muscleGroupId: String?): Workout {
        val userId = supabase.auth.currentUserOrNull()?.id
            ?: throw IllegalStateException("Usuário não autenticado")

        return supabase.postgrest["workouts"]
            .insert(Workout(profileId = userId, muscleGroupId = muscleGroupId)){
                select()
            }
            .decodeSingle()
    }

    override suspend fun finishWorkout(workoutId: String) {
        supabase.postgrest["workouts"]
            .update({ set("finished_at", "now()") }) {
                filter { eq("id", workoutId) }
            }
    }

    override suspend fun deleteWorkout(workoutId: String) {
        supabase.postgrest["workouts"]
            .delete {
                filter { eq("id", workoutId) }
            }
    }

    override suspend fun getActiveWorkout(): Workout? {
        val userId = supabase.auth.currentUserOrNull()?.id
            ?: throw IllegalStateException("Usuário não autenticado")

        return supabase.postgrest["workouts"]
            .select {
                filter {
                    eq("profile_id", userId)
                    filter("finished_at", FilterOperator.IS, null)
                }
                order("started_at", Order.DESCENDING)
                limit(1)
            }
            .decodeSingleOrNull()
    }

    override suspend fun updateFinishedExercises(workoutId: String, finishedExerciseIds: List<String>) {
        supabase.postgrest["workouts"]
            .update({ set("finished_exercise_ids", finishedExerciseIds) }) {
                filter { eq("id", workoutId) }
            }
    }

    override suspend fun getHistory(): List<Workout> {
        return supabase.postgrest["workouts"]
            .select {
                order("started_at", Order.DESCENDING)
                filter {
                    filterNot("finished_at", FilterOperator.IS, null)
                }
            }
            .decodeList()
    }

    override suspend fun getWorkout(workoutId: String): Workout? =
        supabase.postgrest["workouts"]
            .select { filter { eq("id", workoutId) } }
            .decodeSingleOrNull()

    override suspend fun updateNotes(workoutId: String, notes: String?) {
        supabase.postgrest["workouts"]
            .update({ set<String?>("notes", notes?.trim()?.takeIf { it.isNotEmpty() }) }) {
                filter { eq("id", workoutId) }
            }
    }

    override suspend fun addSet(workoutSet: WorkoutSet): WorkoutSet {
        return supabase.postgrest["workout_sets"]
            .insert(workoutSet){
                select()
            }
            .decodeSingle()
    }

    override suspend fun getSetsForWorkout(workoutId: String): List<WorkoutSet> {
        return supabase.postgrest["workout_sets"]
            .select {
                filter { eq("workout_id", workoutId) }
                order("set_number", Order.ASCENDING)
            }
            .decodeList()
    }

    override suspend fun getProgressionForExercise(exerciseId: String): List<ExerciseProgression> {
        return supabase.postgrest["vw_exercise_progression"]
            .select {
                filter { eq("exercise_id", exerciseId) }
                order("session_date", Order.ASCENDING)
            }
            .decodeList()
    }

    override suspend fun getRecordsSummary(): List<RecordSummary> =
        supabase.postgrest.rpc("records_summary").decodeList()

    override suspend fun updateSet(setId: String, reps: Int, weightKg: Double): WorkoutSet {
        return supabase.postgrest["workout_sets"]
            .update({
                set("reps", reps)
                set("weight_kg", weightKg)
            }) {
                filter { eq("id", setId) }
                select()
            }
            .decodeSingle()
    }

    override suspend fun deleteSet(setId: String) {
        supabase.postgrest["workout_sets"]
            .delete { filter { eq("id", setId) } }
    }

    override suspend fun getExerciseBaseline(
        exerciseId: String,
        excludeWorkoutId: String
    ): ExerciseBaseline {
        val record = supabase.postgrest["workout_sets"]
            .select(Columns.list("weight_kg", "reps")) {
                filter {
                    eq("exercise_id", exerciseId)
                    neq("workout_id", excludeWorkoutId)
                }
                order("weight_kg", Order.DESCENDING)
                limit(1)
            }
            .decodeSingleOrNull<SetRow>()

        val last = supabase.postgrest["workout_sets"]
            .select(Columns.list("weight_kg", "reps")) {
                filter {
                    eq("exercise_id", exerciseId)
                    neq("workout_id", excludeWorkoutId)
                }
                order("completed_at", Order.DESCENDING)
                limit(1)
            }
            .decodeSingleOrNull<SetRow>()

        return ExerciseBaseline(
            lastWeightKg = last?.weightKg,
            lastReps = last?.reps,
            recordWeightKg = record?.weightKg
        )
    }
}
