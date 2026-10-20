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