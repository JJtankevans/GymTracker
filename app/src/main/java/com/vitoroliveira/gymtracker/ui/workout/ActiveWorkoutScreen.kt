package com.vitoroliveira.gymtracker.ui.workout

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.vitoroliveira.gymtracker.data.model.Exercise
import com.vitoroliveira.gymtracker.data.model.WorkoutSet
import com.vitoroliveira.gymtracker.presentation.TimerState
import com.vitoroliveira.gymtracker.presentation.viewmodel.WorkoutUiState
import com.vitoroliveira.gymtracker.presentation.viewmodel.WorkoutViewModel
import com.vitoroliveira.gymtracker.ui.theme.GymTrackerTheme
import com.vitoroliveira.gymtracker.ui.theme.SuccessGreen
import com.vitoroliveira.gymtracker.ui.utils.formatWeightInput
import org.koin.compose.viewmodel.koinViewModel

// Opções de tempo de descanso em segundos
val restOptions = listOf(30, 45, 60, 90, 120)

@Composable
fun ActiveWorkoutScreen(
    muscleGroupId: String?,
    muscleGroupName: String?,
    onWorkoutFinished: () -> Unit,
    onBack: () -> Unit = {},
    viewModel: WorkoutViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Inicia o treino ao abrir a tela
    LaunchedEffect(Unit) {
        viewModel.startWorkout(muscleGroupId)
    }

    // Navega para fora quando o treino for finalizado
    LaunchedEffect(uiState.isWorkoutFinished) {
        if (uiState.isWorkoutFinished) onWorkoutFinished()
    }

    ActiveWorkoutScreenContent(
        muscleGroupName = muscleGroupName,
        uiState = uiState,
        onAddSet = { exerciseId, reps, weight ->
            viewModel.addSet(exerciseId, reps, weight)
        },
        onStartTimer = { viewModel.startTimer(it) },
        onPauseTimer = { viewModel.pauseTimer() },
        onResumeTimer = { viewModel.resumeTimer() },
        onCancelTimer = { viewModel.cancelTimer() },
        onFinishWorkout = { viewModel.finishWorkout() },
        onClearError = { viewModel.clearError() },
        onResumeWorkout = { viewModel.resumeWorkout(it) },
        onDiscardWorkout = { viewModel.discardAndStartNew(muscleGroupId) },
        onToggleExerciseFinished = { viewModel.toggleExerciseFinished(it) },
        onBack = onBack
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveWorkoutScreenContent(
    muscleGroupName: String?,
    uiState: WorkoutUiState,
    onAddSet: (exerciseId: String, reps: Int, weightKg: Double) -> Unit,
    onStartTimer: (Int) -> Unit,
    onPauseTimer: () -> Unit,
    onResumeTimer: () -> Unit,
    onCancelTimer: () -> Unit,
    onFinishWorkout: () -> Unit,
    onClearError: () -> Unit,
    onResumeWorkout: (com.vitoroliveira.gymtracker.data.model.Workout) -> Unit,
    onDiscardWorkout: () -> Unit,
    onToggleExerciseFinished: (String) -> Unit,
    onBack: () -> Unit = {}
) {
    var showFinishDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            onClearError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Treino Ativo")
                        if (muscleGroupName != null) {
                            Text(
                                text = muscleGroupName,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    Button(
                        onClick = { showFinishDialog = true },
                        modifier = Modifier.padding(end = 8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.tertiary
                        )
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Finalizar")
                    }
                }
            )
        }
    ) { padding ->

        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Temporizador fixo no topo da lista
                if (uiState.timerState.totalSeconds > 0) {
                    item {
                        TimerCard(
                            timerState = uiState.timerState,
                            onPause = onPauseTimer,
                            onResume = onResumeTimer,
                            onCancel = onCancelTimer
                        )
                    }
                }

                // Card de cada exercício
                items(uiState.exercises, key = { it.id ?: it.name }) { exercise ->
                    val setsForExercise = uiState.completedSets
                        .filter { it.exerciseId == exercise.id }
                    
                    val isFinished = uiState.activeWorkout?.finishedExerciseIds
                        ?.contains(exercise.id) == true

                    ExerciseWorkoutCard(
                        exercise = exercise,
                        completedSets = setsForExercise,
                        isFinished = isFinished,
                        onAddSet = { reps, weight ->
                            exercise.id?.let { onAddSet(it, reps, weight) }
                        },
                        onStartTimer = onStartTimer,
                        onToggleFinished = { exercise.id?.let { onToggleExerciseFinished(it) } }
                    )
                }

                // Espaço no final para o FAB não cobrir conteúdo
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }

    if (showFinishDialog) {
        AlertDialog(
            onDismissRequest = { showFinishDialog = false },
            title = { Text("Finalizar treino") },
            text = {
                Text(
                    "Você registrou ${uiState.completedSets.size} séries. " +
                            "Deseja finalizar o treino?"
                )
            },
            confirmButton = {
                Button(onClick = {
                    showFinishDialog = false
                    onFinishWorkout()
                }) {
                    Text("Finalizar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showFinishDialog = false }) {
                    Text("Continuar treinando")
                }
            }
        )
    }

    if (uiState.activeWorkoutToRecover != null) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Treino em andamento") },
            text = { Text("Você tem um treino iniciado anteriormente. Deseja continuar ou iniciar um novo?") },
            confirmButton = {
                Button(onClick = { onResumeWorkout(uiState.activeWorkoutToRecover!!) }) {
                    Text("Continuar")
                }
            },
            dismissButton = {
                TextButton(onClick = onDiscardWorkout) {
                    Text("Iniciar Novo")
                }
            }
        )
    }
}

@Composable
fun TimerCard(
    timerState: TimerState,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Descanso",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = timerState.formattedTime,
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { timerState.progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onCancel) {
                    Text("Cancelar")
                }

                Button(
                    onClick = if (timerState.isRunning) onPause else onResume
                ) {
                    Text(if (timerState.isRunning) "Pausar" else "Retomar")
                }
            }
        }
    }
}

@Composable
fun ExerciseWorkoutCard(
    exercise: Exercise,
    completedSets: List<WorkoutSet>,
    isFinished: Boolean,
    onAddSet: (reps: Int, weightKg: Double) -> Unit,
    onStartTimer: (Int) -> Unit,
    onToggleFinished: () -> Unit
) {
    var reps by remember { mutableStateOf(exercise.defaultReps.toString()) }
    var weight by remember { mutableStateOf("") }
    var showTimerOptions by remember { mutableStateOf(false) }

    val borderStroke = if (isFinished) {
        BorderStroke(2.dp, SuccessGreen)
    } else null

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        border = borderStroke
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Nome do exercício + Botão de finalizar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = exercise.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${exercise.defaultSets} séries × ${exercise.defaultReps} reps",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (completedSets.size >= exercise.defaultSets || isFinished) {
                    IconButton(
                        onClick = onToggleFinished,
                        colors = IconButtonDefaults.iconButtonColors(
                            contentColor = if (isFinished) SuccessGreen 
                                           else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Icon(
                            imageVector = if (isFinished) Icons.Default.DoneAll else Icons.Default.Check,
                            contentDescription = "Finalizar exercício"
                        )
                    }
                }
            }

            // Séries já registradas
            if (completedSets.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))

                completedSets.forEach { set ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Série ${set.setNumber}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${set.reps} reps × ${set.weightKg}kg",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            HorizontalDivider()

            Spacer(modifier = Modifier.height(12.dp))

            // Campos para registrar nova série
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = reps,
                    onValueChange = { reps = it },
                    label = { Text("Reps") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )

                OutlinedTextField(
                    value = weight,
                    onValueChange = { weight = formatWeightInput(it) },
                    label = { Text("Peso (kg)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Botão de adicionar série
            Button(
                onClick = {
                    val repsInt = reps.toIntOrNull() ?: return@Button
                    val weightDouble = weight.replace(",", ".").toDoubleOrNull() ?: return@Button
                    onAddSet(repsInt, weightDouble)
                    showTimerOptions = true
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Registrar série ${completedSets.size + 1}")
            }

            // Opções do temporizador após registrar série
            if (showTimerOptions) {
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Iniciar descanso:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    restOptions.forEach { seconds ->
                        val label = if (seconds < 60) "${seconds}s"
                        else "${seconds / 60}min"
                        FilterChip(
                            selected = false,
                            onClick = {
                                onStartTimer(seconds)
                                showTimerOptions = false
                            },
                            label = { Text(label) }
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun ActiveWorkoutScreenPreview() {
    GymTrackerTheme {
        ActiveWorkoutScreenContent(
            muscleGroupName = "Treino A",
            uiState = WorkoutUiState(
                exercises = listOf(
                    Exercise(
                        id = "1",
                        name = "Supino Reto",
                        defaultSets = 4,
                        defaultReps = 10
                    ),
                    Exercise(
                        id = "2",
                        name = "Crucifixo",
                        defaultSets = 3,
                        defaultReps = 12
                    )
                ),
                completedSets = listOf(
                    WorkoutSet(
                        workoutId = "w1",
                        exerciseId = "1",
                        setNumber = 1,
                        reps = 10,
                        weightKg = 80.0
                    )
                )
            ),
            onAddSet = { _, _, _ -> },
            onStartTimer = {},
            onPauseTimer = {},
            onResumeTimer = {},
            onCancelTimer = {},
            onFinishWorkout = {},
            onClearError = {},
            onResumeWorkout = {},
            onDiscardWorkout = {},
            onToggleExerciseFinished = {},
            onBack = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun TimerCardPreview() {
    GymTrackerTheme {
        TimerCard(
            timerState = TimerState(
                totalSeconds = 90,
                remainingSeconds = 60,
                isRunning = true
            ),
            onPause = {},
            onResume = {},
            onCancel = {}
        )
    }
}