// shared/src/commonMain/kotlin/com/vitoroliveira/gymtracker/data/repository/ExerciseRepositoryImpl.kt

package com.vitoroliveira.gymtracker.data.repository

import com.vitoroliveira.gymtracker.data.model.Exercise
import com.vitoroliveira.gymtracker.data.model.GroupExerciseLink
import com.vitoroliveira.gymtracker.domain.repository.ExerciseRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest

/**
 * Um exercício pode estar em vários grupos: os vínculos ficam em `group_exercises` (migração 006)
 * e voltam em [Exercise.groupIds]. `insert` e `update` também gravam os vínculos.
 */
class ExerciseRepositoryImpl(
    private val supabase: SupabaseClient
) : ExerciseRepository {

    override suspend fun getAll(): List<Exercise> {
        val rows = supabase.postgrest["exercises"]
            .select()
            .decodeList<Exercise>()
        val groupsByExercise = supabase.postgrest["group_exercises"]
            .select()
            .decodeList<GroupExerciseLink>()
            .groupBy({ it.exerciseId }, { it.groupId })
        return rows.map { it.copy(groupIds = groupsByExercise[it.id].orEmpty()) }
    }

    override suspend fun getByMuscleGroup(muscleGroupId: String): List<Exercise> =
        getAll().filter { muscleGroupId in it.groupIds }

    override suspend fun insert(exercise: Exercise): Exercise {
        val userId = supabase.auth.currentUserOrNull()?.id
            ?: throw IllegalStateException("Usuário não autenticado")

        val saved = supabase.postgrest["exercises"]
            .insert(exercise.copy(profileId = userId)) {
                select()
            }
            .decodeSingle<Exercise>()
        val id = requireNotNull(saved.id) { "O exercício criado veio sem id" }
        addLinks(id, exercise.groupIds)
        return saved.copy(groupIds = exercise.groupIds)
    }

    override suspend fun update(exercise: Exercise) {
        val id = exercise.id!!
        supabase.postgrest["exercises"]
            .update(exercise) {
                filter { eq("id", id) }
            }
        syncLinks(id, exercise.groupIds)
    }

    override suspend fun delete(id: String) {
        // os vínculos saem sozinhos (ON DELETE CASCADE)
        supabase.postgrest["exercises"]
            .delete {
                filter { eq("id", id) }
            }
    }

    /** Deixa o exercício exatamente nos grupos de [wanted]: apaga os que saíram e cria os novos. */
    private suspend fun syncLinks(exerciseId: String, wanted: List<String>) {
        val current = supabase.postgrest["group_exercises"]
            .select { filter { eq("exercise_id", exerciseId) } }
            .decodeList<GroupExerciseLink>()
            .map { it.groupId }
            .toSet()
        val target = wanted.toSet()
        (current - target).forEach { groupId ->
            supabase.postgrest["group_exercises"].delete {
                filter {
                    eq("exercise_id", exerciseId)
                    eq("group_id", groupId)
                }
            }
        }
        addLinks(exerciseId, (target - current).toList())
    }

    private suspend fun addLinks(exerciseId: String, groupIds: List<String>) {
        if (groupIds.isEmpty()) return
        // sem select(): INSERT ... RETURNING dependeria da política de leitura (ver bug de RLS em CONTEXTO_SESSAO.md)
        supabase.postgrest["group_exercises"]
            .insert(groupIds.distinct().map { GroupExerciseLink(groupId = it, exerciseId = exerciseId) })
    }
}
