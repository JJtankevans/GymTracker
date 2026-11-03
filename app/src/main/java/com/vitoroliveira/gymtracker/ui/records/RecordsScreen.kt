package com.vitoroliveira.gymtracker.ui.records

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.automirrored.filled.TrendingFlat
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
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
import com.vitoroliveira.gymtracker.data.catalog.ExerciseCatalog
import com.vitoroliveira.gymtracker.presentation.records.GoalProgress
import com.vitoroliveira.gymtracker.presentation.records.Plateau
import com.vitoroliveira.gymtracker.presentation.records.parseGoalKg
import com.vitoroliveira.gymtracker.presentation.records.RecordRow
import com.vitoroliveira.gymtracker.presentation.records.RecordsUiState
import com.vitoroliveira.gymtracker.presentation.records.RecordsViewModel
import com.vitoroliveira.gymtracker.ui.exercise.ExerciseArt
import com.vitoroliveira.gymtracker.ui.exercise.tint
import com.vitoroliveira.gymtracker.ui.theme.ThemedCard
import com.vitoroliveira.gymtracker.ui.theme.ThemedScaffold
import com.vitoroliveira.gymtracker.ui.theme.ThemedTopAppBar
import com.vitoroliveira.gymtracker.ui.utils.formatKg
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun RecordsScreen(onBack: () -> Unit, viewModel: RecordsViewModel = koinViewModel(), goalPrefs: LoadGoalPrefs = koinInject()) {
    val state by viewModel.state.collectAsState()
    val goals by goalPrefs.goals.collectAsState()
    RecordsContent(state, onBack, onRetry = viewModel::load, goals = goals, onSetGoal = goalPrefs::set)
}

@Composable
fun RecordsContent(
    state: RecordsUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit = {},
    goals: Map<String, Double> = emptyMap(),
    onSetGoal: (String, Double?) -> Unit = { _, _ -> }
) {
    val snackbar = remember { SnackbarHostState() }
    var editing by remember { mutableStateOf<RecordRow?>(null) }
    editing?.let { r ->
        GoalDialog(r, goals[r.exerciseId], onDismiss = { editing = null }) { kg -> onSetGoal(r.exerciseId, kg); editing = null }
    }
    LaunchedEffect(state.errorMessage) { state.errorMessage?.let { snackbar.showSnackbar(it) } }

    ThemedScaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            ThemedTopAppBar(
                title = { Text("Recordes") },
                navigationIcon = { IconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar") } }
            )
        }
    ) { padding ->
        when {
            state.isLoading -> Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) { CircularProgressIndicator() }
            state.errorMessage != null && state.records.isEmpty() -> Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                com.vitoroliveira.gymtracker.ui.components.ErrorState(state.errorMessage!!, onRetry)
            }
            state.records.isEmpty() -> Box(Modifier.fillMaxSize().padding(padding).padding(32.dp), Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.EmojiEvents, null, Modifier.size(56.dp), tint = MaterialTheme.colorScheme.tertiary)
                    Spacer(Modifier.height(12.dp))
                    Text("Nenhum recorde ainda", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Registre séries com carga e o seu melhor de cada exercício aparece aqui.",
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            else -> LazyColumn(
                Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item(key = "summary") { Summary(state) }
                if (state.plateaus.isNotEmpty()) item(key = "plateaus") { PlateausCard(state.plateaus) }
                items(state.records, key = { it.exerciseId }) { r ->
                    RecordCard(r, state, goals[r.exerciseId]) { editing = r }
                }
                item(key = "foot") {
                    Text(
                        "Toque num recorde para definir uma meta de carga. " +
                            "O 1RM é uma estimativa (fórmula de Epley) a partir da carga e das repetições do dia. " +
                            "Exercícios só com peso do corpo não entram.",
                        style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun Summary(state: RecordsUiState) {
    ThemedCard(Modifier.fillMaxWidth(), containerColor = MaterialTheme.colorScheme.primaryContainer) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.EmojiEvents, null, Modifier.size(40.dp), tint = MaterialTheme.colorScheme.tertiary)
            Spacer(Modifier.width(14.dp))
            Column {
                Text(
                    "${state.records.size} ${if (state.records.size == 1) "recorde" else "recordes"}",
                    style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    if (state.recentCount > 0) "${state.recentCount} batido${if (state.recentCount > 1) "s" else ""} nos últimos 30 dias"
                    else "Nenhum recorde novo nos últimos 30 dias",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

/** Exercícios com o recorde parado há 6+ semanas, mesmo sendo treinados, com uma dica do que mudar. */
@Composable
private fun PlateausCard(plateaus: List<Plateau>) {
    var open by remember { mutableStateOf(false) }
    ThemedCard(Modifier.fillMaxWidth().clickable { open = !open }, shape = MaterialTheme.shapes.medium) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.AutoMirrored.Filled.TrendingFlat, null, tint = MaterialTheme.colorScheme.error)
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text("Estagnados", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(
                        "${plateaus.size} exercício${if (plateaus.size > 1) "s" else ""} sem recorde há 6+ semanas",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(if (open) Icons.Default.ExpandLess else Icons.Default.ExpandMore, if (open) "Recolher" else "Ver")
            }
            if (open) plateaus.forEach { p ->
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(p.exerciseName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Text("${formatKg(p.bestKg)} kg em ${dayMonth(p.bestDate)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(
                    "${p.sessionsSince} treinos em 6 semanas, máximo ${formatKg(p.topRecentKg)} kg. ${p.tip}",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun RecordCard(r: RecordRow, state: RecordsUiState, goalKg: Double?, onClick: () -> Unit) {
    val catalog = remember(r.exerciseName) {
        ExerciseCatalog.find(r.exerciseName)
    }
    val recent = state.today?.let { r.isRecent(it) } == true

    ThemedCard(Modifier.fillMaxWidth().clickable(onClick = onClick), shape = MaterialTheme.shapes.medium) {
        Column {
            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                if (catalog != null) {
                    val tint = catalog.muscle.tint()
                    Box(
                        Modifier.size(46.dp).clip(MaterialTheme.shapes.small).background(tint.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) { ExerciseArt(catalog, tint, 32.dp, Modifier.fillMaxSize().padding(2.dp)) }
                } else {
                    Box(
                        Modifier.size(46.dp).clip(MaterialTheme.shapes.small).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) { Icon(Icons.Default.EmojiEvents, null, tint = MaterialTheme.colorScheme.primary) }
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(r.exerciseName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(
                        "1RM estimado ${formatKg(r.bestOneRmKg)} kg · ${dayMonth(r.bestDate)}",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    r.improvementKg?.takeIf { it > 0 }?.let {
                        Text(
                            "+${formatKg(it)} kg sobre o recorde anterior",
                            style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "${formatKg(r.bestWeightKg)} kg",
                        style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (recent) Text("NOVO", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary, fontWeight = FontWeight.Bold)
                }
            }
            goalKg?.let { target ->
                val g = GoalProgress(target, r.bestWeightKg)
                Column(Modifier.padding(start = 14.dp, end = 14.dp, bottom = 12.dp)) {
                    LinearProgressIndicator(
                        progress = { g.fraction },
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(MaterialTheme.shapes.small),
                        color = if (g.reached) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        if (g.reached) "Meta de ${formatKg(target)} kg batida! Toque para pôr outra."
                        else "Meta ${formatKg(target)} kg · faltam ${formatKg(g.remainingKg)} kg (${g.percent}%)",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (g.reached) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun GoalDialog(r: RecordRow, current: Double?, onDismiss: () -> Unit, onSave: (Double?) -> Unit) {
    var text by remember { mutableStateOf(current?.let { formatKg(it) } ?: formatKg(r.bestWeightKg + if (r.bestWeightKg < 40) 2.5 else 5.0)) }
    val parsed = parseGoalKg(text)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Meta: ${r.exerciseName}") },
        text = {
            Column {
                Text("Recorde atual: ${formatKg(r.bestWeightKg)} kg", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it.take(7) },
                    label = { Text("Carga da meta (kg)") },
                    singleLine = true,
                    isError = text.isNotBlank() && parsed == null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
            }
        },
        confirmButton = { TextButton(onClick = { onSave(parsed) }, enabled = parsed != null) { Text("Salvar") } },
        dismissButton = {
            Row {
                if (current != null) TextButton(onClick = { onSave(null) }) { Text("Tirar meta") }
                TextButton(onClick = onDismiss) { Text("Cancelar") }
            }
        }
    )
}

private fun dayMonth(iso: String): String =
    iso.split("-").let { if (it.size == 3) "${it[2]}/${it[1]}/${it[0].takeLast(2)}" else iso }
