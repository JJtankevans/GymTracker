package com.vitoroliveira.gymtracker.ui.records

import android.content.Context
import com.vitoroliveira.gymtracker.presentation.records.decodeGoals
import com.vitoroliveira.gymtracker.presentation.records.encodeGoals
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Metas de carga por exercício (id → kg), guardadas só neste aparelho. Apagadas ao sair da conta. */
class LoadGoalPrefs(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("gymtracker_prefs", Context.MODE_PRIVATE)

    private val _goals = MutableStateFlow(decodeGoals(prefs.getString(KEY, null)))
    val goals: StateFlow<Map<String, Double>> = _goals.asStateFlow()

    /** [kg] null apaga a meta. */
    fun set(exerciseId: String, kg: Double?) {
        val next = if (kg == null) _goals.value - exerciseId else _goals.value + (exerciseId to kg)
        prefs.edit().putString(KEY, encodeGoals(next)).apply()
        _goals.value = next
    }

    fun clear() {
        prefs.edit().remove(KEY).apply()
        _goals.value = emptyMap()
    }

    private companion object {
        const val KEY = "load_goals"
    }
}
