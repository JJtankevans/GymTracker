package com.vitoroliveira.gymtracker.presentation.schedule

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Rótulos dos dias, na ordem ISO (1 = segunda ... 7 = domingo). */
val WEEKDAY_LABELS = listOf("Segunda", "Terça", "Quarta", "Quinta", "Sexta", "Sábado", "Domingo")

/** Agenda da semana: dia ISO (1..7) -> id do grupo. Dia ausente = descanso / livre. Fica no aparelho. */
interface ScheduleStore {
    fun load(): Map<Int, String>
    fun save(plan: Map<Int, String>)
}

class InMemoryScheduleStore(private var plan: Map<Int, String> = emptyMap()) : ScheduleStore {
    override fun load() = plan
    override fun save(plan: Map<Int, String>) { this.plan = plan }
}

class ScheduleRepository(private val store: ScheduleStore) {
    private val _plan = MutableStateFlow(store.load().filterKeys { it in 1..7 })
    val plan: StateFlow<Map<Int, String>> = _plan.asStateFlow()

    /** [groupId] null = descanso naquele dia. */
    fun set(day: Int, groupId: String?) {
        require(day in 1..7) { "dia inválido: $day" }
        _plan.update { if (groupId == null) it - day else it + (day to groupId) }
        store.save(_plan.value)
    }
}

/** Grupo planejado para o dia [isoDay] (1..7), só se ele ainda existe (grupo apagado = nada planejado). */
fun plannedGroupFor(plan: Map<Int, String>, isoDay: Int, existingGroupIds: Collection<String?>): String? =
    plan[isoDay]?.takeIf { it in existingGroupIds }

class ScheduleViewModel(private val repo: ScheduleRepository) : ViewModel() {
    val plan: StateFlow<Map<Int, String>> = repo.plan
    fun set(day: Int, groupId: String?) = repo.set(day, groupId)
}
