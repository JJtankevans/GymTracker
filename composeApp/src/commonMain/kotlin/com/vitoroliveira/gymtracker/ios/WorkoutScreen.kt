package com.vitoroliveira.gymtracker.ios

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vitoroliveira.gymtracker.data.model.Exercise
import com.vitoroliveira.gymtracker.presentation.viewmodel.WorkoutViewModel
import com.vitoroliveira.gymtracker.presentation.viewmodel.maxSetsFor
import com.vitoroliveira.gymtracker.watch.formatKg

/** Treino ativo no iPhone (mesmo WorkoutViewModel do Android): séries por exercício, descanso, fim com resumo. */
@Composable
fun WorkoutScreen(groupId: String?, groupName: String, onClose: () -> Unit) {
    val vm = remember { WorkoutViewModel(Services.workouts, Services.exercises) }
    val s by vm.uiState.collectAsState()
    val elapsed by vm.elapsedSeconds.collectAsState()
    var confirmFinish by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { vm.startWorkout(groupId) }

    Column(Modifier.fillMaxSize()) {
        // cabeçalho
        Row(Modifier.fillMaxWidth().padding(start = 8.dp, end = 20.dp, top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onClose) { Text("‹ Voltar") }
            Column(Modifier.weight(1f)) {
                Text(groupName, fontWeight = FontWeight.Black, fontSize = 20.sp)
                Text(clock(elapsed), color = Muted, fontSize = 13.sp)
            }
            Button(onClick = { confirmFinish = true }, enabled = s.activeWorkout != null && !s.isLoading) { Text("TERMINAR", fontWeight = FontWeight.Black) }
        }
        // descanso
        val t = s.timerState
        if (t.isRunning || t.isFinished) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(if (t.isFinished) "Descanso acabou" else "Descanso ${t.formattedTime}", fontWeight = FontWeight.Bold, color = if (t.isFinished) Gold else Lime)
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = { vm.cancelTimer() }) { Text(if (t.isFinished) "OK" else "Pular") }
                }
                LinearProgressIndicator(
                    progress = { t.progress }, modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                    color = Lime, trackColor = Lime.copy(alpha = 0.15f)
                )
            }
        }
        s.errorMessage?.let {
            Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 20.dp))
        }
        if (s.isLoading && s.activeWorkout == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (s.activeWorkout != null && s.exercises.isEmpty()) {
                    item { Text("Nenhum exercício neste grupo. Cadastre no app do Android por enquanto.", color = Muted) }
                }
                items(s.exercises, key = { it.id ?: it.name }) { ex ->
                    ExerciseCard(ex, vm)
                }
            }
        }
    }

    s.activeWorkoutToRecover?.let { old ->
        AlertDialog(
            onDismissRequest = onClose,
            title = { Text("Treino em andamento") },
            text = { Text("Você tem um treino aberto. Continuar com ele ou começar outro?") },
            confirmButton = { Button(onClick = { vm.resumeWorkout(old) }) { Text("Continuar") } },
            dismissButton = { TextButton(onClick = { vm.discardAndStartNew(groupId) }) { Text("Começar novo") } }
        )
    }
    if (confirmFinish) {
        AlertDialog(
            onDismissRequest = { confirmFinish = false },
            title = { Text("Terminar o treino?") },
            text = { Text("${s.completedSets.size} séries registradas.") },
            confirmButton = { Button(onClick = { confirmFinish = false; vm.finishWorkout() }) { Text("Terminar") } },
            dismissButton = { TextButton(onClick = { confirmFinish = false }) { Text("Continuar treinando") } }
        )
    }
    s.summary?.let { sum ->
        AlertDialog(
            onDismissRequest = onClose,
            title = { Text("Treino concluído!") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("${sum.durationMinutes} min · ${sum.totalSets} séries · ${sum.totalReps} repetições")
                    Text("Volume: ${formatKg(sum.totalVolumeKg)} kg")
                    if (sum.newRecords.isNotEmpty()) Text("Recorde em: ${sum.newRecords.joinToString()}", color = Gold, fontWeight = FontWeight.Bold)
                }
            },
            confirmButton = { Button(onClick = onClose) { Text("Concluir") } }
        )
    }
}

@Composable
private fun ExerciseCard(ex: Exercise, vm: WorkoutViewModel) {
    val s by vm.uiState.collectAsState()
    val id = ex.id ?: return
    val sets = s.completedSets.filter { it.exerciseId == id }
    val base = s.baselines[id]
    val last = sets.lastOrNull()
    var reps by remember(id) { mutableStateOf((last?.reps ?: base?.lastReps ?: ex.defaultReps).toString()) }
    var kg by remember(id) { mutableStateOf(formatKg(last?.weightKg ?: base?.lastWeightKg ?: 0.0)) }
    val saving = id in s.savingExerciseIds
    val record = id in s.newRecordExerciseIds

    Panel(highlight = record) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(ex.name, fontWeight = FontWeight.Bold, fontSize = 17.sp, modifier = Modifier.weight(1f))
            Text("${sets.size}/${ex.defaultSets}", color = if (sets.size >= ex.defaultSets) Lime else Muted, fontWeight = FontWeight.Bold)
        }
        val hint = buildString {
            base?.lastWeightKg?.let { append("Última: ${formatKg(it)} kg × ${base.lastReps ?: "-"}") }
            base?.recordWeightKg?.let { if (isNotEmpty()) append(" · "); append("Recorde: ${formatKg(it)} kg") }
        }
        if (hint.isNotEmpty()) Text(hint, color = Muted, fontSize = 12.sp)
        if (record) Text("Recorde novo!", color = Gold, fontWeight = FontWeight.Black, fontSize = 13.sp)
        if (sets.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            Text(sets.joinToString("   ") { "${formatKg(it.weightKg)}×${it.reps}" }, fontSize = 13.sp)
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                reps, { v -> reps = v.filter { it.isDigit() }.take(3) }, label = { Text("reps") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.width(84.dp)
            )
            OutlinedTextField(
                kg, { v -> kg = v.filter { it.isDigit() || it == ',' || it == '.' }.take(6) }, label = { Text("kg") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.width(96.dp)
            )
            Button(
                onClick = {
                    val r = reps.toIntOrNull() ?: return@Button
                    val w = kg.replace(',', '.').toDoubleOrNull() ?: 0.0
                    vm.addSet(id, r, w)
                    vm.startTimer(90)
                },
                enabled = !saving && sets.size < maxSetsFor(ex),
                modifier = Modifier.weight(1f).height(52.dp)
            ) { Text(if (saving) "..." else "REGISTRAR", fontWeight = FontWeight.Black, fontSize = 12.sp) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 6.dp)) {
            listOf(60, 90, 120).forEach { sec ->
                OutlinedButton(onClick = { vm.startTimer(sec) }, contentPadding = PaddingValues(horizontal = 10.dp)) { Text("${sec}s", fontSize = 12.sp) }
            }
        }
    }
}

private fun clock(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    fun two(v: Long) = v.toString().padStart(2, '0')
    return if (h > 0) "$h:${two(m)}:${two(s)}" else "${two(m)}:${two(s)}"
}
