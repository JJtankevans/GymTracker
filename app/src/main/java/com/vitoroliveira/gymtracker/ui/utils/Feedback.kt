package com.vitoroliveira.gymtracker.ui.utils

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/** Vibra + toca um bipe curto. Usado quando o descanso entre séries acaba. */
fun Context.restFinishedFeedback() {
    try {
        val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
        vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 220, 120, 220, 120, 380), -1))
    } catch (_: Exception) { /* sem vibrador: tudo bem */ }

    try {
        val tone = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 80)
        tone.startTone(ToneGenerator.TONE_PROP_BEEP2, 450)
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({ tone.release() }, 900)
    } catch (_: Exception) { /* sem áudio: tudo bem */ }
}
