package com.vitoroliveira.gymtracker.ui.history

import androidx.compose.material.icons.filled.DateRange
import com.vitoroliveira.gymtracker.ui.components.EmptyState
import com.vitoroliveira.gymtracker.ui.theme.ThemedCard
import com.vitoroliveira.gymtracker.ui.theme.ThemedScaffold
import com.vitoroliveira.gymtracker.ui.theme.ThemedTopAppBar
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
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
    onWorkoutClick: (workoutId: String, groupName: String) -> Unit = { _, _ -> },
    onBack: () -> Unit = {},
    viewModel: HistoryViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    HistoryScreenContent(
        uiState = uiState,
        onWorkoutClick = onWorkoutClick,
        onBack = onBack,
        onDismissError = viewModel::clearError,
        onDeleteWorkout = viewModel::deleteWorkout,
        onUndoDelete = viewModel::undoDelete,
        onCommitDelete = viewModel::commitPendingDelete
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreenContent(
    uiState: HistoryUiState,
    onWorkoutClick: (workoutId: String, groupName: String) -> Unit = { _, _ -> },
    onBack: () -> Unit = {},
    onDismissError: () -> Unit = {},
    onDeleteWorkout: (String) -> Unit = {},
    onUndoDelete: () -> Unit = {},
    onCommitDelete: () -> Unit = {}
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val calendar = remember(uiState.workouts) {
        val tz = TimeZone.currentSystemDefault()
        com.vitoroliveira.gymtracker.presentation.workout.buildTrainingCalendar(
            uiState.workouts.map { it.startedAt.toLocalDateTime(tz).date },
            kotlin.time.Clock.System.now().toLocalDateTime(tz).date
        )
    }
    // Filtro por grupo com contadores (Moxen "faceta"): toque de novo no chip ativo para limpar.
    var groupFilter by remember { mutableStateOf<String?>(null) }
    val groupCounts = remember(uiState.workouts) {
        uiState.workouts.groupingBy { com.vitoroliveira.gymtracker.ui.components.prettyGroupName(it.muscleGroupName) }.eachCount()
            .entries.sortedByDescending { it.value }
    }
    val shownWorkouts = remember(uiState.workouts, groupFilter) {
        groupFilter?.let { g -> uiState.workouts.filter { com.vitoroliveira.gymtracker.ui.components.prettyGroupName(it.muscleGroupName) == g } } ?: uiState.workouts
    }
    // Ao sair da tela com um "Desfazer" ainda aberto, a exclusão pendente é concluída.
    androidx.compose.runtime.DisposableEffect(Unit) { onDispose { onCommitDelete() } }

    // Aviso "Treino apagado · Desfazer": se não tocar em Desfazer, a exclusão é concluída.
    LaunchedEffect(uiState.pendingUndo) {
        val pending = uiState.pendingUndo ?: return@LaunchedEffect
        val result = snackbarHostState.showSnackbar(
            message = "Treino apagado: " + com.vitoroliveira.gymtracker.ui.components.prettyGroupName(pending.item.muscleGroupName),
            actionLabel = "Desfazer",
            duration = androidx.compose.material3.SnackbarDuration.Long
        )
        if (result == androidx.compose.material3.SnackbarResult.ActionPerformed) onUndoDelete() else onCommitDelete()
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            onDismissError()
        }
    }

    ThemedScaffold(
        topBar = {
            ThemedTopAppBar(
                title = { Text("Histórico") },
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
                uiState.isLoading -> com.vitoroliveira.gymtracker.ui.components.ListSkeleton()
                uiState.workouts.isEmpty() -> EmptyHistoryState(Modifier.align(Alignment.Center))
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item(key = "heatmap") { TrainingHeatmap(calendar) }
                    if (groupCounts.size > 1) item(key = "filters") {
                        androidx.compose.foundation.layout.Row(
                            Modifier.fillMaxWidth().horizontalScroll(androidx.compose.foundation.rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(groupFilter == null, { groupFilter = null }, { Text("Todos (${uiState.workouts.size})") })
                            groupCounts.forEach { (name, count) ->
                                FilterChip(groupFilter == name, { groupFilter = if (groupFilter == name) null else name }, { Text("$name ($count)") })
                            }
                        }
                    }
                    items(shownWorkouts, key = { it.workoutId }) { workout ->
                        WorkoutHistoryCard(
                            item = workout,
                            onClick = { onWorkoutClick(workout.workoutId, workout.muscleGroupName) },
                            onDelete = { onDeleteWorkout(workout.workoutId) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WorkoutHistoryCard(item: WorkoutHistoryItem, onClick: () -> Unit, onDelete: () -> Unit) {
    ThemedCard(onClick = onClick, onLongClick = onDelete, modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium) {
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
                Text(com.vitoroliveira.gymtracker.ui.components.prettyGroupName(item.muscleGroupName), fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                Text(
                    formatDate(item.startedAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                item.durationMinutes?.takeIf { it in 1..MAX_PLAUSIBLE_MINUTES }?.let { formatDuration(it) } ?: "—",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            // Apagar tem botão visível (não só gesto de segurar); o "Desfazer" cobre o engano.
            var menuOpen by remember { mutableStateOf(false) }
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(androidx.compose.material.icons.Icons.Default.MoreVert, contentDescription = "Mais opções", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                androidx.compose.material3.DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    androidx.compose.material3.DropdownMenuItem(
                        text = { Text("Apagar treino", color = MaterialTheme.colorScheme.error) },
                        leadingIcon = { Icon(androidx.compose.material.icons.Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) },
                        onClick = { menuOpen = false; onDelete() }
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyHistoryState(modifier: Modifier = Modifier) {
    EmptyState(
        icon = Icons.Default.DateRange,
        title = "Nenhum treino finalizado ainda",
        message = "Seus treinos concluídos vão aparecer aqui.",
        modifier = modifier
    )
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

/** Treino que ficou aberto por horas (esqueceram de finalizar); acima de 4 h não mostramos não tem duração confiável. */
private const val MAX_PLAUSIBLE_MINUTES = 240L

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
        ),
        onBack = {}
    )
}

@Preview
@Composable
private fun HistoryScreenPreviewEmpty() {
    GymTrackerTheme { HistoryScreenContent(uiState = HistoryUiState(), onBack = {}) }
}
