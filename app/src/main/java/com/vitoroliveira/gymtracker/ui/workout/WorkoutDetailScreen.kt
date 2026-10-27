package com.vitoroliveira.gymtracker.ui.workout

import com.vitoroliveira.gymtracker.ui.theme.ThemedCard
import com.vitoroliveira.gymtracker.ui.theme.ThemedScaffold
import com.vitoroliveira.gymtracker.ui.theme.ThemedTopAppBar
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Share
import androidx.compose.ui.platform.LocalContext
import com.vitoroliveira.gymtracker.presentation.social.formatInt
import com.vitoroliveira.gymtracker.presentation.workout.WorkoutSummary
import com.vitoroliveira.gymtracker.ui.utils.formatKg
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.vitoroliveira.gymtracker.data.model.WorkoutSet
import com.vitoroliveira.gymtracker.presentation.viewmodel.ExerciseSetGroup
import com.vitoroliveira.gymtracker.presentation.viewmodel.WorkoutDetailUiState
import com.vitoroliveira.gymtracker.presentation.viewmodel.WorkoutDetailViewModel
import com.vitoroliveira.gymtracker.ui.theme.GymTrackerTheme
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun WorkoutDetailScreen(
    workoutId: String,
    groupName: String = "",
    onBack: () -> Unit = {},
    viewModel: WorkoutDetailViewModel = koinViewModel(parameters = { parametersOf(workoutId) })
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    WorkoutDetailScreenContent(
        uiState = uiState,
        groupName = groupName,
        onShare = {
            val send = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(android.content.Intent.EXTRA_TEXT, viewModel.shareText(groupName))
            }
            context.startActivity(android.content.Intent.createChooser(send, "Compartilhar treino"))
        },
        onSaveNotes = viewModel::saveNotes,
        onBack = onBack,
        onDismissError = viewModel::clearError
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutDetailScreenContent(
    uiState: WorkoutDetailUiState,
    groupName: String = "",
    onShare: () -> Unit = {},
    onSaveNotes: (String) -> Unit = {},
    onBack: () -> Unit = {},
    onDismissError: () -> Unit = {}
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var editingNotes by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.errorMessage, uiState.successMessage) {
        (uiState.errorMessage ?: uiState.successMessage)?.let {
            snackbarHostState.showSnackbar(it)
            onDismissError()
        }
    }
    if (editingNotes) {
        NotesDialog(
            initial = uiState.workout?.notes.orEmpty(),
            onSave = { onSaveNotes(it); editingNotes = false },
            onDismiss = { editingNotes = false }
        )
    }

    ThemedScaffold(
        topBar = {
            ThemedTopAppBar(
                title = { Text(groupName.ifBlank { "Detalhe do Treino" }) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    IconButton(onClick = onShare, enabled = !uiState.isLoading && uiState.exerciseGroups.isNotEmpty()) {
                        Icon(Icons.Default.Share, contentDescription = "Compartilhar treino")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                uiState.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    uiState.summary?.let { summary -> item(key = "summary") { SummaryCard(summary) } }
                    item(key = "notes") {
                        NotesCard(uiState.workout?.notes, saving = uiState.isSavingNotes, onEdit = { editingNotes = true })
                    }
                    if (uiState.exerciseGroups.isEmpty()) item(key = "empty") { EmptyDetailState(Modifier.fillMaxWidth()) }
                    items(uiState.exerciseGroups, key = { it.exerciseId }) { group ->
                        ExerciseSetGroupCard(group)
                    }
                }
            }
        }
    }
}

@Composable
private fun ExerciseSetGroupCard(group: ExerciseSetGroup) {
    ThemedCard(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium) {
        Column(Modifier.padding(16.dp)) {
            Text(
                group.exerciseName,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(8.dp))

            Row(Modifier.fillMaxWidth()) {
                Text("Série", Modifier.weight(1f), style = MaterialTheme.typography.labelMedium)
                Text("Reps", Modifier.weight(1f), style = MaterialTheme.typography.labelMedium)
                Text("Carga", Modifier.weight(1f), style = MaterialTheme.typography.labelMedium)
            }
            HorizontalDivider(Modifier.padding(vertical = 4.dp))

            group.sets.forEach { set -> SetRow(set) }
        }
    }
}

@Composable
private fun SetRow(set: WorkoutSet) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("${set.setNumber}", Modifier.weight(1f))
        Text("${set.reps}", Modifier.weight(1f))
        Text(if (set.weightKg > 0) "${formatKg(set.weightKg)} kg" else "—", Modifier.weight(1f))
    }
}

/** Data, duração, séries e volume do treino. */
@Composable
private fun SummaryCard(s: WorkoutSummary) {
    ThemedCard(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                listOfNotNull(s.date, s.durationMinutes?.let { "$it min" }).joinToString(" · ").ifEmpty { "Treino" },
                style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold
            )
            Row(Modifier.fillMaxWidth()) {
                SummaryStat("${s.sets}", if (s.sets == 1) "série" else "séries", Modifier.weight(1f))
                SummaryStat("${s.reps}", "repetições", Modifier.weight(1f))
                SummaryStat(formatInt(s.volumeKg), "kg de volume", Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun SummaryStat(value: String, label: String, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Anotação livre do treino (como foi, dor, energia...). */
@Composable
private fun NotesCard(notes: String?, saving: Boolean, onEdit: () -> Unit) {
    ThemedCard(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium, onClick = onEdit) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Anotação", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    notes?.takeIf { it.isNotBlank() } ?: "Toque para anotar como foi o treino",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (notes.isNullOrBlank()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                )
            }
            if (saving) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            else Icon(Icons.Default.Edit, contentDescription = "Editar anotação", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun NotesDialog(initial: String, onSave: (String) -> Unit, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Anotação do treino") },
        text = {
            OutlinedTextField(
                value = text, onValueChange = { text = it.take(1000) },
                placeholder = { Text("Ex.: dormi mal, ombro incomodou, bati recorde no supino") },
                modifier = Modifier.fillMaxWidth(), minLines = 3
            )
        },
        confirmButton = { Button(onClick = { onSave(text) }) { Text("Salvar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
private fun EmptyDetailState(modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Nenhuma série registrada", style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(
            "Esse treino não teve séries registradas.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Preview
@Composable
private fun WorkoutDetailScreenPreview() {
    GymTrackerTheme {
        WorkoutDetailScreenContent(
            uiState = WorkoutDetailUiState(
                exerciseGroups = listOf(
                    ExerciseSetGroup(
                        exerciseId = "1",
                        exerciseName = "Supino Reto",
                        sets = listOf(
                            WorkoutSet(id = "s1", workoutId = "w1", exerciseId = "1", setNumber = 1, reps = 12, weightKg = 40.0),
                            WorkoutSet(id = "s2", workoutId = "w1", exerciseId = "1", setNumber = 2, reps = 10, weightKg = 42.5)
                        )
                    )
                )
            )
        )
    }
}