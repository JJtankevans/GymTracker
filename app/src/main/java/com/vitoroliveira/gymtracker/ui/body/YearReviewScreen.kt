package com.vitoroliveira.gymtracker.ui.body

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.vitoroliveira.gymtracker.presentation.body.EvolutionViewModel
import com.vitoroliveira.gymtracker.presentation.body.MonthlyRecap
import com.vitoroliveira.gymtracker.presentation.body.YearReview
import com.vitoroliveira.gymtracker.ui.components.prettyGroupName
import com.vitoroliveira.gymtracker.ui.theme.ThemedCard
import com.vitoroliveira.gymtracker.ui.theme.ThemedScaffold
import com.vitoroliveira.gymtracker.ui.theme.ThemedTopAppBar
import com.vitoroliveira.gymtracker.ui.utils.formatVolume
import com.vitoroliveira.gymtracker.ui.workout.ShareColors
import com.vitoroliveira.gymtracker.ui.workout.ShareStats
import com.vitoroliveira.gymtracker.ui.workout.renderWorkoutImage
import com.vitoroliveira.gymtracker.ui.workout.shareWorkoutImage
import kotlinx.datetime.DayOfWeek
import org.koin.compose.viewmodel.koinViewModel

private val MONTH_NAMES = listOf("janeiro", "fevereiro", "março", "abril", "maio", "junho", "julho", "agosto", "setembro", "outubro", "novembro", "dezembro")
private val MONTH_INITIALS = listOf("J", "F", "M", "A", "M", "J", "J", "A", "S", "O", "N", "D")

private fun weekdayName(d: DayOfWeek) = when (d) {
    DayOfWeek.MONDAY -> "segunda-feira"; DayOfWeek.TUESDAY -> "terça-feira"; DayOfWeek.WEDNESDAY -> "quarta-feira"
    DayOfWeek.THURSDAY -> "quinta-feira"; DayOfWeek.FRIDAY -> "sexta-feira"; DayOfWeek.SATURDAY -> "sábado"
    else -> "domingo"
}

/** "Meu ano" (#539): tela própria da retrospectiva anual, com treinos por mês, destaques e imagem para compartilhar. */
@Composable
fun YearReviewScreen(onBack: () -> Unit, vm: EvolutionViewModel = koinViewModel()) {
    val state by vm.state.collectAsState()
    val review = state.yearReview
    val recap = state.yearRecap
    ThemedScaffold(
        topBar = {
            ThemedTopAppBar(
                title = { Text("Meu ano") },
                navigationIcon = { IconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar") } }
            )
        }
    ) { padding ->
        when {
            state.isLoading && review == null -> Column(Modifier.fillMaxSize().padding(padding), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) { CircularProgressIndicator() }
            review == null || recap == null || review.isEmpty -> Column(Modifier.fillMaxSize().padding(padding).padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text("Ainda não há treinos neste ano.", style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
                Text("Quando você treinar, a retrospectiva do ano aparece aqui.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            }
            else -> YearReviewContent(review, recap, Modifier.padding(padding))
        }
    }
}

@Composable
private fun YearReviewContent(review: YearReview, recap: MonthlyRecap, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    val ctx = LocalContext.current
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item(key = "hero") {
            ThemedCard(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("${review.year}", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black, color = cs.primary)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Stat("${recap.workouts}", if (recap.workouts == 1) "treino" else "treinos")
                        Stat("${recap.daysTrained}", if (recap.daysTrained == 1) "dia" else "dias")
                        Stat("${recap.sets}", "séries")
                        Stat(formatVolume(recap.volumeKg), "volume")
                    }
                }
            }
        }
        item(key = "months") {
            ThemedCard(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Treinos por mês", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    MonthBars(review.perMonth, review.bestMonth)
                    review.bestMonth?.let {
                        Text("Melhor mês: ${MONTH_NAMES[it - 1]} (${review.perMonth[it - 1]} treinos)", style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
                    }
                }
            }
        }
        item(key = "highlights") {
            ThemedCard(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Destaques", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    review.favoriteWeekday?.let { Fact("Dia favorito", weekdayName(it)) }
                    if (review.longestStreakDays > 1) Fact("Maior sequência", "${review.longestStreakDays} dias seguidos")
                    val max = review.topGroups.maxOfOrNull { it.workouts } ?: 1
                    review.topGroups.forEach { g ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(prettyGroupName(g.name), Modifier.weight(1f).padding(end = 12.dp), style = MaterialTheme.typography.bodyMedium, maxLines = 2)
                            LinearProgressIndicator(progress = { g.workouts / max.toFloat() }, Modifier.width(110.dp).height(10.dp))
                            Text("${g.workouts}x", Modifier.width(44.dp), textAlign = TextAlign.End, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
        item(key = "share") {
            OutlinedButton(
                onClick = {
                    val colors = ShareColors(
                        bgTop = cs.background.toArgb(), bgBottom = cs.surfaceContainerHighest.compositeOver(cs.background).toArgb(),
                        text = cs.onBackground.toArgb(), muted = cs.onSurfaceVariant.toArgb(), accent = cs.primary.toArgb(), onAccent = cs.onPrimary.toArgb()
                    )
                    val pills = buildList {
                        review.bestMonth?.let { add("Melhor mês: ${MONTH_NAMES[it - 1]}") }
                        if (review.longestStreakDays > 1) add("${review.longestStreakDays} dias seguidos")
                        review.favoriteWeekday?.let { add("Dia favorito: ${weekdayName(it)}") }
                        review.topGroups.firstOrNull()?.let { add("Mais treinado: ${prettyGroupName(it.name)}") }
                    }
                    val stats = ShareStats(
                        title = "Meu ${review.year}", dateText = "${review.year}", subtitle = "Minha retrospectiva do ano",
                        blocks = listOf("TREINOS" to "${recap.workouts}", "SÉRIES" to "${recap.sets}", "VOLUME" to formatVolume(recap.volumeKg)),
                        records = pills, footer = "${recap.daysTrained} dias treinados · GymTracker"
                    )
                    runCatching { ctx.shareWorkoutImage(renderWorkoutImage(stats, colors), "gymtracker_meu_ano.png") }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Compartilhar como imagem") }
        }
    }
}

@Composable
private fun Stat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun Fact(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = FontWeight.Bold)
    }
}

/** 12 barras (janeiro a dezembro); o melhor mês na cor de destaque, os outros mais suaves, número em cima de cada barra. */
@Composable
private fun MonthBars(perMonth: List<Int>, best: Int?) {
    val cs = MaterialTheme.colorScheme
    val max = (perMonth.maxOrNull() ?: 0).coerceAtLeast(1)
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth().height(110.dp), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.Bottom) {
            perMonth.forEachIndexed { i, n ->
                Column(Modifier.weight(1f).fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                    if (n > 0) Text("$n", style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant)
                    val color = if (best == i + 1) cs.primary else cs.primary.copy(alpha = 0.40f)
                    Canvas(Modifier.fillMaxWidth().height((if (n == 0) 3 else 6 + 80 * n / max).dp)) {
                        drawRoundRect(if (n == 0) cs.onSurface.copy(alpha = 0.10f) else color, size = Size(size.width, size.height), cornerRadius = CornerRadius(4.dp.toPx()))
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            MONTH_INITIALS.forEach { Text(it, Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant) }
        }
    }
}
