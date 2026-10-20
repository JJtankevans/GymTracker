package com.vitoroliveira.gymtracker.ui.exercise

import android.content.Context
import com.vitoroliveira.gymtracker.data.catalog.ExerciseCatalog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Favoritos e exercícios recentes do catálogo, guardados só neste aparelho. */
class CatalogPrefs(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("gymtracker_prefs", Context.MODE_PRIVATE)

    private val _favorites = MutableStateFlow(prefs.getStringSet(KEY_FAVS, emptySet()).orEmpty().toSet())
    val favorites: StateFlow<Set<String>> = _favorites.asStateFlow()

    private val _recent = MutableStateFlow(
        prefs.getString(KEY_RECENT, "").orEmpty().split(SEP).filter { it.isNotBlank() }
    )
    val recent: StateFlow<List<String>> = _recent.asStateFlow()

    fun toggleFavorite(name: String) {
        val next = _favorites.value.let { if (name in it) it - name else it + name }
        // copia: o Android não garante a gravação se mexermos no mesmo Set devolvido por getStringSet
        prefs.edit().putStringSet(KEY_FAVS, HashSet(next)).apply()
        _favorites.value = next
    }

    fun addRecent(names: List<String>) {
        val next = ExerciseCatalog.pushRecent(_recent.value, names)
        prefs.edit().putString(KEY_RECENT, next.joinToString(SEP)).apply()
        _recent.value = next
    }

    private companion object {
        const val KEY_FAVS = "catalog_favorites"
        const val KEY_RECENT = "catalog_recent"
        const val SEP = "|"
    }
}
