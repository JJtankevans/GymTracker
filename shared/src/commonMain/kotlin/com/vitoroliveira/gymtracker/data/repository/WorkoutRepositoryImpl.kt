// shared/src/commonMain/kotlin/com/vitoroliveira/gymtracker/data/repository/WorkoutRepositoryImpl.kt

package com.vitoroliveira.gymtracker.data.repository

import com.vitoroliveira.gymtracker.data.model.ExerciseBaseline
import com.vitoroliveira.gymtracker.data.model.ExerciseProgression
import com.vitoroliveira.gymtracker.data.model.RecordSummary
import com.vitoroliveira.gymtracker.data.model.RecentSet
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

    override suspend fun createWorkout(workoutId: String, muscleGroupId: String?, startedAtIso: String) {
        val userId = supabase.auth.currentUserOrNull()?.id
            ?: throw IllegalStateException("Usuário não autenticado")
        supabase.postgrest["workouts"]
            .insert(Workout(id = workoutId, profileId = userId, muscleGroupId = muscleGroupId, startedAt = startedAtIso))
    }

    override suspend fun finishWorkoutAt(workoutId: String, finishedAtIso: String) {
        supabase.postgrest["workouts"]
            .update({ set("finished_at", finishedAtIso) }) {
                filter { eq("id", workoutId) }
            }
    }

    override suspend fun addSets(sets: List<WorkoutSet>) {
        if (sets.isEmpty()) return
        supabase.postgrest["workout_sets"].insert(sets)
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
        // o Supabase devolve no máximo 1000 linhas por chamada: busca em páginas (erro nº 16)
        val page = 1000L
        val all = mutableListOf<Workout>()
        var from = 0L
        while (true) {
            val chunk = supabase.postgrest["workouts"]
                .select {
                    order("started_at", Order.DESCENDING)
                    order("id", Order.ASCENDING)
                    filter {
                        filterNot("finished_at", FilterOperator.IS, null)
                    }
                    range(from, from + page - 1)
                }
                .decodeList<Workout>()
            all += chunk
            if (chunk.size < page) break
            from += page
        }
        return all
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

    override suspend fun getSetsSince(sinceIso: String): List<RecentSet> =
        supabase.postgrest["workout_sets"]
            .select(Columns.raw("exercise_id, reps, weight_kg, workouts!inner(started_at)")) {
                filter { gte("workouts.started_at", sinceIso) }
            }
            .decodeList()

    override suspend fun getAllSetsForExport(): List<com.vitoroliveira.gymtracker.data.model.ExportSet> {
        // o Supabase devolve no máximo 1000 linhas por chamada: busca em páginas até acabar
        val page = 1000L
        val all = mutableListOf<com.vitoroliveira.gymtracker.data.model.ExportSet>()
        var from = 0L
        while (true) {
            val chunk = supabase.postgrest["workout_sets"]
                .select(Columns.raw("exercise_id, set_number, reps, weight_kg, completed_at, workouts!inner(id, started_at, muscle_group_id, muscle_group_name)")) {
                    filter { filterNot("workouts.finished_at", FilterOperator.IS, null) }
                    order("completed_at", Order.ASCENDING)
                    order("id", Order.ASCENDING)          // desempate: sem ele, páginas podem repetir/pular séries
                    range(from, from + page - 1)
                }
                .decodeList<com.vitoroliveira.gymtracker.data.model.ExportSet>()
            all += chunk
            if (chunk.size < page) break
            from += page
        }
        return all
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
