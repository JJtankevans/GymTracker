package com.vitoroliveira.gymtracker.ui.circuit

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.tts.TextToSpeech
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

/** Avisos por voz, vibração e bipe, pra não precisar olhar o celular. */
class CircuitCues(private val context: Context) {
    private var ready = false
    private val tts: TextToSpeech = TextToSpeech(context.applicationContext) { status ->
        if (status == TextToSpeech.SUCCESS) {
            val r = ready(Locale("pt", "BR"))
            ready = r
        }
    }

    private fun ready(l: Locale): Boolean {
        val r = tts.setLanguage(l)
        return r != TextToSpeech.LANG_MISSING_DATA && r != TextToSpeech.LANG_NOT_SUPPORTED
    }

    var voiceEnabled = true

    fun speak(text: String) {
        if (voiceEnabled && ready) tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "cue")
    }

    private fun vibrator(): Vibrator =
        if (Build.VERSION.SDK_INT >= 31) (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        else @Suppress("DEPRECATION") context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

    fun buzz(vararg pattern: Long) {
        runCatching { vibrator().vibrate(VibrationEffect.createWaveform(longArrayOf(0, *pattern), -1)) }
    }

    fun beep() {
        runCatching {
            val g = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 80)
            g.startTone(ToneGenerator.TONE_PROP_BEEP2, 300)
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({ g.release() }, 500)
        }
    }

    fun shutdown() { tts.stop(); tts.shutdown() }
}

@Composable
fun rememberCircuitCues(): CircuitCues {
    val ctx = LocalContext.current
    val cues = remember { CircuitCues(ctx) }
    DisposableEffect(Unit) { onDispose { cues.shutdown() } }
    return cues
}
