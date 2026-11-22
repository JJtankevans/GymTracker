package com.vitoroliveira.gymtracker.ui.components

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * Vibração do treino (#365): confirmação curta ao registrar a série e um padrão duplo ao bater recorde.
 * O fim do descanso continua em `restFinishedFeedback`. O sistema já respeita "não perturbe" e a configuração
 * de vibração do aparelho; aqui só há a chave do app (Perfil → Aplicativo → Vibração).
 */
object Haptics {
    private const val PREFS = "gymtracker_prefs"
    private const val KEY = "haptics"

    fun enabled(context: Context): Boolean =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY, true)

    fun setEnabled(context: Context, on: Boolean) {
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY, on).apply()
    }

    /** Toque seco e curto: "a série entrou". */
    fun setLogged(context: Context) = play(context, VibrationEffect.createOneShot(28, VibrationEffect.DEFAULT_AMPLITUDE))

    /** Dois toques com pausa: "recorde!". */
    fun record(context: Context) =
        play(context, VibrationEffect.createWaveform(longArrayOf(0, 35, 70, 55), -1))

    private fun play(context: Context, effect: VibrationEffect) {
        if (!enabled(context)) return
        runCatching { vibrator(context)?.takeIf { it.hasVibrator() }?.vibrate(effect) }
    }

    private fun vibrator(context: Context): Vibrator? =
        if (Build.VERSION.SDK_INT >= 31) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
}
