package com.vitoroliveira.gymtracker.presentation.body

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Fonte única dos dados corporais (um só objeto no app, para todas as telas verem o mesmo valor). */
class BodyRepository(private val store: BodyStore) {
    private val _data = MutableStateFlow(store.load())
    val data: StateFlow<BodyData> = _data.asStateFlow()

    private fun change(f: (BodyData) -> BodyData) {
        _data.update(f)
        store.save(_data.value)
    }

    fun setProfile(p: BodyProfile) = change { it.copy(profile = p) }

    /** Um registro por dia: se já existe, substitui. Valores fora de 20–400 kg são ignorados. */
    fun logWeight(epochDay: Int, kg: Double) {
        if (kg !in 20.0..400.0) return
        change { d -> d.copy(weights = (d.weights.filter { it.epochDay != epochDay } + BodyWeightEntry(epochDay, kg)).sortedBy { it.epochDay }) }
    }

    fun deleteWeight(epochDay: Int) = change { d -> d.copy(weights = d.weights.filter { it.epochDay != epochDay }) }
}

class BodyViewModel(private val repo: BodyRepository) : ViewModel() {
    val data: StateFlow<BodyData> = repo.data
    fun setProfile(p: BodyProfile) = repo.setProfile(p)
    fun logWeight(epochDay: Int, kg: Double) = repo.logWeight(epochDay, kg)
    fun deleteWeight(epochDay: Int) = repo.deleteWeight(epochDay)
}
