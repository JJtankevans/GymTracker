package com.vitoroliveira.gymtracker.ui.workout

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.vitoroliveira.gymtracker.data.model.Exercise
import com.vitoroliveira.gymtracker.data.model.ExerciseBaseline
import com.vitoroliveira.gymtracker.data.model.Workout
import com.vitoroliveira.gymtracker.data.model.WorkoutSet
import com.vitoroliveira.gymtracker.presentation.TimerState
import com.vitoroliveira.gymtracker.presentation.viewmodel.WorkoutSummary
import com.vitoroliveira.gymtracker.presentation.viewmodel.WorkoutUiState
import com.vitoroliveira.gymtracker.presentation.viewmodel.WorkoutViewModel
import com.vitoroliveira.gymtracker.ui.theme.GymTrackerTheme
import com.vitoroliveira.gymtracker.ui.theme.LocalThemeExtras
import com.vitoroliveira.gymtracker.ui.theme.ThemedCard
import com.vitoroliveira.gymtracker.ui.theme.ThemedScaffold
import com.vitoroliveira.gymtracker.ui.theme.ThemedTopAppBar
import com.vitoroliveira.gymtracker.ui.utils.formatElapsed
import com.vitoroliveira.gymtracker.ui.utils.formatKg
import com.vitoroliveira.gymtracker.ui.utils.formatVolume
import com.vitoroliveira.gymtracker.ui.utils.formatWeightInput
import com.vitoroliveira.gymtracker.ui.utils.restFinishedFeedback
import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.Lifecycle
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
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
    val elapsed by viewModel.elapsedSeconds.collectAsState()
    val context = LocalContext.current
    val view = LocalView.current


    // Inicia o treino ao abrir a tela (o ViewModel ignora chamadas repetidas, ex.: ao girar a tela)
    LaunchedEffect(Unit) {
        viewModel.startWorkout(muscleGroupId)
    }

    // Tela ligada durante o treino: ninguém quer desbloquear com a mão suada entre as séries.
    DisposableEffect(view) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }

    // Descanso: bipe/vibração no fim e notificação com contagem regressiva, também com a tela bloqueada.
    // Coleta o estado direto do ViewModel (e não por recomposição): com o app em segundo plano a tela não recompõe,
    // mas esta coleta continua, então o aviso de fim chega mesmo fora do app.
    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    val restNotifier = remember { RestNotifier(context) }
    var askedNotificationPermission by rememberSaveable { mutableStateOf(false) }
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(viewModel) {
        var previous: TimerState? = null
        viewModel.uiState.map { it.timerState }.distinctUntilChanged().collect { t ->
            val prev = previous
            previous = t
            val visible = lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
            when {
                t.isFinished && prev?.isFinished != true -> {
                    context.restFinishedFeedback()
                    if (visible) restNotifier.cancel() else restNotifier.showFinished()
                }
                t.isRunning && (prev == null || !prev.isRunning || t.totalSeconds != prev.totalSeconds || t.remainingSeconds > prev.remainingSeconds) -> {
                    if (Build.VERSION.SDK_INT >= 33 && !restNotifier.canNotify() && !askedNotificationPermission) {
                        askedNotificationPermission = true
                        notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    restNotifier.showCountdown(t.remainingSeconds)
                }
                !t.isRunning && !t.isFinished -> restNotifier.cancel() // pausado ou cancelado
            }
        }
    }
    DisposableEffect(Unit) { onDispose { restNotifier.cancel() } }

    androidx.compose.foundation.layout.Box {
        ActiveWorkoutScreenContent(
            muscleGroupName = muscleGroupName,
            uiState = uiState,
            elapsedSeconds = elapsed,
            onAddSet = { exerciseId, reps, weight -> viewModel.addSet(exerciseId, reps, weight) },
            onUpdateSet = { id, reps, weight -> viewModel.updateSet(id, reps, weight) },
            onDeleteSet = { viewModel.deleteSet(it) },
            onStartTimer = { viewModel.startTimer(it) },
            onPauseTimer = { viewModel.pauseTimer() },
            onResumeTimer = { viewModel.resumeTimer() },
            onCancelTimer = { viewModel.cancelTimer() },
            onFinishWorkout = { viewModel.finishWorkout() },
            onClearError = { viewModel.clearError() },
            onResumeWorkout = { viewModel.resumeWorkout(it) },
            onDiscardWorkout = { viewModel.discardAndStartNew(muscleGroupId) },
            onToggleExerciseFinished = { viewModel.toggleExerciseFinished(it) },
            onSummaryDone = onWorkoutFinished,
            onShareSummary = {
                val send = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(android.content.Intent.EXTRA_TEXT, viewModel.shareText(muscleGroupName))
                }
                context.startActivity(android.content.Intent.createChooser(send, "Compartilhar treino"))
            },
            onBack = onBack,
        )
        // explosão de confete ao bater recorde de carga
        RecordCelebration(uiState.recordBursts)
    }
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
    onResumeWorkout: (Workout) -> Unit,
    onDiscardWorkout: () -> Unit,
    onToggleExerciseFinished: (String) -> Unit,
    onBack: () -> Unit = {},
    elapsedSeconds: Long = 0,
    onUpdateSet: (setId: String, reps: Int, weightKg: Double) -> Unit = { _, _, _ -> },
    onDeleteSet: (setId: String) -> Unit = {},
    onSummaryDone: () -> Unit = {},
    onShareSummary: () -> Unit = {},
    topSlot: @Composable () -> Unit = {}
) {
    var showFinishDialog by remember { mutableStateOf(false) }
    var editingSet by remember { mutableStateOf<WorkoutSet?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            onClearError()
        }
    }

    ThemedScaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            ThemedTopAppBar(
                title = {
                    Column {
                        Text("Treino Ativo")
                        val subtitle = listOfNotNull(
                            muscleGroupName,
                            if (uiState.activeWorkout != null) formatElapsed(elapsedSeconds) else null
                        ).joinToString("  ·  ")
                        if (subtitle.isNotEmpty()) {
                            Text(
                                text = subtitle,
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
                        enabled = uiState.activeWorkout != null && !uiState.isWorkoutFinished,
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

        if (uiState.isLoading && uiState.activeWorkout == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            if (com.vitoroliveira.gymtracker.ui.theme.LocalThemeExtras.current.workoutLayout ==
                com.vitoroliveira.gymtracker.ui.theme.WorkoutLayout.FOCUS) {
                FocusWorkoutBody(
                    uiState = uiState,
                    onAddSet = onAddSet,
                    onStartTimer = onStartTimer,
                    onCancelTimer = onCancelTimer,
                    onToggleExerciseFinished = onToggleExerciseFinished,
                    onEditSet = { editingSet = it },
                    modifier = Modifier.padding(padding),
                    topSlot = topSlot
                )
            } else
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { topSlot() }

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

                if (uiState.exercises.isEmpty() && uiState.activeWorkout != null) {
                    item {
                        EmptyExercisesHint(muscleGroupName)
                    }
                }

                items(uiState.exercises, key = { it.id ?: it.name }) { exercise ->
                    val setsForExercise = uiState.completedSets.filter { it.exerciseId == exercise.id }
                    val isFinished = uiState.activeWorkout?.finishedExerciseIds?.contains(exercise.id) == true

                    ExerciseWorkoutCard(
                        exercise = exercise,
                        completedSets = setsForExercise,
                        isFinished = isFinished,
                        baseline = exercise.id?.let { uiState.baselines[it] },
                        hasNewRecord = exercise.id in uiState.newRecordExerciseIds,
                        isSaving = exercise.id in uiState.savingExerciseIds,
                        onAddSet = { reps, weight -> exercise.id?.let { onAddSet(it, reps, weight) } },
                        onEditSet = { editingSet = it },
                        onStartTimer = onStartTimer,
                        onToggleFinished = { exercise.id?.let { onToggleExerciseFinished(it) } }
                    )
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }

    if (showFinishDialog) {
        val sets = uiState.completedSets.size
        AlertDialog(
            onDismissRequest = { showFinishDialog = false },
            title = { Text("Finalizar treino") },
            text = {
                Text(
                    if (sets == 0) "Você ainda não registrou nenhuma série. Deseja finalizar mesmo assim?"
                    else "Você registrou $sets ${if (sets == 1) "série" else "séries"}. Deseja finalizar o treino?"
                )
            },
            confirmButton = {
                Button(onClick = {
                    showFinishDialog = false
                    onFinishWorkout()
                }) { Text("Finalizar") }
            },
            dismissButton = {
                TextButton(onClick = { showFinishDialog = false }) { Text("Continuar treinando") }
            }
        )
    }

    editingSet?.let { set ->
        EditSetDialog(
            set = set,
            onSave = { reps, weight ->
                set.id?.let { onUpdateSet(it, reps, weight) }
                editingSet = null
            },
            onDelete = {
                set.id?.let(onDeleteSet)
                editingSet = null
            },
            onDismiss = { editingSet = null }
        )
    }

    if (uiState.activeWorkoutToRecover != null) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Treino em andamento") },
            text = { Text("Você tem um treino iniciado anteriormente. Deseja continuar ou iniciar um novo?") },
            confirmButton = {
                Button(onClick = { onResumeWorkout(uiState.activeWorkoutToRecover!!) }) { Text("Continuar") }
            },
            dismissButton = {
                TextButton(onClick = onDiscardWorkout) { Text("Iniciar Novo") }
            }
        )
    }

    uiState.summary?.let { summary ->
        WorkoutSummaryDialog(summary = summary, onDone = onSummaryDone, onShare = onShareSummary)
    }
}

@Composable
private fun EmptyExercisesHint(groupName: String?) {
    ThemedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Nenhum exercício aqui ainda", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(6.dp))
            Text(
                text = if (groupName != null) "Cadastre exercícios no grupo \"$groupName\" pra eles aparecerem no treino."
                else "Cadastre exercícios pra eles aparecerem no treino.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun TimerCard(
    timerState: TimerState,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit
) {
    val done = timerState.isFinished
    ThemedCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = if (done) MaterialTheme.colorScheme.tertiaryContainer
        else MaterialTheme.colorScheme.primaryContainer
    ) {
        val content = if (done) MaterialTheme.colorScheme.onTertiaryContainer
        else MaterialTheme.colorScheme.onPrimaryContainer
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (done) "Descanso concluído. Bora pra próxima série!" else "Descanso",
                style = MaterialTheme.typography.labelMedium,
                color = content
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = timerState.formattedTime,
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = content
            )

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { timerState.progress },
                modifier = Modifier.fillMaxWidth().height(8.dp),
                color = if (done) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onCancel) { Text(if (done) "Fechar" else "Cancelar") }

                if (!done) {
                    Button(onClick = if (timerState.isRunning) onPause else onResume) {
                        Text(if (timerState.isRunning) "Pausar" else "Retomar")
                    }
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
    onToggleFinished: () -> Unit,
    baseline: ExerciseBaseline? = null,
    hasNewRecord: Boolean = false,
    onEditSet: (WorkoutSet) -> Unit = {},
    /** Série deste exercício sendo gravada: o botão fica desligado até terminar (toque duplo não duplica). */
    isSaving: Boolean = false
) {
    var reps by remember { mutableStateOf(exercise.defaultReps.toString()) }
    var weight by remember { mutableStateOf("") }
    var weightError by remember { mutableStateOf(false) }
    var repsTouched by remember { mutableStateOf(false) }
    var showTimerOptions by remember { mutableStateOf(false) }

    val suggestion = com.vitoroliveira.gymtracker.ui.components.rememberLoadSuggestion(
        exercise.name, baseline?.lastWeightKg, baseline?.lastReps, exercise.defaultReps
    )
    // Pré-preenche com a última vez (sem sobrescrever o que o usuário já digitou)
    LaunchedEffect(baseline, suggestion?.weightKg) {
        val w = baseline?.lastWeightKg ?: suggestion?.weightKg
        w?.let { if (weight.isBlank()) weight = formatKg(it) }
        baseline?.lastReps?.let { if (!repsTouched) reps = it.toString() }
    }

    // Mostra as opções de descanso só quando uma série foi realmente salva
    var seenCount by remember { mutableIntStateOf(completedSets.size) }
    LaunchedEffect(completedSets.size) {
        if (completedSets.size > seenCount) showTimerOptions = true
        seenCount = completedSets.size
    }

    val successColor = LocalThemeExtras.current.success
    val borderStroke = if (isFinished) BorderStroke(2.dp, successColor) else null

    ThemedCard(
        modifier = Modifier.fillMaxWidth(),
        border = borderStroke
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
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
                    if (suggestion != null && formatKg(suggestion.weightKg) != weight) {
                        com.vitoroliveira.gymtracker.ui.components.LoadSuggestionChip(suggestion, onApply = { weight = formatKg(it) })
                    }
                    if (baseline?.lastWeightKg != null) {
                        Text(
                            text = "Última vez: ${formatKg(baseline.lastWeightKg!!)} kg × ${baseline.lastReps ?: "-"}" +
                                    (baseline.recordWeightKg?.let { "  ·  Recorde: ${formatKg(it)} kg" } ?: ""),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (completedSets.size >= exercise.defaultSets || isFinished) {
                    IconButton(
                        onClick = onToggleFinished,
                        colors = IconButtonDefaults.iconButtonColors(
                            contentColor = if (isFinished) successColor else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Icon(
                            imageVector = if (isFinished) Icons.Default.DoneAll else Icons.Default.Check,
                            contentDescription = "Finalizar exercício"
                        )
                    }
                }
            }

            if (hasNewRecord) {
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Novo recorde de carga!",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.tertiary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Séries já registradas (toque pra editar ou apagar)
            if (completedSets.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                completedSets.sortedBy { it.setNumber }.forEachIndexed { index, set ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onEditSet(set) }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Série ${index + 1}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "${set.reps} reps × ${formatKg(set.weightKg)} kg",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(Modifier.width(8.dp))
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Editar série ${index + 1}",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.height(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = reps,
                    onValueChange = { reps = it.filter(Char::isDigit).take(3); repsTouched = true },
                    label = { Text("Reps") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )

                var showPlates by remember { mutableStateOf(false) }
                if (showPlates) {
                    com.vitoroliveira.gymtracker.ui.exercise.PlateCalculatorSheet(
                        initialKg = weight.replace(",", ".").toDoubleOrNull(), onDismiss = { showPlates = false }
                    )
                }
                OutlinedTextField(
                    value = weight,
                    onValueChange = { weight = formatWeightInput(it); weightError = false },
                    label = { Text("Peso (kg)") },
                    trailingIcon = {
                        IconButton(onClick = { showPlates = true }) {
                            Icon(Icons.Default.Calculate, contentDescription = "Calculadora de anilhas")
                        }
                    },
                    isError = weightError,
                    supportingText = if (weightError) ({ Text("Informe o peso") }) else null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = {
                    val repsInt = reps.toIntOrNull()
                    val weightDouble = weight.replace(",", ".").toDoubleOrNull()
                    if (weightDouble == null) {
                        weightError = true
                        return@Button
                    }
                    if (repsInt == null) return@Button
                    onAddSet(repsInt, weightDouble)
                },
                enabled = !isSaving,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Registrar série ${completedSets.size + 1}")
            }

            if (showTimerOptions) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Iniciar descanso:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    restOptions.forEach { seconds ->
                        val label = if (seconds < 60) "${seconds}s" else "${seconds / 60}min"
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

@Composable
private fun EditSetDialog(
    set: WorkoutSet,
    onSave: (reps: Int, weightKg: Double) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    var reps by remember { mutableStateOf(set.reps.toString()) }
    var weight by remember { mutableStateOf(formatKg(set.weightKg)) }
    val repsInt = reps.toIntOrNull()
    val weightD = weight.replace(",", ".").toDoubleOrNull()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar série") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = reps,
                    onValueChange = { reps = it.filter(Char::isDigit).take(3) },
                    label = { Text("Reps") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                OutlinedTextField(
                    value = weight,
                    onValueChange = { weight = formatWeightInput(it) },
                    label = { Text("Peso (kg)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(repsInt!!, weightD!!) },
                enabled = repsInt != null && weightD != null
            ) { Text("Salvar") }
        },
        dismissButton = {
            Row {
                TextButton(
                    onClick = onDelete,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Apagar") }
                TextButton(onClick = onDismiss) { Text("Cancelar") }
            }
        }
    )
}

@Composable
fun WorkoutSummaryDialog(summary: WorkoutSummary, onDone: () -> Unit, onShare: (() -> Unit)? = null) {
    AlertDialog(
        onDismissRequest = { },
        title = { Text("Treino concluído!") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SummaryRow("Duração", "${summary.durationMinutes} min")
                SummaryRow("Exercícios", summary.exercisesWorked.toString())
                SummaryRow("Séries", summary.totalSets.toString())
                SummaryRow("Repetições", summary.totalReps.toString())
                SummaryRow("Volume total", formatVolume(summary.totalVolumeKg))
                if (summary.newRecords.isNotEmpty()) {
                    HorizontalDivider()
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(
                                if (summary.newRecords.size == 1) "Novo recorde em" else "Novos recordes em",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(summary.newRecords.joinToString(", "), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        },
        confirmButton = { Button(onClick = onDone) { Text("Concluir") } },
        dismissButton = onShare?.let { share -> { TextButton(onClick = share) { Text("Compartilhar") } } }
    )
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = FontWeight.Bold)
    }
}

private val previewExercises = listOf(
    Exercise(id = "1", name = "Supino Reto", defaultSets = 4, defaultReps = 10),
    Exercise(id = "2", name = "Crucifixo", defaultSets = 3, defaultReps = 12)
)

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun ActiveWorkoutScreenPreview() {
    GymTrackerTheme {
        ActiveWorkoutScreenContent(
            muscleGroupName = "Treino A",
            uiState = WorkoutUiState(
                activeWorkout = Workout(id = "w1"),
                exercises = previewExercises,
                completedSets = listOf(
                    WorkoutSet(id = "s1", workoutId = "w1", exerciseId = "1", setNumber = 1, reps = 10, weightKg = 80.0),
                    WorkoutSet(id = "s2", workoutId = "w1", exerciseId = "1", setNumber = 2, reps = 8, weightKg = 85.0)
                ),
                baselines = mapOf("1" to ExerciseBaseline(80.0, 10, 82.5)),
                newRecordExerciseIds = setOf("1"),
                timerState = TimerState(totalSeconds = 90, remainingSeconds = 62, isRunning = true)
            ),
            elapsedSeconds = 1432,
            onAddSet = { _, _, _ -> },
            onStartTimer = {}, onPauseTimer = {}, onResumeTimer = {}, onCancelTimer = {},
            onFinishWorkout = {}, onClearError = {}, onResumeWorkout = {}, onDiscardWorkout = {},
            onToggleExerciseFinished = {}, onBack = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun TimerCardPreview() {
    GymTrackerTheme {
        TimerCard(
            timerState = TimerState(totalSeconds = 90, remainingSeconds = 60, isRunning = true),
            onPause = {}, onResume = {}, onCancel = {}
        )
    }
}
