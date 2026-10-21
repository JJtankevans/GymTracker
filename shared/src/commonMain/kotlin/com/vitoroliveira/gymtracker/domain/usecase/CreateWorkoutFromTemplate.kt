package com.vitoroliveira.gymtracker.domain.usecase

import com.vitoroliveira.gymtracker.data.catalog.WorkoutTemplate
import com.vitoroliveira.gymtracker.data.model.Exercise
import com.vitoroliveira.gymtracker.data.model.MuscleGroup
import com.vitoroliveira.gymtracker.domain.repository.ExerciseRepository
import com.vitoroliveira.gymtracker.domain.repository.MuscleGroupRepository

/**
 * Cria o grupo do treino pronto e põe nele os exercícios do catálogo: os que o usuário já tem (em outro grupo, ou
 * soltos depois de apagar um grupo) são reaproveitados, com o mesmo histórico; os outros são criados. Devolve quantos entraram.
 */
class CreateWorkoutFromTemplate(
    private val groups: MuscleGroupRepository,
    private val exercises: ExerciseRepository
) {
    suspend operator fun invoke(template: WorkoutTemplate, existingGroupNames: List<String>): Int {
        val group = groups.insert(MuscleGroup(name = uniqueName(template.name, existingGroupNames), colorHex = template.colorHex))
        val groupId = requireNotNull(group.id) { "O grupo criado veio sem id" }
        try {
            return insertAll(template, groupId)
        } catch (e: Exception) {
            // não deixa um grupo pela metade; se nem apagar der, o erro original é o que importa
            runCatching { groups.delete(groupId) }
            throw e
        }
    }

    private suspend fun insertAll(template: WorkoutTemplate, groupId: String): Int {
        val known = exercises.getAll().toMutableList()
        template.exercises.forEach { ex ->
            exercises.addToGroups(
                Exercise(name = ex.name, defaultSets = ex.sets, defaultReps = ex.reps, notes = ex.hint),
                setOf(groupId),
                known
            )
        }
        return template.exercises.size
    }

    /** "Push" já existe? Vira "Push 2", "Push 3"... */
    internal fun uniqueName(base: String, existing: List<String>): String {
        val taken = existing.map { it.trim().lowercase() }.toSet()
        if (base.lowercase() !in taken) return base
        var n = 2
        while ("$base $n".lowercase() in taken) n++
        return "$base $n"
    }
}
