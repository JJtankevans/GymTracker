package com.vitoroliveira.gymtracker.ui.circuit

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vitoroliveira.gymtracker.data.model.Exercise
import com.vitoroliveira.gymtracker.presentation.circuit.*
import com.vitoroliveira.gymtracker.presentation.viewmodel.WorkoutViewModel
import com.vitoroliveira.gymtracker.ui.home.ProgressRing
import com.vitoroliveira.gymtracker.ui.music.MiniPlayer
import com.vitoroliveira.gymtracker.ui.theme.ThemedCard
import com.vitoroliveira.gymtracker.ui.theme.ThemedScaffold
import com.vitoroliveira.gymtracker.ui.theme.ThemedTopAppBar
import com.vitoroliveira.gymtracker.ui.utils.formatKg
import com.vitoroliveira.gymtracker.ui.workout.WorkoutSummaryDialog
import org.koin.compose.viewmodel.koinViewModel

/**
 * Modo Circuito (experimental): o app conta a série, entra no descanso sozinho e
 * volta pra próxima série, aprendendo o ritmo de cada exercício. Toque só pra corrigir.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CircuitScreen(
    muscleGroupId: String?,
    muscleGroupName: String?,
    onExit: () -> Unit,
    workoutVm: WorkoutViewModel = koinViewModel(),
    circuitVm: CircuitViewModel = koinViewModel()
) {
    val w by workoutVm.uiState.collectAsState()
    val c by circuitVm.state.collectAsState()
    val cues = rememberCircuitCues()
    val view = LocalView.current
    var confirmExit by remember { mutableStateOf(false) }

    com.vitoroliveira.gymtracker.ui.music.TrackRecording(w.activeWorkout?.id, w.summary != null)
    LaunchedEffect(Unit) { workoutVm.startWorkout(muscleGroupId) }
    DisposableEffect(view) { view.keepScreenOn = true; onDispose { view.keepScreenOn = false } }

    // Eventos do motor → voz/vibração e gravação das séries no treino
    LaunchedEffect(Unit) {
        circuitVm.events.collect { ev ->
            when (ev) {
                is CircuitEvent.SetStarted -> {
                    cues.buzz(120)
                    val s = ev.step
                    cues.speak("Série ${s.setNumber} de ${s.setsTotal}. ${s.exerciseName}. ${s.reps} repetições, ${formatKg(s.weightKg)} quilos.")
                }
                is CircuitEvent.SetAutoEnded -> { cues.buzz(150, 100, 150); cues.beep() }
                is CircuitEvent.SetCommitted -> workoutVm.addSet(ev.set.step.exerciseId, ev.set.step.reps, ev.set.step.weightKg)
                is CircuitEvent.RestEnded -> {}
                CircuitEvent.Finished -> { cues.speak("Circuito concluído."); workoutVm.finishWorkout() }
            }
        }
    }

    ThemedScaffold(
        topBar = {
            ThemedTopAppBar(
                title = { Column { Text("Circuito"); Text("experimental", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary) } },
                navigationIcon = { IconButton({ if (circuitVm.isRunning) confirmExit = true else onExit() }) { Icon(Icons.Default.Close, "Sair") } },
                actions = {
                    var voice by remember { mutableStateOf(cues.voiceEnabled) }
                    TextButton({ voice = !voice; cues.voiceEnabled = voice }) { Text(if (voice) "Voz ligada" else "Voz off") }
                }
            )
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                w.activeWorkoutToRecover != null -> RecoverDialog(
                    onResume = { workoutVm.resumeWorkout(w.activeWorkoutToRecover!!) },
                    onDiscard = { workoutVm.discardAndStartNew(muscleGroupId) }
                )
                w.activeWorkout == null -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
                c.phase == CircuitPhase.IDLE -> CircuitSetup(
                    muscleGroupName = muscleGroupName,
                    exercises = w.exercises,
                    lastWeights = w.baselines.mapValues { it.value.lastWeightKg },
                    learner = circuitVm.learner,
                    onStart = { steps -> circuitVm.start(steps) }
                )
                else -> CircuitRun(c, circuitVm)
            }
        }
    }

    w.summary?.let { WorkoutSummaryDialog(summary = it, onDone = { circuitVm.stop(); onExit() }) }

    if (confirmExit) AlertDialog(
        onDismissRequest = { confirmExit = false },
        title = { Text("Encerrar circuito?") },
        text = { Text("As séries já concluídas ficam salvas no treino.") },
        confirmButton = { TextButton({ confirmExit = false; circuitVm.stop(); workoutVm.finishWorkout() }) { Text("Encerrar e salvar") } },
        dismissButton = { TextButton({ confirmExit = false }) { Text("Continuar") } }
    )
}

@Composable
private fun RecoverDialog(onResume: () -> Unit, onDiscard: () -> Unit) {
    AlertDialog(
        onDismissRequest = {},
        title = { Text("Treino em andamento") },
        text = { Text("Você já tem um treino aberto. Quer continuar com ele ou começar outro?") },
        confirmButton = { TextButton(onResume) { Text("Continuar") } },
        dismissButton = { TextButton(onDiscard) { Text("Descartar e começar novo") } }
    )
}

// ───────────────────────── preparação ─────────────────────────

private data class Row(val e: Exercise, val sets: Int, val reps: Int, val kg: Double)

@Composable
internal fun CircuitSetup(
    muscleGroupName: String?,
    exercises: List<Exercise>,
    lastWeights: Map<String, Double?>,
    learner: SetTimingLearner,
    onStart: (List<CircuitStep>) -> Unit
) {
    var order by remember { mutableStateOf(CircuitPlanner.Order.SEQUENTIAL) }
    var goal by remember { mutableStateOf(TrainingGoal.HYPERTROPHY) }
    var rest by remember { mutableIntStateOf(goal.defaultRest) }
    val rows = remember(exercises) {
        mutableStateListOf<Row>().apply {
            exercises.forEach { add(Row(it, it.defaultSets, it.defaultReps, lastWeights[it.id] ?: 0.0)) }
        }
    }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text(muscleGroupName ?: "Treino livre", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(
                "O app conta cada série, entra no descanso sozinho e aprende o seu ritmo. Você só toca pra corrigir.",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        item {
            Text("Ordem", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(order == CircuitPlanner.Order.SEQUENTIAL, { order = CircuitPlanner.Order.SEQUENTIAL }, { Text("Sequencial") })
                FilterChip(order == CircuitPlanner.Order.ROUND_ROBIN, { order = CircuitPlanner.Order.ROUND_ROBIN }, { Text("Revezado (circuito)") })
            }
        }
        item {
            Text("Objetivo", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TrainingGoal.entries.forEach { g ->
                    FilterChip(goal == g, { goal = g; rest = g.defaultRest }, { Text(g.label) })
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton({ rest = (rest - 15).coerceAtLeast(15) }) { Icon(Icons.Default.Remove, "menos") }
                Text("Descanso ${rest}s", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                IconButton({ rest += 15 }) { Icon(Icons.Default.Add, "mais") }
            }
            val verdict = ReferenceTimes.restVerdict(goal, rest)
            Text(
                "Referência para ${goal.label.lowercase()}: ${goal.restRange.first}–${goal.restRange.last}s (${goal.repsHint}). " + when (verdict) {
                    RestVerdict.SHORT -> "Você está abaixo da faixa."
                    RestVerdict.IN_RANGE -> "Dentro da faixa."
                    RestVerdict.LONG -> "Acima da faixa; ok se o foco é força máxima."
                },
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (rows.isEmpty()) item {
            Text("Esse grupo não tem exercícios. Cadastre exercícios primeiro.", style = MaterialTheme.typography.bodyMedium)
        }
        items(rows.size) { i ->
            val r = rows[i]
            val id = r.e.id ?: r.e.name
            ThemedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(r.e.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    val n = learner.sampleCount(id)
                    Text(
                        if (n == 0) "Ritmo: ainda aprendendo (padrão ${learner.predictSeconds(id, r.reps)}s por série)"
                        else "Ritmo aprendido: ~${learner.predictSeconds(id, r.reps)}s por série ($n medidas)",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Mini("séries", "${r.sets}", { rows[i] = r.copy(sets = (r.sets - 1).coerceAtLeast(1)) }, { rows[i] = r.copy(sets = (r.sets + 1).coerceAtMost(10)) })
                        Mini("reps", "${r.reps}", { rows[i] = r.copy(reps = (r.reps - 1).coerceAtLeast(1)) }, { rows[i] = r.copy(reps = (r.reps + 1).coerceAtMost(50)) })
                        Mini("kg", formatKg(r.kg), { rows[i] = r.copy(kg = (r.kg - 2.5).coerceAtLeast(0.0)) }, { rows[i] = r.copy(kg = r.kg + 2.5) })
                    }
                }
            }
        }
        item {
            Button(
                onClick = {
                    onStart(CircuitPlanner.build(
                        rows.map { CircuitPlanner.Input(it.e.id ?: it.e.name, it.e.name, it.sets, it.reps, it.kg) },
                        order, rest
                    ))
                },
                enabled = rows.isNotEmpty(),
                modifier = Modifier.fillMaxWidth().height(60.dp)
            ) { Text("INICIAR CIRCUITO", fontWeight = FontWeight.Black, letterSpacing = 1.sp) }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun Mini(label: String, value: String, onMinus: () -> Unit, onPlus: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onMinus, Modifier.size(32.dp)) { Icon(Icons.Default.Remove, null, Modifier.size(18.dp)) }
            Text(value, Modifier.widthIn(min = 38.dp), textAlign = TextAlign.Center, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            IconButton(onPlus, Modifier.size(32.dp)) { Icon(Icons.Default.Add, null, Modifier.size(18.dp)) }
        }
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ───────────────────────── execução ─────────────────────────

@Composable
fun CircuitRun(c: CircuitState, vm: CircuitViewModel) = CircuitRunContent(
    c, onDone = vm::tapDone, onStillGoing = vm::tapStillGoing, onExtend = { vm.extendRest(15) },
    onAdjust = { r, k -> vm.adjust(r, k) }, player = { MiniPlayer() }
)

@Composable
fun CircuitRunContent(
    c: CircuitState,
    onDone: () -> Unit, onStillGoing: () -> Unit, onExtend: () -> Unit,
    onAdjust: (reps: Int?, kg: Double?) -> Unit,
    player: @Composable () -> Unit = {}
) {
    val step = c.step ?: return
    val cs = MaterialTheme.colorScheme
    val resting = c.phase == CircuitPhase.REST
    val color = if (resting) cs.tertiary else cs.primary
    val progress = if (resting) (c.restLeftMs / (c.restTotalSec * 1000f).coerceAtLeast(1f)).coerceIn(0f, 1f)
    else (c.setElapsedMs / (c.predictedSetSec * 1000f).coerceAtLeast(1f)).coerceIn(0f, 1f)

    Box(Modifier.fillMaxSize()) {
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        // progresso geral
        Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            c.steps.forEachIndexed { i, _ ->
                Box(Modifier.weight(1f).height(5.dp).clip(RoundedCornerShape(3.dp)).background(
                    when { i < c.completed.size -> cs.primary; i == c.stepIndex -> color; else -> cs.onSurface.copy(alpha = 0.14f) }
                ))
            }
        }
        Spacer(Modifier.height(10.dp))
        Text("${c.completed.size}/${c.steps.size} séries", style = MaterialTheme.typography.labelMedium, color = cs.onSurfaceVariant)
        Text(step.exerciseName, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
        Text("Série ${step.setNumber} de ${step.setsTotal}  ·  ${step.reps} × ${formatKg(step.weightKg)} kg", style = MaterialTheme.typography.titleMedium)

        Spacer(Modifier.height(8.dp))
        ProgressRing(progress, Modifier.size(230.dp), stroke = 16.dp, color = color, track = color.copy(alpha = 0.16f)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(if (resting) "DESCANSO" else "SÉRIE", style = MaterialTheme.typography.labelLarge, letterSpacing = 3.sp, color = color)
                if (resting) {
                    val s = ((c.restLeftMs + 999) / 1000).toInt()
                    Text("%d:%02d".format(s / 60, s % 60), fontSize = 58.sp, fontWeight = FontWeight.Black)
                    Text("próxima: " + (c.next?.let { "${it.exerciseName} ${it.setNumber}/${it.setsTotal}" } ?: "fim"), style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
                } else {
                    Text("${c.setElapsedMs / 1000}", fontSize = 72.sp, fontWeight = FontWeight.Black)
                    Text("previsto ${c.predictedSetSec}s", style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
                }
            }
        }

        if (!resting) {
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip({ onAdjust(step.reps - 1, null) }, { Text("− rep") })
                AssistChip({ onAdjust(step.reps + 1, null) }, { Text("+ rep") })
                AssistChip({ onAdjust(null, step.weightKg - 2.5) }, { Text("− 2,5 kg") })
                AssistChip({ onAdjust(null, step.weightKg + 2.5) }, { Text("+ 2,5 kg") })
            }
        } else if (c.autoEnded) {
            Spacer(Modifier.height(6.dp))
            Text("Acabou pelo tempo previsto. Se ainda estava fazendo a série, toque no botão grande.", style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center, color = cs.onSurfaceVariant)
        }

        Spacer(Modifier.weight(1f))

        if (!resting) {
            Button(onDone, Modifier.fillMaxWidth().height(96.dp), shape = MaterialTheme.shapes.large) {
                Text("TERMINEI", fontSize = 26.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
            }
        } else {
            val still = c.pendingDurationSec + (((c.restTotalSec * 1000L - c.restLeftMs) / 1000).toInt())
            Button(
                onStillGoing, Modifier.fillMaxWidth().height(96.dp), shape = MaterialTheme.shapes.large,
                colors = ButtonDefaults.buttonColors(containerColor = cs.tertiary, contentColor = cs.onTertiary)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("AINDA NÃO TERMINEI", fontSize = 20.sp, fontWeight = FontWeight.Black)
                    Text("conta a série como ${still}s e reinicia o descanso", style = MaterialTheme.typography.labelSmall)
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onExtend, Modifier.weight(1f).height(48.dp)) { Text("+15s descanso") }
                OutlinedButton(onDone, Modifier.weight(1f).height(48.dp)) { Icon(Icons.Default.SkipNext, null); Spacer(Modifier.width(4.dp)); Text("Pular descanso") }
            }
        }
        Spacer(Modifier.height(8.dp))
        player()
        Spacer(Modifier.height(12.dp))
    }
    com.vitoroliveira.gymtracker.ui.components.ConfettiBurst(c.completed.size)
    }
}
