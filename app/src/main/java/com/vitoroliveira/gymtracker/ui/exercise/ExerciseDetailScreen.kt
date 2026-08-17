// app/src/main/java/com/vitoroliveira/gymtracker/ui/exercise/ExerciseDetailScreen.kt

package com.vitoroliveira.gymtracker.ui.exercise

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.m3.common.rememberM3VicoTheme
import com.patrykandpatrick.vico.compose.common.ProvideVicoTheme
import com.patrykandpatrick.vico.compose.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.compose.cartesian.data.lineSeries
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

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            onDismissError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(exerciseName) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
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
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    StatsRow(uiState)
                    ProgressionChart(uiState.progression)
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
            value = uiState.maxWeightKg?.let { "${it} kg" } ?: "—",
            modifier = Modifier.weight(1f)
        )
        StatCard(
            label = "Última carga",
            value = uiState.lastWeightKg?.let { "${it} kg" } ?: "—",
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
    Card(modifier = modifier, shape = RoundedCornerShape(12.dp)) {
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
    val modelProducer = remember { CartesianChartModelProducer() }

    // Datas indexadas — usadas pelo formatter do eixo X (ver bottomAxisFormatter abaixo)
    val dates = remember(progression) {
        progression.map { runCatching { LocalDate.parse(it.sessionDate) }.getOrNull() }
    }

    LaunchedEffect(progression) {
        modelProducer.runTransaction {
            lineSeries { series(progression.map { it.maxWeightKg }) }
        }
    }

    val bottomAxisFormatter = remember(dates) {
        CartesianValueFormatter { _, x, _ ->
            dates.getOrNull(x.toInt())?.let { "${it.dayOfMonth}/${it.monthNumber}" } ?: ""
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                "Progressão de carga",
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(12.dp))

            ProvideVicoTheme(rememberM3VicoTheme()) {
                CartesianChartHost(
                    chart = rememberCartesianChart(
                        rememberLineCartesianLayer(),
                        startAxis = VerticalAxis.rememberStart(),
                        bottomAxis = HorizontalAxis.rememberBottom(valueFormatter = bottomAxisFormatter)
                    ),
                    modelProducer = modelProducer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                )
            }
        }
    }
}

@Composable
private fun EmptyProgressionState(modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Sem histórico ainda", style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(
            "Registre séries desse exercício em um treino para ver a progressão de carga aqui.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
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