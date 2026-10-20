package com.vitoroliveira.gymtracker.ui.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vitoroliveira.gymtracker.presentation.viewmodel.HomeUiState
import com.vitoroliveira.gymtracker.ui.components.BarChart
import com.vitoroliveira.gymtracker.ui.theme.HomeLayout
import com.vitoroliveira.gymtracker.ui.theme.ThemedCard

/** Ações da Home, pra os layouts não dependerem de navegação. */
data class HomeActions(
    val onGroups: () -> Unit,
    val onHistory: () -> Unit,
    val onStart: () -> Unit
)

/** Layouts que desenham o próprio botão de iniciar (sem FAB). */
fun HomeLayout.hasOwnStart(): Boolean = this != HomeLayout.CLASSIC && this != HomeLayout.BENTO

@Composable
fun HomeBody(layout: HomeLayout, state: HomeUiState, actions: HomeActions, modifier: Modifier = Modifier) {
    when (layout) {
        HomeLayout.CLASSIC -> ClassicHome(state, actions, modifier)
        HomeLayout.BENTO -> BentoHome(state, actions, modifier)
        HomeLayout.POSTER -> PosterHome(state, actions, modifier)
        HomeLayout.TERMINAL -> TerminalHome(state, actions, modifier)
        HomeLayout.ORBIT -> OrbitHome(state, actions, modifier)
        HomeLayout.EDITORIAL -> EditorialHome(state, actions, modifier)
    }
}

private fun HomeUiState.progress(): Float =
    if (weeklyGoal <= 0) 0f else (weeklyCount.toFloat() / weeklyGoal).coerceIn(0f, 1f)

private fun HomeUiState.remainingText(): String {
    val left = weeklyGoal - weeklyCount
    return when {
        weeklyGoal <= 0 -> "Defina uma meta no perfil."
        left <= 0 -> "Meta batida. Descanse bem."
        left == 1 -> "Falta 1 treino para a meta."
        else -> "Faltam $left treinos para a meta."
    }
}

// ───────────────────────── CLASSIC ─────────────────────────

@Composable
private fun ClassicHome(state: HomeUiState, a: HomeActions, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        WeeklyGoalCard(state.weeklyCount, state.weeklyGoal)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            StreakCard(state.streakDays, Modifier.weight(1f))
            LastWorkoutCard(state.lastWorkoutDate, state.lastWorkoutMuscleGroupName, Modifier.weight(1f))
        }
        if (state.weeklyHistory.any { it.count > 0 }) WeeklyHistoryCard(state)
        Spacer(Modifier.height(8.dp))
        Text("Atalhos", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        QuickAccessRow(a.onGroups, a.onHistory)
        Spacer(Modifier.height(72.dp))
    }
}

// ───────────────────────── anel de progresso ─────────────────────────

@Composable
internal fun ProgressRing(
    progress: Float,
    modifier: Modifier = Modifier,
    stroke: androidx.compose.ui.unit.Dp = 14.dp,
    color: Color = MaterialTheme.colorScheme.primary,
    track: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
    content: @Composable BoxScope.() -> Unit = {}
) {
    val anim = remember(progress) { Animatable(0f) }
    LaunchedEffect(progress) { anim.animateTo(progress, tween(1000, easing = FastOutSlowInEasing)) }
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val w = stroke.toPx()
            val d = size.minDimension - w
            val tl = Offset((size.width - d) / 2, (size.height - d) / 2)
            drawArc(track, 0f, 360f, false, tl, Size(d, d), style = Stroke(w, cap = StrokeCap.Round))
            if (anim.value > 0f) drawArc(color, -90f, 360f * anim.value, false, tl, Size(d, d), style = Stroke(w, cap = StrokeCap.Round))
        }
        content()
    }
}

// ───────────────────────── BENTO ─────────────────────────

@Composable
private fun BentoHome(state: HomeUiState, a: HomeActions, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth().height(206.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ThemedCard(Modifier.weight(1.25f).fillMaxHeight(), containerColor = MaterialTheme.colorScheme.primaryContainer) {
                Column(Modifier.fillMaxSize().padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    ProgressRing(state.progress(), Modifier.size(128.dp), stroke = 12.dp) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${state.weeklyCount}", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Text("de ${state.weeklyGoal}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("Meta da semana", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
            Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ThemedCard(Modifier.weight(1f).fillMaxWidth()) {
                    Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.Center) {
                        Icon(Icons.Default.LocalFireDepartment, null, tint = MaterialTheme.colorScheme.tertiary)
                        Text("${state.streakDays}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                        Text("dias seguidos", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                ThemedCard(Modifier.weight(1f).fillMaxWidth()) {
                    Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.Center) {
                        Text(state.lastWorkoutDate ?: "—", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(state.lastWorkoutMuscleGroupName ?: "Sem treinos", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
                    }
                }
            }
        }
        if (state.weeklyHistory.any { it.count > 0 }) WeeklyHistoryCard(state)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BentoTile("Grupos", Icons.Default.List, Modifier.weight(1f), a.onGroups)
            BentoTile("Histórico", Icons.Default.DateRange, Modifier.weight(1f), a.onHistory)
        }
        Spacer(Modifier.height(72.dp))
    }
}

@Composable
private fun BentoTile(label: String, icon: ImageVector, modifier: Modifier, onClick: () -> Unit) {
    ThemedCard(modifier, onClick = onClick) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(10.dp))
            Text(label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Icon(Icons.AutoMirrored.Filled.ArrowForward, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ───────────────────────── POSTER ─────────────────────────

@Composable
private fun PosterHome(state: HomeUiState, a: HomeActions, modifier: Modifier) {
    val cs = MaterialTheme.colorScheme
    Column(modifier, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("ESTA SEMANA", style = MaterialTheme.typography.labelLarge, letterSpacing = 4.sp, color = cs.onSurfaceVariant)
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                "${state.weeklyCount}",
                style = MaterialTheme.typography.displayLarge.copy(fontSize = 176.sp, lineHeight = 160.sp, fontWeight = FontWeight.Black),
                color = cs.primary
            )
            Text(
                "/${state.weeklyGoal}",
                style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Black),
                color = cs.onBackground.copy(alpha = 0.55f),
                modifier = Modifier.padding(bottom = 18.dp, start = 4.dp)
            )
        }
        // barra segmentada: um bloco por treino da meta
        Row(Modifier.fillMaxWidth().height(18.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            val total = state.weeklyGoal.coerceAtLeast(1)
            repeat(total) { i ->
                Box(
                    Modifier.weight(1f).fillMaxHeight()
                        .clip(MaterialTheme.shapes.small)
                        .background(if (i < state.weeklyCount) cs.primary else cs.primary.copy(alpha = 0.16f))
                )
            }
        }
        Text(state.remainingText(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        HorizontalDivider(Modifier.padding(vertical = 4.dp), thickness = 3.dp, color = cs.onBackground)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(Modifier.weight(1f)) {
                Text("${state.streakDays}", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black, color = cs.tertiary)
                Text("DIAS SEGUIDOS", style = MaterialTheme.typography.labelSmall, letterSpacing = 2.sp)
            }
            Column(Modifier.weight(1f)) {
                Text(state.lastWorkoutDate ?: "—", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black)
                Text((state.lastWorkoutMuscleGroupName ?: "SEM TREINOS").uppercase(), style = MaterialTheme.typography.labelSmall, letterSpacing = 2.sp, maxLines = 1)
            }
        }
        if (state.weeklyHistory.any { it.count > 0 }) {
            BarChart(
                values = state.weeklyHistory.map { it.count },
                xLabels = state.weeklyHistory.map { "${it.weekStart.dayOfMonth}/${it.weekStart.monthNumber}" },
                goal = state.weeklyGoal,
                modifier = Modifier.fillMaxWidth().height(120.dp)
            )
        }
        Button(
            onClick = a.onStart,
            modifier = Modifier.fillMaxWidth().height(64.dp),
        ) {
            Icon(Icons.Default.PlayArrow, null)
            Spacer(Modifier.width(8.dp))
            Text("INICIAR TREINO", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(a.onGroups, Modifier.weight(1f)) { Text("Grupos") }
            OutlinedButton(a.onHistory, Modifier.weight(1f)) { Text("Histórico") }
        }
        Spacer(Modifier.height(16.dp))
    }
}

// ───────────────────────── TERMINAL ─────────────────────────

@Composable
private fun TerminalHome(state: HomeUiState, a: HomeActions, modifier: Modifier) {
    val cs = MaterialTheme.colorScheme
    val mono = FontFamily.Monospace
    val blink by rememberInfiniteTransition(label = "cursor").animateFloat(
        1f, 0f, infiniteRepeatable(tween(530), RepeatMode.Reverse), label = "b"
    )
    val base = MaterialTheme.typography.bodyMedium
    fun line(color: Color = cs.primary) = base.copy(fontFamily = mono, color = color)
    fun bar(p: Float, n: Int = 12): String { val f = (p * n).toInt(); return "█".repeat(f) + "░".repeat(n - f) }

    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("gymtracker v1.0 — sessão iniciada", style = line(cs.onSurfaceVariant))
        Spacer(Modifier.height(8.dp))
        Text("$ status --semana", style = line())
        Text("meta ....... [${bar(state.progress())}] ${state.weeklyCount}/${state.weeklyGoal}", style = line(cs.onBackground))
        Text("sequência .. ${state.streakDays}d", style = line(cs.onBackground))
        Text("último ..... ${state.lastWorkoutDate ?: "—"} ${state.lastWorkoutMuscleGroupName ?: ""}", style = line(cs.onBackground))
        Text("total ...... ${state.totalWorkouts} treinos", style = line(cs.onBackground))
        Text("> ${state.remainingText()}", style = line(cs.tertiary))
        Spacer(Modifier.height(12.dp))
        if (state.weeklyHistory.isNotEmpty()) {
            Text("$ log --8semanas", style = line())
            val max = state.weeklyHistory.maxOf { it.count }.coerceAtLeast(1)
            state.weeklyHistory.forEach {
                val d = "${it.weekStart.dayOfMonth}".padStart(2, '0') + "/" + "${it.weekStart.monthNumber}".padStart(2, '0')
                Text("$d ${bar(it.count.toFloat() / max, 10)} ${it.count}", style = line(cs.onBackground))
            }
            Spacer(Modifier.height(12.dp))
        }
        Text("$ ls ./atalhos", style = line())
        TermCommand("[1] iniciar_treino", cs.tertiary, a.onStart)
        TermCommand("[2] grupos_musculares", cs.primary, a.onGroups)
        TermCommand("[3] historico", cs.primary, a.onHistory)
        Spacer(Modifier.height(8.dp))
        Row { Text("$ ", style = line()); Text("█", style = line().copy(color = cs.primary.copy(alpha = blink))) }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun TermCommand(text: String, color: Color, onClick: () -> Unit) {
    Text(
        "  $text",
        style = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace, color = color, fontWeight = FontWeight.Bold),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 8.dp)
    )
}

// ───────────────────────── ORBIT ─────────────────────────

@Composable
private fun OrbitHome(state: HomeUiState, a: HomeActions, modifier: Modifier) {
    val cs = MaterialTheme.colorScheme
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Spacer(Modifier.height(4.dp))
        ProgressRing(state.progress(), Modifier.size(260.dp), stroke = 20.dp, color = cs.primary, track = cs.primary.copy(alpha = 0.14f)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${state.weeklyCount}", style = MaterialTheme.typography.displayLarge.copy(fontSize = 84.sp), fontWeight = FontWeight.Black)
                Text("de ${state.weeklyGoal} treinos", style = MaterialTheme.typography.titleSmall, color = cs.onSurfaceVariant)
            }
        }
        Text(state.remainingText(), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Pill(Icons.Default.LocalFireDepartment, "${state.streakDays} dias", cs.tertiary)
            Pill(Icons.Default.CheckCircle, state.lastWorkoutDate ?: "sem treinos", cs.primary)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(28.dp), verticalAlignment = Alignment.CenterVertically) {
            RoundAction("Grupos", Icons.Default.List, 56.dp, false, a.onGroups)
            RoundAction("Iniciar", Icons.Default.PlayArrow, 84.dp, true, a.onStart)
            RoundAction("Histórico", Icons.Default.DateRange, 56.dp, false, a.onHistory)
        }
        if (state.weeklyHistory.any { it.count > 0 }) WeeklyHistoryCard(state)
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun Pill(icon: ImageVector, text: String, tint: Color) {
    val cs = MaterialTheme.colorScheme
    Row(
        Modifier.clip(CircleShape).background(cs.surfaceContainerHighest).border(1.dp, cs.outlineVariant, CircleShape).padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, Modifier.size(18.dp), tint = tint)
        Spacer(Modifier.width(6.dp))
        Text(text, style = MaterialTheme.typography.labelLarge, color = cs.onSurface)
    }
}

@Composable
private fun RoundAction(label: String, icon: ImageVector, size: androidx.compose.ui.unit.Dp, primary: Boolean, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.size(size).clip(CircleShape)
                .background(if (primary) cs.primary else cs.surfaceContainerHighest)
                .border(1.dp, if (primary) cs.primary else cs.outlineVariant, CircleShape)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, label, Modifier.size(size * 0.45f), tint = if (primary) cs.onPrimary else cs.onSurface)
        }
        Spacer(Modifier.height(6.dp))
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}

// ───────────────────────── EDITORIAL ─────────────────────────

@Composable
private fun EditorialHome(state: HomeUiState, a: HomeActions, modifier: Modifier) {
    val cs = MaterialTheme.colorScheme
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        HorizontalDivider(thickness = 3.dp, color = cs.onBackground)
        Text("EDIÇÃO DA SEMANA", style = MaterialTheme.typography.labelMedium, letterSpacing = 3.sp, color = cs.primary)
        Text(
            "${state.weeklyCount} de ${state.weeklyGoal} treinos",
            style = MaterialTheme.typography.displaySmall.copy(fontSize = 44.sp, lineHeight = 48.sp),
            fontWeight = FontWeight.Black
        )
        Text(state.remainingText(), style = MaterialTheme.typography.titleMedium, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic, color = cs.onSurfaceVariant)
        HorizontalDivider(color = cs.onBackground)
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            Column(Modifier.weight(1f).padding(end = 12.dp)) {
                Text("SEQUÊNCIA", style = MaterialTheme.typography.labelSmall, letterSpacing = 2.sp, color = cs.onSurfaceVariant)
                Text("${state.streakDays} dias", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            }
            VerticalDivider(color = cs.onBackground)
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text("ÚLTIMO TREINO", style = MaterialTheme.typography.labelSmall, letterSpacing = 2.sp, color = cs.onSurfaceVariant)
                Text(state.lastWorkoutDate ?: "—", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text(state.lastWorkoutMuscleGroupName ?: "", style = MaterialTheme.typography.bodySmall)
            }
        }
        HorizontalDivider(color = cs.onBackground)
        if (state.weeklyHistory.any { it.count > 0 }) {
            Text("OITO SEMANAS EM GRÁFICO", style = MaterialTheme.typography.labelSmall, letterSpacing = 2.sp, color = cs.onSurfaceVariant)
            BarChart(
                values = state.weeklyHistory.map { it.count },
                xLabels = state.weeklyHistory.map { "${it.weekStart.dayOfMonth}/${it.weekStart.monthNumber}" },
                goal = state.weeklyGoal,
                modifier = Modifier.fillMaxWidth().height(130.dp)
            )
            HorizontalDivider(color = cs.onBackground)
        }
        Text("SEÇÕES", style = MaterialTheme.typography.labelSmall, letterSpacing = 2.sp, color = cs.onSurfaceVariant)
        EditorialRow("Iniciar treino", a.onStart, bold = true)
        EditorialRow("Grupos musculares", a.onGroups)
        EditorialRow("Histórico", a.onHistory)
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun EditorialRow(text: String, onClick: () -> Unit, bold: Boolean = false) {
    Column(Modifier.clickable(onClick = onClick)) {
        Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(text, style = MaterialTheme.typography.titleLarge, fontWeight = if (bold) FontWeight.Black else FontWeight.Normal, modifier = Modifier.weight(1f))
            Icon(Icons.AutoMirrored.Filled.ArrowForward, null)
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}
