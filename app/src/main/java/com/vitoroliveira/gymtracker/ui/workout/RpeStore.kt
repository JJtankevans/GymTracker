package com.vitoroliveira.gymtracker.ui.workout

import android.content.Context

/**
 * Esforço percebido por série (RPE de 6 a 10; 10 = falha), #350.
 *
 * Fica **só no aparelho** por enquanto: a coluna `rpe` em `workout_sets` ainda não existe no banco
 * (migração 014 em docs/MIGRACOES_PENDENTES.md). Quando existir, o app passa a enviar e este arquivo vira cache.
 * Chave = id da série; valor = 6..10 (0 = não informado).
 */
object RpeStore {
    private const val PREFS = "gymtracker_rpe"

    fun get(context: Context, setId: String?): Int =
        if (setId == null) 0 else context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(setId, 0)

    fun put(context: Context, setId: String?, rpe: Int) {
        if (setId == null) return
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putInt(setId, rpe).apply()
    }

    /** Rótulo curto para a escolha: "RPE 8 · 2 reps sobrando". */
    fun describe(rpe: Int): String = when (rpe) {
        6 -> "leve (4+ sobrando)"
        7 -> "moderada (3 sobrando)"
        8 -> "forte (2 sobrando)"
        9 -> "muito forte (1 sobrando)"
        10 -> "no limite (falha)"
        else -> ""
    }
}
