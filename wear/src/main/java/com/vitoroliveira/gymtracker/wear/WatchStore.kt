package com.vitoroliveira.gymtracker.wear

import android.content.Context

/** Estado simples do treino no relógio, guardado localmente (sobrevive ao app fechar). */
class WatchStore(context: Context) {
    private val p = context.getSharedPreferences("watch", Context.MODE_PRIVATE)

    var accent: Int
        get() = p.getInt("accent", 0)
        set(v) = p.edit().putInt("accent", v).apply()

    var sets: Int
        get() = p.getInt("sets", 0)
        set(v) = p.edit().putInt("sets", v).apply()

    var volume: Float
        get() = p.getFloat("volume", 0f)
        set(v) = p.edit().putFloat("volume", v).apply()

    /** Início do treino em ms de relógio de parede; 0 = ainda não começou. */
    var startedAt: Long
        get() = p.getLong("startedAt", 0L)
        set(v) = p.edit().putLong("startedAt", v).apply()

    var reps: Int
        get() = p.getInt("reps", 10)
        set(v) = p.edit().putInt("reps", v).apply()

    var kg: Float
        get() = p.getFloat("kg", 40f)
        set(v) = p.edit().putFloat("kg", v).apply()

    var restSeconds: Int
        get() = p.getInt("rest", 90)
        set(v) = p.edit().putInt("rest", v).apply()

    fun resetSession() {
        p.edit().putInt("sets", 0).putFloat("volume", 0f).putLong("startedAt", 0L).apply()
    }
}
