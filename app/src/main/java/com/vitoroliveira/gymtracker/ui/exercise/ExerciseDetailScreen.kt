// app/src/main/java/com/vitoroliveira/gymtracker/ui/exercise/ExerciseDetailScreen.kt

package com.vitoroliveira.gymtracker.ui.exercise

import androidx.compose.material.icons.filled.Star
import com.vitoroliveira.gymtracker.ui.components.EmptyState
import com.vitoroliveira.gymtracker.ui.theme.ThemedCard
import com.vitoroliveira.gymtracker.ui.components.LineChart
import com.vitoroliveira.gymtracker.ui.utils.formatKg
import com.vitoroliveira.gymtracker.ui.theme.ThemedScaffold
import com.vitoroliveira.gymtracker.ui.theme.ThemedTopAppBar
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import com.vitoroliveira.gymtracker.presentation.workout.PlateCalculator
import com.vitoroliveira.gymtracker.presentation.workout.repMaxTable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.vitoroliveira.gymtracker.data.model.ExerciseProgression
import com.vitoroliveira.gymtracker.presentation.viewmodel.ExerciseDetailUiState
import com.vitoroliveira.gymtracker.presentation.viewmodel.ExerciseDetailViewModel
import com.vitoroliveira.gymtracker.ui.theme.GymTrackerTheme
import kotlinx.datetime.LocalDate
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun ExerciseDetailScreen(
    exerciseId: String,
    exerciseName: String,
    onBack: () -> Unit = {},
    viewModel: ExerciseDetailViewModel = koinViewModel(parameters = { parametersOf(exerciseId) })
) {
    val uiState by viewModel.uiState.collectAsState()

    ExerciseDetailScreenContent(
        exerciseName = exerciseName,
        uiState = uiState,
        onBack = onBack,
        onDismissError = viewModel::clearError
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseDetailScreenContent(
    exerciseName: String,
    uiState: ExerciseDetailUiState,
    onBack: () -> Unit = {},
    onDismissError: () -> Unit = {}
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var showPlates by remember { mutableStateOf(false) }
    if (showPlates) PlateCalculatorSheet(initialKg = uiState.lastWeightKg, onDismiss = { showPlates = false })

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            onDismissError()
        }
    }

    ThemedScaffold(
        topBar = {
            ThemedTopAppBar(
                title = { Text(exerciseName) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    IconButton(onClick = { showPlates = true }) {
                        Icon(Icons.Default.Calculate, contentDescription = "Calculadora de anilhas")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                uiState.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                uiState.progression.isEmpty() -> EmptyProgressionState(Modifier.align(Alignment.Center))
                else -> Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    StatsRow(uiState)
                    ProgressionChart(uiState.progression)
                    uiState.oneRmKg?.let { RepMaxCard(it) }
                }
            }
        }
    }
}

/** 1RM estimado e quanto daria para fazer em cada número de repetições. */
@Composable
private fun RepMaxCard(oneRmKg: Double) {
    val rows = remember(oneRmKg) { repMaxTable(oneRmKg) }
    ThemedCard(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("1RM estimado: ${formatKg(oneRmKg)} kg", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                "Carga máxima estimada para 1 repetição (fórmula de Epley, a partir das suas séries). Use como referência, não como meta.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
                Text("Reps", Modifier.weight(1f), style = MaterialTheme.typography.labelMedium)
                Text("Carga", Modifier.weight(1f), style = MaterialTheme.typography.labelMedium)
                Text("% do 1RM", Modifier.weight(1f), style = MaterialTheme.typography.labelMedium)
            }
            HorizontalDivider()
            rows.forEach { r ->
                Row(Modifier.fillMaxWidth()) {
                    Text("${r.reps}", Modifier.weight(1f))
                    Text("${formatKg(r.weightKg)} kg", Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                    Text("${r.percent}%", Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

/** Calculadora de anilhas: carga total + barra -> anilhas de cada lado. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PlateCalculatorSheet(initialKg: Double?, onDismiss: () -> Unit) {
    var targetText by remember { mutableStateOf(initialKg?.takeIf { it > 0 }?.let { formatKg(it) } ?: "") }
    var bar by remember { mutableStateOf(PlateCalculator.bars.first()) }
    val target = targetText.replace(',', '.').toDoubleOrNull()
    val result = target?.let { PlateCalculator.plates(it, bar) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Calculadora de anilhas", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = targetText,
                onValueChange = { targetText = it.filter { c -> c.isDigit() || c == ',' || c == '.' }.take(6) },
                label = { Text("Carga total (kg)") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )
            Text("Barra", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PlateCalculator.bars.forEach { b ->
                    FilterChip(selected = bar == b, onClick = { bar = b }, label = { Text("${formatKg(b)} kg") })
                }
            }
            when {
                result == null -> Text("Digite a carga total para ver as anilhas.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                result.belowBar -> Text("A carga é menor que a barra (${formatKg(bar)} kg).", color = MaterialTheme.colorScheme.error)
                result.perSide.isEmpty() -> Text("Só a barra.", fontWeight = FontWeight.Bold)
                else -> {
                    Text("De cada lado:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        result.perSide.forEach { p ->
                            Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                                Text(
                                    "${formatKg(p)} kg", Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                    Text(
                        "Total montado: ${formatKg(result.achievedKg)} kg" +
                            if (result.missingKg > 0) " (faltam ${formatKg(result.missingKg)} kg: não fecha com as anilhas comuns)" else "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (result.missingKg > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun StatsRow(uiState: ExerciseDetailUiState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatCard(
            label = "Maior carga",
            value = uiState.maxWeightKg?.let { "${formatKg(it)} kg" } ?: "—",
            modifier = Modifier.weight(1f)
        )
        StatCard(
            label = "Última carga",
            value = uiState.lastWeightKg?.let { "${formatKg(it)} kg" } ?: "—",
            modifier = Modifier.weight(1f)
        )
        StatCard(
            label = "Sessões",
            value = "${uiState.totalSessions}",
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    ThemedCard(modifier = modifier, shape = MaterialTheme.shapes.medium) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ProgressionChart(progression: List<ExerciseProgression>) {

    val labels = remember(progression) {
        progression.map {
            runCatching { LocalDate.parse(it.sessionDate) }.getOrNull()
                ?.let { d -> "${d.dayOfMonth}/${d.monthNumber}" } ?: ""
        }
    }
    val weights = remember(progression) { progression.map { it.maxWeightKg } }

    ThemedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                "Progressão de carga",
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(12.dp))

            LineChart(
                values = weights,
                xLabels = labels,
                modifier = Modifier.fillMaxWidth().height(220.dp)
            )
        }
    }
}

@Composable
private fun EmptyProgressionState(modifier: Modifier = Modifier) {
    EmptyState(
        icon = Icons.Default.Star,
        title = "Sem histórico ainda",
        message = "Registre séries desse exercício em um treino para ver a progressão de carga aqui.",
        modifier = modifier
    )
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun ExerciseDetailScreenPreview() {
    GymTrackerTheme {
        ExerciseDetailScreenContent(
            exerciseName = "Supino Reto",
            uiState = ExerciseDetailUiState(
                progression = listOf(
                    ExerciseProgression("1", "Supino Reto", "p1", "2026-07-01", 40.0, 30, 3),
                    ExerciseProgression("1", "Supino Reto", "p1", "2026-07-15", 42.5, 30, 3),
                    ExerciseProgression("1", "Supino Reto", "p1", "2026-08-01", 45.0, 30, 3)
                ),
                maxWeightKg = 45.0,
                lastWeightKg = 45.0,
                totalSessions = 3
            )
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun ExerciseDetailScreenEmptyPreview() {
    GymTrackerTheme {
        ExerciseDetailScreenContent(exerciseName = "Supino Reto", uiState = ExerciseDetailUiState())
    }
}