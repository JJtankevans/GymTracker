package com.vitoroliveira.gymtracker.wear

import android.content.Context
import com.vitoroliveira.gymtracker.watch.WatchJson
import com.vitoroliveira.gymtracker.watch.WatchOutbox
import com.vitoroliveira.gymtracker.watch.WatchRoutines
import com.vitoroliveira.gymtracker.watch.WatchSession

/** Descanso em andamento. [endsAt] em ms de relógio de parede (sobrevive ao app fechar). */
data class RestState(val endsAt: Long, val totalSec: Int)

/** Tudo que o relógio guarda (sobrevive ao app fechar e ao relógio reiniciar). */
class WatchStore(context: Context) {
    private val p = context.getSharedPreferences("watch", Context.MODE_PRIVATE)

    var accent: Int
        get() = p.getInt("accent", 0)
        set(v) = p.edit().putInt("accent", v).apply()

    /** Descanso padrão, em segundos. */
    var restSeconds: Int
        get() = p.getInt("rest", 90)
        set(v) = p.edit().putInt("rest", v).apply()

    /** Passo da carga na coroa e nos botões. */
    var kgStep: Float
        get() = p.getFloat("kgStep", 2.5f)
        set(v) = p.edit().putFloat("kgStep", v).apply()

    /** Começar o descanso sozinho ao registrar a série. */
    var autoRest: Boolean
        get() = p.getBoolean("autoRest", true)
        set(v) = p.edit().putBoolean("autoRest", v).apply()

    var heartRateOn: Boolean
        get() = p.getBoolean("hr", true)
        set(v) = p.edit().putBoolean("hr", v).apply()

    /** Já pedimos as permissões uma vez (não insistir a cada treino). */
    var askedPermissions: Boolean
        get() = p.getBoolean("askedPerms", false)
        set(v) = p.edit().putBoolean("askedPerms", v).apply()

    var routines: WatchRoutines?
        get() = p.getString("routines", null)?.let(WatchJson::decodeRoutines)
        set(v) = p.edit().putString("routines", v?.let(WatchJson::encodeRoutines)).apply()

    /** Quando as rotinas chegaram pela última vez (epoch ms; 0 = nunca). */
    var routinesReceivedAt: Long
        get() = p.getLong("routinesAt", 0L)
        set(v) = p.edit().putLong("routinesAt", v).apply()

    var session: WatchSession?
        get() = p.getString("session", null)?.let(WatchJson::decodeSession)
        set(v) = p.edit().putString("session", v?.let(WatchJson::encodeSession)).apply()

    var outbox: WatchOutbox
        get() = p.getString("outbox", null)?.let(WatchJson::decodeOutbox) ?: WatchOutbox()
        set(v) = p.edit().putString("outbox", WatchJson.encodeOutbox(v)).apply()

    var rest: RestState?
        get() = p.getLong("restEnds", 0L).takeIf { it > 0 }?.let { RestState(it, p.getInt("restTotal", 90)) }
        set(v) = p.edit().putLong("restEnds", v?.endsAt ?: 0L).putInt("restTotal", v?.totalSec ?: 0).apply()

    /** Valores do treino rápido (sem rotina). */
    var quickReps: Int
        get() = p.getInt("reps", 10)
        set(v) = p.edit().putInt("reps", v).apply()

    var quickKg: Float
        get() = p.getFloat("kg", 20f)
        set(v) = p.edit().putFloat("kg", v).apply()
}
