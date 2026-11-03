package com.vitoroliveira.gymtracker.domain.usecase

import com.vitoroliveira.gymtracker.data.model.Exercise
import com.vitoroliveira.gymtracker.data.model.MuscleGroup
import com.vitoroliveira.gymtracker.data.model.SharedRoutine
import com.vitoroliveira.gymtracker.domain.repository.ExerciseRepository
import com.vitoroliveira.gymtracker.domain.repository.MuscleGroupRepository

/** Limites do que entra de um treino de outra pessoa (o código é público): evita encher a conta de lixo. */
const val MAX_IMPORTED_EXERCISES = 60
private const val MAX_NAME_LENGTH = 80
private const val MAX_NOTES_LENGTH = 500

/** Resultado da importação: nome do grupo criado (pode ganhar " 2") e quantos exercícios entraram. */
data class ImportedRoutine(val groupName: String, val exercises: Int)

/**
 * Cria um grupo a partir de um treino compartilhado por um amigo. Exercícios que a pessoa já tem (mesmo nome, sem
 * caixa/acento) são reaproveitados com o histórico dela; os outros são criados com as séries/reps do amigo.
 * Se algo falhar no meio, o grupo novo é desfeito.
 */
class ImportSharedRoutine(
    private val groups: MuscleGroupRepository,
    private val exercises: ExerciseRepository
) {
    suspend operator fun invoke(routine: SharedRoutine, existingGroupNames: List<String>): ImportedRoutine {
        val name = uniqueGroupName(routine.name.trim().take(MAX_NAME_LENGTH), existingGroupNames)
        val color = routine.colorHex.takeIf { Regex("^#[0-9A-Fa-f]{6}$").matches(it) } ?: "#6200EE"
        val group = groups.insert(MuscleGroup(name = name, colorHex = color))
        val groupId = requireNotNull(group.id) { "O grupo criado veio sem id" }
        try {
            val known = exercises.getAll().toMutableList()
            val valid = routine.exercises.filter { it.name.isNotBlank() }.take(MAX_IMPORTED_EXERCISES)
            valid.forEach { ex ->
                exercises.addToGroups(
                    Exercise(
                        name = ex.name.trim().take(MAX_NAME_LENGTH),
                        defaultSets = ex.sets.coerceIn(1, 20),
                        defaultReps = ex.reps.coerceIn(1, 100),
                        notes = ex.notes?.takeIf { it.isNotBlank() }?.take(MAX_NOTES_LENGTH)
                    ),
                    setOf(groupId),
                    known
                )
            }
            return ImportedRoutine(name, valid.size)
        } catch (e: Exception) {
            runCatching { groups.delete(groupId) }
            throw e
        }
    }
}

/** "Pernas" já existe? Vira "Pernas 2", "Pernas 3"... (sem diferenciar maiúsculas). */
fun uniqueGroupName(base: String, existing: List<String>): String {
    val clean = base.trim().ifEmpty { "Treino" }
    val taken = existing.map { it.trim().lowercase() }.toSet()
    if (clean.lowercase() !in taken) return clean
    var n = 2
    while ("$clean $n".lowercase() in taken) n++
    return "$clean $n"
}
