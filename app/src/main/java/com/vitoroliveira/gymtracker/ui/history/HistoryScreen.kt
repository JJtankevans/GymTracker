package com.vitoroliveira.gymtracker.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.vitoroliveira.gymtracker.presentation.viewmodel.HistoryUiState
import com.vitoroliveira.gymtracker.presentation.viewmodel.HistoryViewModel
import com.vitoroliveira.gymtracker.presentation.viewmodel.WorkoutHistoryItem
import com.vitoroliveira.gymtracker.ui.theme.GymTrackerTheme
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun HistoryScreen(
    onWorkoutClick: (String) -> Unit = {},
    viewModel: HistoryViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    HistoryScreenContent(
        uiState = uiState,
        onWorkoutClick = onWorkoutClick,
        onDismissError = viewModel::clearError
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreenContent(
    uiState: HistoryUiState,
    onWorkoutClick: (String) -> Unit = {},
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
        topBar = { TopAppBar(title = { Text("Histórico") }) },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                uiState.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                uiState.workouts.isEmpty() -> EmptyHistoryState(Modifier.align(Alignment.Center))
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.workouts, key = { it.workoutId }) { workout ->
                        WorkoutHistoryCard(item = workout, onClick = { onWorkoutClick(workout.workoutId) })
                    }
                }
            }
        }
    }
}

@Composable
private fun WorkoutHistoryCard(item: WorkoutHistoryItem, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(color = parseColor(item.muscleGroupColorHex), shape = CircleShape)
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(item.muscleGroupName, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                Text(
                    formatDate(item.startedAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                item.durationMinutes?.let { formatDuration(it) } ?: "—",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun EmptyHistoryState(modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Nenhum treino finalizado ainda", style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(
            "Seus treinos concluídos vão aparecer aqui.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

private fun parseColor(hex: String): Color = try {
    Color(("FF" + hex.removePrefix("#")).toLong(16))
} catch (e: Exception) {
    Color.Gray
}

private fun formatDate(instant: Instant): String {
    val local = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    val day = local.dayOfMonth.toString().padStart(2, '0')
    val month = local.monthNumber.toString().padStart(2, '0')
    return "$day/$month/${local.year}"
}

private fun formatDuration(minutes: Long): String {
    val hours = minutes / 60
    val mins = minutes % 60
    return if (hours > 0) "${hours}h ${mins}min" else "${mins}min"
}

@Preview
@Composable
private fun HistoryScreenPreviewWithData() {
    HistoryScreenContent(
        uiState = HistoryUiState(
            workouts = listOf(
                WorkoutHistoryItem(
                    workoutId = "1",
                    muscleGroupName = "Peito e Tríceps",
                    muscleGroupColorHex = "#E53935",
                    startedAt = Instant.parse("2026-08-10T14:00:00Z"),
                    finishedAt = Instant.parse("2026-08-10T15:05:00Z"),
                    durationMinutes = 65
                ),
                WorkoutHistoryItem(
                    workoutId = "2",
                    muscleGroupName = "Costas e Bíceps",
                    muscleGroupColorHex = "#1E88E5",
                    startedAt = Instant.parse("2026-08-08T10:00:00Z"),
                    finishedAt = Instant.parse("2026-08-08T10:50:00Z"),
                    durationMinutes = 50
                )
            )
        )
    )
}

@Preview
@Composable
private fun HistoryScreenPreviewEmpty() {
    GymTrackerTheme {HistoryScreenContent(uiState = HistoryUiState())}
}