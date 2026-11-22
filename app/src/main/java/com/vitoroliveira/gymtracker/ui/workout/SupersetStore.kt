package com.vitoroliveira.gymtracker.ui.workout

import android.content.Context

/**
 * Supersérie (#575) e cadência (#581) guardadas no aparelho (o banco ainda não tem as colunas: migração 017, não aplicada).
 * Mesmo desenho do [RpeStore].
 */
object SupersetStore {
    private const val PREFS = "gymtracker_superset"

    /** Ids dos exercícios ligados ao próximo, neste treino. */
    fun load(context: Context, workoutId: String?): Set<String> =
        if (workoutId == null) emptySet()
        else prefs(context).getStringSet("w_$workoutId", emptySet())?.toSet().orEmpty()

    fun save(context: Context, workoutId: String?, links: Set<String>) {
        if (workoutId == null) return
        prefs(context).edit().putStringSet("w_$workoutId", links).apply()
    }

    private fun prefs(context: Context) = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}

object TempoStore {
    private const val PREFS = "gymtracker_tempo"

    /** Cadência (ex.: "3-1-2-0") gravada para uma série, ou null. */
    fun getForSet(context: Context, setId: String?): String? =
        if (setId == null) null else prefs(context).getString("s_$setId", null)

    fun putForSet(context: Context, setId: String?, text: String) {
        if (setId == null) return
        prefs(context).edit().putString("s_$setId", text).apply()
    }

    /** Cadência padrão do exercício (a última que o usuário deixou preenchida). */
    fun getDefault(context: Context, exerciseId: String?): String =
        if (exerciseId == null) "" else prefs(context).getString("e_$exerciseId", "").orEmpty()

    fun putDefault(context: Context, exerciseId: String?, text: String) {
        if (exerciseId == null) return
        prefs(context).edit().apply { if (text.isBlank()) remove("e_$exerciseId") else putString("e_$exerciseId", text.trim()) }.apply()
    }

    private fun prefs(context: Context) = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
