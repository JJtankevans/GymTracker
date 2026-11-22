package com.vitoroliveira.gymtracker.ui.workout

import android.content.Context
import com.vitoroliveira.gymtracker.presentation.workout.CardioEntry
import com.vitoroliveira.gymtracker.presentation.workout.SetFlag
import com.vitoroliveira.gymtracker.presentation.workout.TrainingPhase
import com.vitoroliveira.gymtracker.presentation.workout.decodeCardio
import com.vitoroliveira.gymtracker.presentation.workout.encodeCardio

/** Marca da série (aquecimento, drop-set, rest-pause, falha), guardada no aparelho; mesmo desenho do [RpeStore]. */
object FlagStore {
    private const val PREFS = "gymtracker_setflags"

    fun get(context: Context, setId: String?): SetFlag? =
        if (setId == null) null
        else context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(setId, null)
            ?.let { name -> SetFlag.entries.firstOrNull { it.name == name } }

    fun put(context: Context, setId: String?, flag: SetFlag) {
        if (setId == null) return
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(setId, flag.name).apply()
    }
}

/** Fase do treino (Volume, Força, Definição): ajusta a meta de repetições da sugestão de carga. */
object PhaseStore {
    private const val PREFS = "gymtracker_prefs"
    private const val KEY = "training_phase"

    fun get(context: Context): TrainingPhase =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null)
            ?.let { name -> TrainingPhase.entries.firstOrNull { it.name == name } } ?: TrainingPhase.AUTO

    fun put(context: Context, phase: TrainingPhase) {
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, phase.name).apply()
    }
}

/** Cardio registrado (só no aparelho). */
object CardioStore {
    private const val PREFS = "gymtracker_cardio"
    private const val KEY = "entries"

    fun load(context: Context): List<CardioEntry> =
        decodeCardio(context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, "").orEmpty())

    fun save(context: Context, entries: List<CardioEntry>) {
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, encodeCardio(entries)).apply()
    }
}
