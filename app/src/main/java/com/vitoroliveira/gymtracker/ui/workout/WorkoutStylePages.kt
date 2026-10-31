package com.vitoroliveira.gymtracker.ui.workout

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vitoroliveira.gymtracker.data.model.Exercise
import com.vitoroliveira.gymtracker.data.model.ExerciseBaseline
import com.vitoroliveira.gymtracker.data.model.WorkoutSet
import com.vitoroliveira.gymtracker.presentation.TimerState
import com.vitoroliveira.gymtracker.presentation.body.LoadSuggestion
import com.vitoroliveira.gymtracker.presentation.viewmodel.maxSetsFor
import com.vitoroliveira.gymtracker.presentation.workout.ComparisonTone
import com.vitoroliveira.gymtracker.presentation.workout.SetComparison
import com.vitoroliveira.gymtracker.presentation.workout.compareSet
import com.vitoroliveira.gymtracker.ui.components.LoadSuggestionChip
import com.vitoroliveira.gymtracker.ui.theme.Intensity
import com.vitoroliveira.gymtracker.ui.theme.LocalIntensity
import com.vitoroliveira.gymtracker.ui.utils.formatKg

/** Tudo que uma página de exercício precisa para desenhar (os estilos Arena e Anel). */
internal class PageData(
    val exercise: Exercise,
    val sets: List<WorkoutSet>,
    val baseline: ExerciseBaseline?,
    val suggestion: LoadSuggestion?,
    val reps: Int,
    val kg: Double,
    val newRecord: Boolean,
    val saving: Boolean,
    val finished: Boolean,
    val rest: Int,
    val timer: TimerState
) {
    /** Séries previstas: as do plano, ou uma a mais que as feitas se já passou do plano (no máximo o limite do exercício). */
    val planned: Int get() = maxOf(exercise.defaultSets, sets.size + 1).coerceAtMost(maxOf(maxSetsFor(exercise), sets.size))
    val resting: Boolean get() = timer.totalSeconds > 0 && !timer.isFinished
    val comparison: SetComparison? get() = compareSet(kg, reps, baseline?.lastWeightKg, baseline?.lastReps, baseline?.recordWeightKg)
}

internal class PageActions(
    val setInput: (reps: Int, kg: Double) -> Unit,
    val register: () -> Unit,
    val editSet: (WorkoutSet) -> Unit,
    val toggleFinished: () -> Unit,
    val setRest: (Int) -> Unit,
    val cancelTimer: () -> Unit
)

// ───────────────────────── ARENA ─────────────────────────
// Sem rolagem, pensado para o polegar: nome e bolinhas de série em cima, números gigantes no meio
// (arraste para cima/baixo em cima do número), e embaixo o botão REGISTRAR que vira a barra de descanso.

@Composable
internal fun ArenaPage(d: PageData, a: PageActions, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    val wild = LocalIntensity.current == Intensity.WILD
    val e = d.exercise
    // "soco" nos números quando uma série é registrada
    val punch = remember { Animatable(1f) }
    LaunchedEffect(d.sets.size) {
        if (d.sets.isNotEmpty()) {
            punch.snapTo(if (wild) 1.2f else 1.08f)
            punch.animateTo(1f, spring(dampingRatio = 0.35f, stiffness = 320f))
        }
    }
    Column(modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            e.name, Modifier.fillMaxWidth(), maxLines = 2, textAlign = TextAlign.Center,
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black)
        )
        SetPips(d.planned, d.sets.size, Modifier.align(Alignment.CenterHorizontally), wild)
        Text(
            referenceLine(d), Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelLarge, color = cs.onSurfaceVariant
        )

        Spacer(Modifier.weight(1f))
        Row(
            Modifier.fillMaxWidth().graphicsLayer { scaleX = punch.value; scaleY = punch.value },
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            DragNumber("REPS", "${d.reps}", Modifier.weight(1f),
                onStep = { a.setInput((d.reps + it).coerceIn(1, 99), d.kg) })
            DragNumber("KG", formatKg(d.kg), Modifier.weight(1f),
                onStep = { a.setInput(d.reps, (d.kg + it * 2.5).coerceAtLeast(0.0)) })
        }
        Text(
            if (d.sets.isEmpty()) "Arraste os números para cima ou para baixo" else " ",
            Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant
        )
        Box(Modifier.fillMaxWidth().height(56.dp), Alignment.Center) {
            d.comparison?.let { CompareChip(it) }
        }
        d.suggestion?.takeIf { it.weightKg != d.kg }?.let {
            LoadSuggestionChip(it, onApply = { kg -> a.setInput(d.reps, kg) }, modifier = Modifier.align(Alignment.CenterHorizontally))
        }
        if (d.newRecord) RecordLine(Modifier.align(Alignment.CenterHorizontally))
        Spacer(Modifier.weight(1f))

        RegisterDock(d, a)
        RestChips(d.rest, a.setRest, Modifier.align(Alignment.CenterHorizontally))
        DoneSetsRow(d, a)
    }
}

// ───────────────────────── ANEL ─────────────────────────
// Um anel segmentado no centro: cada segmento é uma série (as feitas acesas, a atual pulsando) e, no descanso,
// um anel interno faz a contagem. Tocar no meio registra a série (ou pula o descanso).

@Composable
internal fun RingPage(d: PageData, a: PageActions, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    val wild = LocalIntensity.current == Intensity.WILD
    val e = d.exercise
    val tr = rememberInfiniteTransition(label = "ring")
    val pulse by tr.animateFloat(0.45f, 1f, infiniteRepeatable(tween(if (wild) 520 else 900, easing = LinearEasing), RepeatMode.Reverse), label = "pulse")
    val punch = remember { Animatable(1f) }
    LaunchedEffect(d.sets.size) {
        if (d.sets.isNotEmpty()) {
            punch.snapTo(if (wild) 1.1f else 1.04f)
            punch.animateTo(1f, spring(dampingRatio = 0.4f, stiffness = 260f))
        }
    }
    Column(modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            e.name, Modifier.fillMaxWidth(), maxLines = 2, textAlign = TextAlign.Center,
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black)
        )
        Text(
            referenceLine(d), Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelLarge, color = cs.onSurfaceVariant
        )

        BoxWithConstraints(Modifier.fillMaxWidth().weight(1f), Alignment.Center) {
            val side = minOf(maxWidth, maxHeight).coerceAtMost(320.dp)
            val n = d.planned.coerceIn(1, 12)
            val done = d.sets.size.coerceAtMost(n)
            val track = cs.outlineVariant.copy(alpha = 0.55f)
            val doneColor = cs.primary
            val restColor = cs.tertiary
            Box(
                Modifier.size(side).graphicsLayer { scaleX = punch.value; scaleY = punch.value }
                    .clip(CircleShape)
                    .clickable(enabled = d.resting || !d.saving) { if (d.resting) a.cancelTimer() else a.register() },
                Alignment.Center
            ) {
                Canvas(Modifier.fillMaxSize()) {
                    val stroke = 20.dp.toPx()
                    val half = stroke / 2
                    val arc = Size(size.width - stroke, size.height - stroke)
                    val slice = 360f / n
                    val gap = if (n > 1) 7f else 0f
                    for (i in 0 until n) {
                        val color = when {
                            i < done -> doneColor
                            i == done && !d.finished -> doneColor.copy(alpha = pulse)
                            else -> track
                        }
                        drawArc(color, -90f + i * slice + gap / 2, slice - gap, false, Offset(half, half), arc, style = Stroke(stroke, cap = StrokeCap.Round))
                    }
                    if (d.timer.totalSeconds > 0) {
                        val inner = stroke * 1.9f
                        val innerSize = Size(size.width - inner * 2, size.height - inner * 2)
                        drawArc(track, -90f, 360f, false, Offset(inner, inner), innerSize, style = Stroke(stroke * 0.45f))
                        drawArc(
                            if (d.timer.isFinished) doneColor else restColor, -90f, 360f * d.timer.progress, false,
                            Offset(inner, inner), innerSize, style = Stroke(stroke * 0.45f, cap = StrokeCap.Round)
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (d.resting) {
                        Text("DESCANSO", style = MaterialTheme.typography.labelMedium, letterSpacing = 2.sp, color = cs.onSurfaceVariant)
                        Text(d.timer.formattedTime, fontSize = 52.sp, fontWeight = FontWeight.Black, color = cs.tertiary)
                        Text("toque para pular", style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant)
                    } else {
                        Text(
                            if (d.timer.isFinished) "BORA!" else "SÉRIE ${d.sets.size + 1} DE ${d.planned}",
                            style = MaterialTheme.typography.labelMedium, letterSpacing = 2.sp, color = if (d.timer.isFinished) cs.tertiary else cs.onSurfaceVariant
                        )
                        Text(formatKg(d.kg), fontSize = 60.sp, fontWeight = FontWeight.Black, color = cs.primary)
                        Text("kg × ${d.reps} reps", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        Text("toque para registrar", style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant)
                    }
                }
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MiniStepper("REPS", "${d.reps}", Modifier.weight(1f),
                onMinus = { a.setInput((d.reps - 1).coerceAtLeast(1), d.kg) }, onPlus = { a.setInput((d.reps + 1).coerceAtMost(99), d.kg) })
            MiniStepper("KG", formatKg(d.kg), Modifier.weight(1f),
                onMinus = { a.setInput(d.reps, (d.kg - 2.5).coerceAtLeast(0.0)) }, onPlus = { a.setInput(d.reps, d.kg + 2.5) })
        }
        Box(Modifier.fillMaxWidth().height(44.dp), Alignment.Center) {
            val c = d.comparison
            val s = d.suggestion?.takeIf { it.weightKg != d.kg }
            when {
                d.newRecord -> RecordLine(Modifier)
                c != null -> CompareChip(c)
                s != null -> LoadSuggestionChip(s, onApply = { kg -> a.setInput(d.reps, kg) })
            }
        }
        RestChips(d.rest, a.setRest, Modifier.align(Alignment.CenterHorizontally))
        DoneSetsRow(d, a)
    }
}

// ───────────────────────── Peças compartilhadas ─────────────────────────

private fun referenceLine(d: PageData): String {
    val b = d.baseline
    val last = b?.lastWeightKg?.let { "Última: ${formatKg(it)} kg × ${b.lastReps ?: "-"}" }
    val record = b?.recordWeightKg?.takeIf { it > 0 }?.let { "Recorde: ${formatKg(it)} kg" }
    return listOfNotNull(last, record).joinToString("  ·  ").ifEmpty { "Série ${d.sets.size + 1} de ${d.planned}" }
}

/** Uma bolinha por série prevista: feitas cheias, a atual pulsando, as próximas vazias. */
@Composable
private fun SetPips(planned: Int, done: Int, modifier: Modifier, wild: Boolean) {
    val cs = MaterialTheme.colorScheme
    val tr = rememberInfiniteTransition(label = "pips")
    val pulse by tr.animateFloat(0.35f, 1f, infiniteRepeatable(tween(if (wild) 480 else 850, easing = LinearEasing), RepeatMode.Reverse), label = "pip")
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        for (i in 0 until planned.coerceAtMost(14)) {
            val filled = i < done
            val current = i == done
            Box(
                Modifier.size(if (current) 18.dp else 14.dp).clip(CircleShape)
                    .background(if (filled) cs.primary else if (current) cs.primary.copy(alpha = pulse * 0.45f) else cs.surfaceContainerHighest)
                    .border(1.5.dp, if (filled || current) cs.primary else cs.outlineVariant, CircleShape)
            )
        }
    }
}

/** Número gigante que também muda arrastando para cima (+) ou para baixo (-); vibra a cada passo. */
@Composable
private fun DragNumber(label: String, value: String, modifier: Modifier, onStep: (Int) -> Unit) {
    val cs = MaterialTheme.colorScheme
    val haptic = LocalHapticFeedback.current
    val step by rememberUpdatedState(onStep)
    var acc by remember { mutableFloatStateOf(0f) }
    val stepPx = with(LocalDensity.current) { 34.dp.toPx() }
    com.vitoroliveira.gymtracker.ui.theme.ThemedCard(
        modifier.pointerInput(stepPx) {
            detectVerticalDragGestures(
                onDragStart = { acc = 0f },
                onDragEnd = { acc = 0f },
                onDragCancel = { acc = 0f }
            ) { change, dy ->
                change.consume()
                acc -= dy // arrastar para cima = dy negativo = aumenta
                while (acc >= stepPx) { step(1); acc -= stepPx; haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) }
                while (acc <= -stepPx) { step(-1); acc += stepPx; haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) }
            }
        }
    ) {
        Column(Modifier.fillMaxWidth().padding(vertical = 14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, style = MaterialTheme.typography.labelMedium, letterSpacing = 2.sp, color = cs.onSurfaceVariant)
            Text(value, fontSize = 76.sp, fontWeight = FontWeight.Black, color = cs.primary, maxLines = 1)
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                FilledTonalIconButton({ step(-1) }, Modifier.size(44.dp)) { Icon(Icons.Default.Remove, "menos") }
                FilledTonalIconButton({ step(1) }, Modifier.size(44.dp)) { Icon(Icons.Default.Add, "mais") }
            }
        }
    }
}

@Composable
private fun MiniStepper(label: String, value: String, modifier: Modifier, onMinus: () -> Unit, onPlus: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    com.vitoroliveira.gymtracker.ui.theme.ThemedCard(modifier) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            FilledTonalIconButton(onMinus, Modifier.size(44.dp)) { Icon(Icons.Default.Remove, "menos") }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(label, style = MaterialTheme.typography.labelSmall, letterSpacing = 2.sp, color = cs.onSurfaceVariant)
                Text(value, fontSize = 30.sp, fontWeight = FontWeight.Black, color = cs.primary, maxLines = 1)
            }
            FilledTonalIconButton(onPlus, Modifier.size(44.dp)) { Icon(Icons.Default.Add, "mais") }
        }
    }
}

@Composable
internal fun CompareChip(c: SetComparison) {
    val cs = MaterialTheme.colorScheme
    val color = when (c.tone) {
        ComparisonTone.RECORD -> cs.tertiary
        ComparisonTone.UP -> cs.primary
        ComparisonTone.SAME -> cs.onSurfaceVariant
        ComparisonTone.DOWN -> cs.error
    }
    Row(
        Modifier.clip(CircleShape).background(color.copy(alpha = 0.14f)).border(1.dp, color.copy(alpha = 0.5f), CircleShape)
            .padding(horizontal = 14.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (c.tone == ComparisonTone.RECORD) { Icon(Icons.Default.EmojiEvents, null, Modifier.size(16.dp), tint = color); Spacer(Modifier.width(6.dp)) }
        Text(c.text, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
private fun RecordLine(modifier: Modifier) {
    val cs = MaterialTheme.colorScheme
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.EmojiEvents, null, tint = cs.tertiary); Spacer(Modifier.width(6.dp))
        Text("Novo recorde de carga!", color = cs.tertiary, fontWeight = FontWeight.Bold)
    }
}

/** O botão REGISTRAR, que durante o descanso vira a barra de contagem (tocar pula o descanso). */
@Composable
private fun RegisterDock(d: PageData, a: PageActions) {
    val cs = MaterialTheme.colorScheme
    if (d.resting) {
        Box(
            Modifier.fillMaxWidth().height(84.dp).clip(MaterialTheme.shapes.large)
                .background(cs.surfaceContainerHighest).border(1.dp, cs.outlineVariant, MaterialTheme.shapes.large)
                .clickable { a.cancelTimer() },
            Alignment.CenterStart
        ) {
            Box(Modifier.fillMaxHeight().fillMaxWidth(d.timer.progress.coerceIn(0f, 1f)).background(cs.tertiary.copy(alpha = 0.35f)))
            Row(Modifier.fillMaxWidth().padding(horizontal = 22.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("DESCANSO", style = MaterialTheme.typography.labelLarge, letterSpacing = 2.sp, color = cs.onSurfaceVariant)
                Spacer(Modifier.weight(1f))
                Text(d.timer.formattedTime, fontSize = 34.sp, fontWeight = FontWeight.Black, color = cs.onSurface)
                Spacer(Modifier.weight(1f))
                Text("pular", style = MaterialTheme.typography.labelLarge, color = cs.onSurfaceVariant)
            }
        }
    } else {
        Button(
            onClick = a.register,
            enabled = !d.saving,
            modifier = Modifier.fillMaxWidth().height(84.dp), shape = MaterialTheme.shapes.large
        ) {
            Icon(Icons.Default.Add, null); Spacer(Modifier.width(8.dp))
            Text(if (d.timer.isFinished) "BORA! REGISTRAR" else "REGISTRAR SÉRIE", fontSize = 20.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
        }
    }
}

@Composable
private fun RestChips(rest: Int, onRest: (Int) -> Unit, modifier: Modifier) {
    val cs = MaterialTheme.colorScheme
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("Descanso", style = MaterialTheme.typography.labelMedium, color = cs.onSurfaceVariant)
        listOf(60, 90, 120, 180).forEach { s ->
            FilterChip(rest == s, { onRest(s) }, { Text(if (s < 120 || s == 180) "${s}s" else "${s / 60}min") })
        }
    }
}

/** Séries feitas (toque para editar) e o botão de concluir o exercício, numa faixa só. */
@Composable
private fun DoneSetsRow(d: PageData, a: PageActions) {
    val cs = MaterialTheme.colorScheme
    if (d.sets.isEmpty()) return
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        d.sets.forEach { s ->
            Box(
                Modifier.clip(MaterialTheme.shapes.medium).background(cs.surfaceContainerHighest)
                    .border(1.dp, cs.outlineVariant, MaterialTheme.shapes.medium)
                    .clickable { a.editSet(s) }.padding(horizontal = 12.dp, vertical = 8.dp)
            ) { Text("${s.reps} × ${formatKg(s.weightKg)}", fontWeight = FontWeight.Bold) }
        }
        if (d.sets.size >= d.exercise.defaultSets) {
            OutlinedButton(a.toggleFinished) { Text(if (d.finished) "Concluído ✓" else "Concluir exercício") }
        }
    }
}
