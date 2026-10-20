package com.vitoroliveira.gymtracker.ui.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vitoroliveira.gymtracker.data.model.Exercise
import com.vitoroliveira.gymtracker.data.model.WorkoutSet
import com.vitoroliveira.gymtracker.presentation.viewmodel.WorkoutUiState
import com.vitoroliveira.gymtracker.ui.home.ProgressRing
import com.vitoroliveira.gymtracker.ui.theme.LocalThemeExtras
import com.vitoroliveira.gymtracker.ui.theme.Intensity
import com.vitoroliveira.gymtracker.ui.theme.LocalIntensity
import com.vitoroliveira.gymtracker.ui.theme.Decor
import com.vitoroliveira.gymtracker.ui.components.ConfettiBurst
import com.vitoroliveira.gymtracker.ui.components.GlitchText
import com.vitoroliveira.gymtracker.ui.components.LiftingDumbbell
import com.vitoroliveira.gymtracker.ui.utils.formatKg
import kotlinx.coroutines.launch

/**
 * Layout "Foco": um exercício por vez, números gigantes, sem teclado.
 * Registrar uma série já inicia o descanso escolhido (menos toques).
 */
@Composable
fun FocusWorkoutBody(
    uiState: WorkoutUiState,
    onAddSet: (exerciseId: String, reps: Int, weightKg: Double) -> Unit,
    onStartTimer: (Int) -> Unit,
    onCancelTimer: () -> Unit,
    onToggleExerciseFinished: (String) -> Unit,
    onEditSet: (WorkoutSet) -> Unit,
    modifier: Modifier = Modifier,
    topSlot: @Composable () -> Unit = {}
) {
    val exercises = uiState.exercises
    if (exercises.isEmpty()) {
        Box(modifier.fillMaxSize().padding(24.dp), Alignment.Center) {
            Text("Cadastre exercícios nesse grupo para treinar aqui.", textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyLarge)
        }
        return
    }
    val cs = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()
    val pager = rememberPagerState { exercises.size }
    var rest by remember { mutableIntStateOf(90) }
    val timer = uiState.timerState
    val inputs = remember(exercises) { mutableStateMapOf<String, Pair<Int, Double>>() }

    Box(modifier.fillMaxSize()) {
    Column(Modifier.fillMaxSize()) {
        // faixa de exercícios (toque pra pular)
        Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            exercises.forEachIndexed { i, e ->
                val done = uiState.activeWorkout?.finishedExerciseIds?.contains(e.id) == true
                val sel = pager.currentPage == i
                Row(
                    Modifier.clip(CircleShape)
                        .background(if (sel) cs.primary else cs.surfaceContainerHighest)
                        .border(1.dp, if (sel) cs.primary else cs.outlineVariant, CircleShape)
                        .clickable { scope.launch { pager.animateScrollToPage(i) } }
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (done) { Icon(Icons.Default.Check, null, Modifier.size(14.dp), tint = LocalThemeExtras.current.success); Spacer(Modifier.width(4.dp)) }
                    Text(e.name, style = MaterialTheme.typography.labelLarge, color = if (sel) cs.onPrimary else cs.onSurface, maxLines = 1)
                }
            }
        }

        Box(Modifier.padding(horizontal = 16.dp)) { topSlot() }

        HorizontalPager(pager, Modifier.weight(1f).fillMaxWidth(), key = { exercises[it].id ?: exercises[it].name }) { page ->
            val e = exercises[page]
            val id = e.id ?: e.name
            val sets = uiState.completedSets.filter { it.exerciseId == e.id }.sortedBy { it.setNumber }
            val base = e.id?.let { uiState.baselines[it] }
            val (reps, kg) = inputs[id] ?: Pair(base?.lastReps ?: e.defaultReps, base?.lastWeightKg ?: 0.0)
            val newRecord = e.id in uiState.newRecordExerciseIds

            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val wild = LocalIntensity.current == Intensity.WILD
                if (wild) LiftingDumbbell(cs.primary)
                val glitchy = wild && LocalThemeExtras.current.decor.let { it == Decor.NEON_GRID || it == Decor.SCANLINES }
                GlitchText(e.name, MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black, textAlign = TextAlign.Center), cs.onBackground, active = glitchy)
                Text(
                    "Série ${sets.size + 1} de ${e.defaultSets}" + (base?.lastWeightKg?.let { "  ·  última: ${formatKg(it)} kg × ${base.lastReps ?: "-"}" } ?: ""),
                    style = MaterialTheme.typography.titleSmall, color = cs.onSurfaceVariant, textAlign = TextAlign.Center
                )
                if (newRecord) Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.EmojiEvents, null, tint = cs.tertiary); Spacer(Modifier.width(6.dp))
                    Text("Novo recorde de carga!", color = cs.tertiary, fontWeight = FontWeight.Bold)
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    BigStepper("REPS", "$reps", Modifier.weight(1f),
                        { inputs[id] = Pair((reps - 1).coerceAtLeast(1), kg) }, { inputs[id] = Pair((reps + 1).coerceAtMost(99), kg) })
                    BigStepper("KG", formatKg(kg), Modifier.weight(1f),
                        { inputs[id] = Pair(reps, (kg - 2.5).coerceAtLeast(0.0)) }, { inputs[id] = Pair(reps, kg + 2.5) })
                }

                Button(
                    onClick = {
                        onAddSet(e.id ?: return@Button, reps, kg)
                        onStartTimer(rest)
                    },
                    modifier = Modifier.fillMaxWidth().height(84.dp), shape = MaterialTheme.shapes.large
                ) {
                    Icon(Icons.Default.Add, null); Spacer(Modifier.width(8.dp))
                    Text("REGISTRAR SÉRIE", fontSize = 20.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                }

                // descanso: escolha uma vez, vale pro treino todo
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Descanso", style = MaterialTheme.typography.labelMedium, color = cs.onSurfaceVariant)
                    listOf(60, 90, 120, 180).forEach { s ->
                        FilterChip(rest == s, { rest = s }, { Text(if (s < 120 || s == 180) "${s}s" else "${s / 60}min") })
                    }
                }

                if (timer.totalSeconds > 0) {
                    val done = timer.isFinished
                    ProgressRing(timer.progress, Modifier.size(150.dp), stroke = 12.dp,
                        color = if (done) cs.tertiary else cs.primary, track = cs.primary.copy(alpha = 0.16f)) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(if (done) "BORA!" else timer.formattedTime, fontSize = 34.sp, fontWeight = FontWeight.Black)
                            TextButton(onCancelTimer) { Text(if (done) "fechar" else "cancelar") }
                        }
                    }
                }

                if (sets.isNotEmpty()) {
                    Text("Séries feitas (toque para editar)", style = MaterialTheme.typography.labelMedium, color = cs.onSurfaceVariant)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        sets.forEach { s ->
                            Box(
                                Modifier.clip(MaterialTheme.shapes.medium).background(cs.surfaceContainerHighest)
                                    .border(1.dp, cs.outlineVariant, MaterialTheme.shapes.medium)
                                    .clickable { onEditSet(s) }.padding(horizontal = 12.dp, vertical = 8.dp)
                            ) { Text("${s.reps} × ${formatKg(s.weightKg)}", fontWeight = FontWeight.Bold) }
                        }
                    }
                }
                if (sets.size >= e.defaultSets) {
                    val fin = uiState.activeWorkout?.finishedExerciseIds?.contains(e.id) == true
                    OutlinedButton({ e.id?.let(onToggleExerciseFinished) }) {
                        Text(if (fin) "Exercício concluído ✓" else "Marcar exercício como concluído")
                    }
                }
                Spacer(Modifier.height(20.dp))
            }
        }
    }
    ConfettiBurst(uiState.completedSets.size)
    }
}

@Composable
private fun BigStepper(label: String, value: String, modifier: Modifier, onMinus: () -> Unit, onPlus: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    ThemedStepperCard(modifier) {
        Text(label, style = MaterialTheme.typography.labelMedium, letterSpacing = 2.sp, color = cs.onSurfaceVariant)
        Text(value, fontSize = 52.sp, fontWeight = FontWeight.Black, color = cs.primary)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FilledTonalIconButton(onMinus, Modifier.size(52.dp)) { Icon(Icons.Default.Remove, "menos") }
            FilledTonalIconButton(onPlus, Modifier.size(52.dp)) { Icon(Icons.Default.Add, "mais") }
        }
    }
}

@Composable
private fun ThemedStepperCard(modifier: Modifier, content: @Composable ColumnScope.() -> Unit) {
    com.vitoroliveira.gymtracker.ui.theme.ThemedCard(modifier) {
        Column(Modifier.fillMaxWidth().padding(vertical = 14.dp), horizontalAlignment = Alignment.CenterHorizontally, content = content)
    }
}
