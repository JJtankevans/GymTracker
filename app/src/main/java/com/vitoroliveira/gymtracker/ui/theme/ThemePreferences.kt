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

    private val _musicReactive = MutableStateFlow(prefs.getBoolean(KEY_MUSIC, true))
    /** Quando ligado, as cores do tema acompanham a capa do álbum tocando. */
    val musicReactive: StateFlow<Boolean> = _musicReactive.asStateFlow()

    fun setMusicReactive(on: Boolean) {
        prefs.edit().putBoolean(KEY_MUSIC, on).apply()
        _musicReactive.value = on
    }

    private val _intensity = MutableStateFlow(
        runCatching { Intensity.valueOf(prefs.getString(KEY_INTENSITY, null) ?: "NORMAL") }.getOrDefault(Intensity.NORMAL)
    )
    /** Calmo / Normal / Insano — vale para todos os temas. */
    val intensity: StateFlow<Intensity> = _intensity.asStateFlow()

    fun setIntensity(i: Intensity) {
        prefs.edit().putString(KEY_INTENSITY, i.name).apply()
        _intensity.value = i
    }

    fun select(id: String) {
        val valid = AppThemes.byId(id).id
        prefs.edit().putString(KEY_THEME, valid).apply()
        _themeId.value = valid
    }

    private companion object {
        const val KEY_THEME = "theme_id"
        const val KEY_MUSIC = "music_reactive"
        const val KEY_INTENSITY = "intensity"
    }
}
