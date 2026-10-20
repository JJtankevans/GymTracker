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
    onBack: () -> Unit = {},
    viewModel: WorkoutDetailViewModel = koinViewModel(parameters = { parametersOf(workoutId) })
) {
    val uiState by viewModel.uiState.collectAsState()

    WorkoutDetailScreenContent(
        uiState = uiState,
        onBack = onBack,
        onDismissError = viewModel::clearError
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutDetailScreenContent(
    uiState: WorkoutDetailUiState,
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
                title = { Text("Detalhe do Treino") },
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
                uiState.exerciseGroups.isEmpty() -> EmptyDetailState(Modifier.align(Alignment.Center))
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
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
        Text("${set.weightKg} kg", Modifier.weight(1f))
    }
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