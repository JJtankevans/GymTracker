package com.vitoroliveira.gymtracker.ui.workout

import android.media.AudioManager
import android.media.ToneGenerator
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vitoroliveira.gymtracker.presentation.workout.liveSchedule
import com.vitoroliveira.gymtracker.presentation.workout.phaseAt
import com.vitoroliveira.gymtracker.presentation.workout.scheduleSeconds
import kotlinx.coroutines.delay

private const val LEAD_SECONDS = 3.0

/**
 * Série cronometrada (porte do LiveTimer da web): conta as fases da cadência (descer, pausa, subir, pausa) com bip a cada
 * mudança e um bip duplo no fim. O usuário confere e toca em "Registrar".
 */
@Composable
fun LiveSetTimer(tempo: List<Int>, reps: Int) {
    val schedule = remember(tempo, reps) { liveSchedule(tempo, reps) }
    if (schedule.isEmpty()) return
    val total = scheduleSeconds(schedule)
    var running by remember { mutableStateOf(false) }
    var finished by remember { mutableStateOf(false) }
    var elapsed by remember { mutableDoubleStateOf(-LEAD_SECONDS) }
    var lastIndex by remember { mutableIntStateOf(-2) }
    val tone = remember { runCatching { ToneGenerator(AudioManager.STREAM_MUSIC, 70) }.getOrNull() }
    DisposableEffect(Unit) { onDispose { tone?.release() } }

    LaunchedEffect(running) {
        if (!running) return@LaunchedEffect
        val t0 = System.nanoTime()
        while (running) {
            elapsed = (System.nanoTime() - t0) / 1e9 - LEAD_SECONDS
            val pos = if (elapsed >= 0) phaseAt(schedule, elapsed) else null
            if (pos != null && pos.index != lastIndex) { lastIndex = pos.index; tone?.startTone(ToneGenerator.TONE_PROP_BEEP, 90) }
            if (elapsed >= total) { tone?.startTone(ToneGenerator.TONE_PROP_BEEP2, 300); finished = true; running = false }
            delay(100)
        }
    }

    Column(Modifier.fillMaxWidth().padding(top = 4.dp), verticalArrangement = Arrangement.spacedBy(4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        if (!running) {
            OutlinedButton(
                onClick = { lastIndex = -2; finished = false; elapsed = -LEAD_SECONDS; running = true },
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (finished) "Série completa! Cronometrar de novo" else "Série cronometrada ($reps reps · ${total}s sob tensão)") }
        } else {
            val pos = if (elapsed >= 0) phaseAt(schedule, elapsed) else null
            if (elapsed < 0) {
                Text("Prepare-se", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                Text("${kotlin.math.ceil(-elapsed).toInt()}", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
            } else if (pos != null) {
                Text("Repetição ${pos.phase.rep} de $reps", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                Text(pos.phase.label, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("${pos.left}", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
            }
            TextButton(onClick = { running = false }) { Text("Parar") }
        }
    }
}
