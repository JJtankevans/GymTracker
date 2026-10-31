package com.vitoroliveira.gymtracker.ui.theme

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Guarda localmente qual tema o usuário escolheu. */
class ThemePreferences(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("gymtracker_prefs", Context.MODE_PRIVATE)

    private val _themeId = MutableStateFlow(
        AppThemes.byId(prefs.getString(KEY_THEME, null)).id
    )
    val themeId: StateFlow<String> = _themeId.asStateFlow()

    private val _intensity = MutableStateFlow(
        runCatching { Intensity.valueOf(prefs.getString(KEY_INTENSITY, null) ?: "NORMAL") }.getOrDefault(Intensity.NORMAL)
    )
    /** Calmo / Normal / Insano — vale para todos os temas. */
    val intensity: StateFlow<Intensity> = _intensity.asStateFlow()

    fun setIntensity(i: Intensity) {
        prefs.edit().putString(KEY_INTENSITY, i.name).apply()
        _intensity.value = i
    }

    private val _workoutStyle = MutableStateFlow(
        prefs.getString(KEY_WORKOUT_STYLE, null)?.let { name -> runCatching { WorkoutLayout.valueOf(name) }.getOrNull() }
    )
    /** Estilo da tela de treino ativo escolhido pela pessoa; null = segue o tema. */
    val workoutStyle: StateFlow<WorkoutLayout?> = _workoutStyle.asStateFlow()

    fun setWorkoutStyle(style: WorkoutLayout?) {
        prefs.edit().apply { if (style == null) remove(KEY_WORKOUT_STYLE) else putString(KEY_WORKOUT_STYLE, style.name) }.apply()
        _workoutStyle.value = style
    }

    fun select(id: String) {
        val valid = AppThemes.byId(id).id
        prefs.edit().putString(KEY_THEME, valid).apply()
        _themeId.value = valid
    }

    private companion object {
        const val KEY_THEME = "theme_id"
        const val KEY_INTENSITY = "intensity"
        const val KEY_WORKOUT_STYLE = "workout_style"
    }
}
