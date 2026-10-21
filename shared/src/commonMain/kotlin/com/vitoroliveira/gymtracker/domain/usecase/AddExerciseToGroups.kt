package com.vitoroliveira.gymtracker.domain.usecase

import com.vitoroliveira.gymtracker.data.catalog.ExerciseCatalog
import com.vitoroliveira.gymtracker.data.model.Exercise
import com.vitoroliveira.gymtracker.domain.repository.ExerciseRepository

sealed interface AddToGroupsResult {
    val exercise: Exercise
    /** Não existia: foi criado já nos grupos pedidos. */
    data class Created(override val exercise: Exercise) : AddToGroupsResult
    /** Já existia (em outro grupo ou em nenhum): só ganhou os grupos que faltavam; o histórico é o mesmo. */
    data class Linked(override val exercise: Exercise) : AddToGroupsResult
    /** Já existia e já estava em todos os grupos pedidos: nada mudou. */
    data class Unchanged(override val exercise: Exercise) : AddToGroupsResult
}

/**
 * Põe o exercício nos grupos [groupIds]. O nome é único por usuário (ignorando caixa e acento), então se já
 * existe um exercício com esse nome ele é reaproveitado em vez de duplicado: assim "Cadeira extensora" pode estar
 * em vários grupos e o histórico/recordes dele continuam juntos. Sem grupos, só garante que ele existe.
 *
 * [known] evita buscar a lista a cada item ao adicionar vários seguidos; é atualizada com o resultado.
 */
suspend fun ExerciseRepository.addToGroups(
    exercise: Exercise,
    groupIds: Set<String>,
    known: MutableList<Exercise>? = null
): AddToGroupsResult {
    val all = known ?: getAll().toMutableList()
    val existing = all.firstOrNull { ExerciseCatalog.isSameName(it.name, exercise.name) }

    if (existing == null) {
        val created = insert(exercise.copy(groupIds = groupIds.toList()))
        all += created
        return AddToGroupsResult.Created(created)
    }

    val missing = groupIds - existing.groupIds.toSet()
    if (missing.isEmpty()) return AddToGroupsResult.Unchanged(existing)

    val linked = existing.copy(groupIds = existing.groupIds + missing)
    update(linked)
    all[all.indexOfFirst { it.id == existing.id }] = linked
    return AddToGroupsResult.Linked(linked)
}
