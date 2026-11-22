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
import androidx.compose.animation.animateContentSize
import androidx.compose.material3.Button
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.filled.PlayArrow
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

/** 45 → "45s", 60 → "1min", 90 → "1min30", 120 → "2min" (antes 60 e 90 apareciam os dois como "1min"). */
fun restLabel(seconds: Int): String = when {
    seconds < 60 -> "${seconds}s"
    seconds % 60 == 0 -> "${seconds / 60}min"
    else -> "${seconds / 60}min${seconds % 60}"
}

@Composable
fun ActiveWorkoutScreen(
    muscleGroupId: String?,
    muscleGroupName: String?,
    onWorkoutFinished: () -> Unit,
    onBack: () -> Unit = {},
    onAddExercises: (() -> Unit)? = null,
    /** Vindo de "Retomar" na Home: continua o treino aberto direto, sem o diálogo de escolha. */
    autoResume: Boolean = false,
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
    if (autoResume) {
        LaunchedEffect(uiState.activeWorkoutToRecover) {
            uiState.activeWorkoutToRecover?.let { viewModel.resumeWorkout(it) }
        }
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
    // Notificação viva do treino (#357): cronômetro na barra/bloqueio enquanto o treino está aberto.
    val ongoingNotifier = remember { WorkoutOngoingNotifier(context) }
    LaunchedEffect(uiState.activeWorkout?.id, uiState.summary != null) {
        val w = uiState.activeWorkout
        if (w != null && uiState.summary == null) ongoingNotifier.show(w.startedAt, muscleGroupName) else if (uiState.summary != null) ongoingNotifier.cancel()
    }
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
            onAddExercises = onAddExercises
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
    onAddExercises: (() -> Unit)? = null,
    elapsedSeconds: Long = 0,
    onUpdateSet: (setId: String, reps: Int, weightKg: Double) -> Unit = { _, _, _ -> },
    onDeleteSet: (setId: String) -> Unit = {},
    onSummaryDone: () -> Unit = {},
    onShareSummary: () -> Unit = {},
    topSlot: @Composable () -> Unit = {}
) {
    var showFinishDialog by remember { mutableStateOf(false) }
    // Supersérie (#575): ligação entre exercícios guardada no aparelho; em grupo o descanso só vem no fim da rodada.
    val appCtx = androidx.compose.ui.platform.LocalContext.current
    val workoutKey = uiState.activeWorkout?.id
    var links by remember(workoutKey) { mutableStateOf(SupersetStore.load(appCtx, workoutKey)) }
    val exerciseIds = uiState.exercises.mapNotNull { it.id }
    val rounds = remember(exerciseIds, links) { com.vitoroliveira.gymtracker.presentation.workout.supersetGroups(exerciseIds, links) }
    var roundNextId by remember { mutableStateOf<String?>(null) }
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    var roundSetCount by remember { mutableIntStateOf(uiState.completedSets.size) }
    LaunchedEffect(uiState.completedSets.size) {
        if (uiState.completedSets.size > roundSetCount) {
            val next = uiState.completedSets.lastOrNull()?.let { com.vitoroliveira.gymtracker.presentation.workout.nextInRound(rounds, it.exerciseId) }
            roundNextId = next
            val idx = uiState.exercises.indexOfFirst { it.id == next }
            if (next != null && idx >= 0) listState.animateScrollToItem(1 + idx) // item 0 = topSlot
        }
        roundSetCount = uiState.completedSets.size
    }
    // Vibração (#365): série registrada e recorde. Ignora a primeira composição (séries já existentes ao abrir/retomar).
    val hapticContext = androidx.compose.ui.platform.LocalContext.current
    var lastSetCount by remember { mutableIntStateOf(uiState.completedSets.size) }
    LaunchedEffect(uiState.completedSets.size) {
        if (uiState.completedSets.size > lastSetCount) com.vitoroliveira.gymtracker.ui.components.Haptics.setLogged(hapticContext)
        lastSetCount = uiState.completedSets.size
    }
    var lastRecordCount by remember { mutableIntStateOf(uiState.newRecordExerciseIds.size) }
    LaunchedEffect(uiState.newRecordExerciseIds.size) {
        if (uiState.newRecordExerciseIds.size > lastRecordCount) com.vitoroliveira.gymtracker.ui.components.Haptics.record(hapticContext)
        lastRecordCount = uiState.newRecordExerciseIds.size
    }

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
                            muscleGroupName?.let { com.vitoroliveira.gymtracker.ui.components.prettyGroupName(it) },
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
                        modifier = Modifier.padding(end = 16.dp),
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
            com.vitoroliveira.gymtracker.ui.components.ListSkeleton(Modifier.padding(padding), rows = 3, rowHeight = 240.dp)
        } else {
            val isFocus = com.vitoroliveira.gymtracker.ui.theme.LocalThemeExtras.current.workoutLayout ==
                com.vitoroliveira.gymtracker.ui.theme.WorkoutLayout.FOCUS
            Box(Modifier.fillMaxSize()) {
            if (isFocus) {
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
            } else LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(
                    start = 16.dp, top = 16.dp, end = 16.dp,
                    // espaço para a ilha do descanso não cobrir o botão da última série
                    bottom = if (uiState.timerState.totalSeconds > 0) 104.dp else 16.dp
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { topSlot() }

                if (uiState.exercises.isEmpty() && uiState.activeWorkout != null) {
                    item {
                        EmptyExercisesHint(muscleGroupName, onAddExercises)
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
                        onToggleFinished = { exercise.id?.let { onToggleExerciseFinished(it) } },
                        groupTag = exercise.id?.let { com.vitoroliveira.gymtracker.presentation.workout.groupLabel(com.vitoroliveira.gymtracker.presentation.workout.groupOf(rounds, it).size) },
                        linkable = uiState.exercises.size > 1 && exercise.id != null && exercise.id != uiState.exercises.last().id,
                        linked = exercise.id in links,
                        onToggleLink = {
                            exercise.id?.let { id ->
                                links = com.vitoroliveira.gymtracker.presentation.workout.toggleLink(exerciseIds, links, id)
                                SupersetStore.save(appCtx, workoutKey, links)
                                roundNextId = null
                            }
                        },
                        skipRest = exercise.id?.let { com.vitoroliveira.gymtracker.presentation.workout.nextInRound(rounds, it) } != null,
                        isUpNext = exercise.id != null && exercise.id == roundNextId
                    )
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
            if (!isFocus) RestIsland(
                timer = uiState.timerState,
                onPause = onPauseTimer,
                onResume = onResumeTimer,
                onCancel = onCancelTimer,
                modifier = Modifier.align(Alignment.BottomCenter).padding(padding).padding(bottom = 16.dp)
            )
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
            text = {
                Text(
                    "Você começou um treino " + com.vitoroliveira.gymtracker.ui.components.timeAgoText(uiState.activeWorkoutToRecover!!.startedAt) +
                        ". Continue de onde parou ou descarte-o e comece outro."
                )
            },
            confirmButton = {
                Button(onClick = { onResumeWorkout(uiState.activeWorkoutToRecover!!) }) { Text("Continuar") }
            },
            dismissButton = {
                TextButton(onClick = onDiscardWorkout) { Text("Descartar e iniciar novo") }
            }
        )
    }

    uiState.summary?.let { summary ->
        WorkoutSummaryDialog(summary = summary, onDone = onSummaryDone, onShare = onShareSummary, groupName = muscleGroupName)
    }
}

@Composable
private fun EmptyExercisesHint(groupName: String?, onAdd: (() -> Unit)?) {
    ThemedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Nenhum exercício aqui ainda", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(6.dp))
            Text(
                text = if (groupName != null) "Cadastre exercícios no grupo \"$groupName\" pra eles aparecerem no treino."
                else "Cadastre exercícios pra eles aparecerem no treino.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            if (onAdd != null) {
                Spacer(Modifier.height(14.dp))
                Button(onClick = onAdd) { Text("Adicionar exercícios") }
            }
        }
    }
}

/**
 * Ilha do descanso: pílula flutuante no pé da tela (alcance do polegar) que acompanha a rolagem.
 * Ideia do Dynamic Island (Skiper/Cult UI): entra com a mola do original (rigidez 400, amortecimento 30,
 * massa 1 → razão 0,75) e troca de tamanho suavemente ao concluir. Saída mais sutil que a entrada.
 */
@Composable
fun RestIsland(
    timer: TimerState,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val visible = timer.totalSeconds > 0
    val islandContext = androidx.compose.ui.platform.LocalContext.current
    val reduceMotion = remember(islandContext) { !com.vitoroliveira.gymtracker.ui.theme.animationsEnabled(islandContext) }
    androidx.compose.animation.AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = if (reduceMotion) androidx.compose.animation.EnterTransition.None else androidx.compose.animation.slideInVertically(
            androidx.compose.animation.core.spring(dampingRatio = 0.75f, stiffness = 400f)
        ) { it } + androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(150)),
        exit = if (reduceMotion) androidx.compose.animation.ExitTransition.None else androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(120)) +
            androidx.compose.animation.slideOutVertically(androidx.compose.animation.core.tween(120)) { it / 3 }
    ) {
        val done = timer.isFinished
        val cs = MaterialTheme.colorScheme
        val container = if (done) cs.tertiaryContainer else cs.primary
        val content = if (done) cs.onTertiaryContainer else cs.onPrimary
        androidx.compose.material3.Surface(
            shape = androidx.compose.foundation.shape.CircleShape,
            color = container,
            contentColor = content,
            shadowElevation = 8.dp,
            modifier = Modifier.animateContentSize(
                androidx.compose.animation.core.spring(dampingRatio = 0.75f, stiffness = 400f)
            )
        ) {
            Row(
                Modifier.padding(start = 18.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                androidx.compose.material3.CircularProgressIndicator(
                    progress = { timer.progress },
                    modifier = Modifier.size(26.dp),
                    color = content,
                    trackColor = content.copy(alpha = 0.25f),
                    strokeWidth = 3.dp
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = if (done) "Bora pra próxima série!" else timer.formattedTime,
                    style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.semantics { liveRegion = androidx.compose.ui.semantics.LiveRegionMode.Polite }
                )
                if (!done) {
                    IconButton(onClick = if (timer.isRunning) onPause else onResume) {
                        Icon(
                            if (timer.isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (timer.isRunning) "Pausar descanso" else "Retomar descanso"
                        )
                    }
                }
                IconButton(onClick = onCancel) {
                    Icon(Icons.Default.Close, contentDescription = if (done) "Fechar aviso de descanso" else "Cancelar descanso")
                }
            }
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
    isSaving: Boolean = false,
    /** Supersérie (#575): "Bi-set"/"Tri-set"/"Circuito" quando ligado a outros; null fora de grupo. */
    groupTag: String? = null,
    linkable: Boolean = false,
    linked: Boolean = false,
    onToggleLink: () -> Unit = {},
    /** No meio de uma supersérie: registrar a série não inicia o descanso (só no último da rodada). */
    skipRest: Boolean = false,
    /** É o próximo exercício da rodada ("Agora"). */
    isUpNext: Boolean = false
) {
    val tempoCtx = androidx.compose.ui.platform.LocalContext.current
    var tempo by remember { mutableStateOf(TempoStore.getDefault(tempoCtx, exercise.id)) }
    var reps by remember { mutableStateOf(exercise.defaultReps.toString()) }
    var weight by remember { mutableStateOf("") }
    var weightError by remember { mutableStateOf(false) }
    var repsTouched by remember { mutableStateOf(false) }
    var showTimerOptions by remember { mutableStateOf(false) }
    // Descanso lembrado entre treinos: ao registrar a série ele começa sozinho (pesquisa de UX: não exigir toque extra).
    val restPrefs = androidx.compose.ui.platform.LocalContext.current
        .getSharedPreferences("gymtracker_prefs", android.content.Context.MODE_PRIVATE)
    var rememberedRest by remember { mutableIntStateOf(restPrefs.getInt("rest_seconds", 0)) }

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
        if (completedSets.size > seenCount) {
            // cadência digitada vale para a série que acabou de entrar (a mais nova deste exercício)
            com.vitoroliveira.gymtracker.presentation.workout.parseTempo(tempo)?.let { t ->
                completedSets.maxByOrNull { it.setNumber }?.let { TempoStore.putForSet(tempoCtx, it.id, com.vitoroliveira.gymtracker.presentation.workout.tempoText(t)) }
            }
            if (!skipRest) {
                showTimerOptions = true
                if (rememberedRest > 0) onStartTimer(rememberedRest)
            }
        }
        seenCount = completedSets.size
    }

    val successColor = LocalThemeExtras.current.success
    val borderStroke = if (isFinished) BorderStroke(2.dp, successColor) else if (isUpNext) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else if (groupTag != null) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null

    ThemedCard(
        modifier = Modifier.fillMaxWidth(),
        border = borderStroke
    ) {
        // animateContentSize: quando a sugestão aparece/some o cartão cresce/encolhe suave,
        // em vez de empurrar os botões de um frame para outro (causava toque no lugar errado).
        Column(modifier = Modifier.fillMaxWidth().animateContentSize().padding(16.dp)) {
            if (isUpNext || groupTag != null) {
                Text(
                    text = (if (isUpNext) "AGORA · " else "") + (groupTag ?: ""),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
            }
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
                        val rpeContext = androidx.compose.ui.platform.LocalContext.current
                        val savedRpe = RpeStore.get(rpeContext, set.id)
                        Text(
                            text = "${set.reps} reps × ${formatKg(set.weightKg)} kg" + (if (savedRpe > 0) " · RPE $savedRpe" else "") +
                                (TempoStore.getForSet(rpeContext, set.id)?.let { com.vitoroliveira.gymtracker.presentation.workout.parseTempo(it) }
                                    ?.let { " · TUT ${com.vitoroliveira.gymtracker.presentation.workout.formatTut(com.vitoroliveira.gymtracker.presentation.workout.tutSeconds(set.reps, it))}" } ?: ""),
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

            Spacer(modifier = Modifier.height(8.dp))
            // ajuste rápido sem teclado: repetições ±1 à esquerda, carga ±2,5 kg à direita (alinhados com os campos de cima)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                com.vitoroliveira.gymtracker.ui.components.QuickSteps(
                    minusLabel = "−1", plusLabel = "+1",
                    minusDescription = "Menos uma repetição", plusDescription = "Mais uma repetição",
                    onMinus = { reps = com.vitoroliveira.gymtracker.ui.utils.stepRepsText(reps, -1); repsTouched = true },
                    onPlus = { reps = com.vitoroliveira.gymtracker.ui.utils.stepRepsText(reps, 1); repsTouched = true },
                    modifier = Modifier.weight(1f),
                )
                com.vitoroliveira.gymtracker.ui.components.QuickSteps(
                    minusLabel = "−2,5", plusLabel = "+2,5",
                    minusDescription = "Menos 2,5 quilos", plusDescription = "Mais 2,5 quilos",
                    onMinus = { weight = com.vitoroliveira.gymtracker.ui.utils.stepKgText(weight, -2.5); weightError = false },
                    onPlus = { weight = com.vitoroliveira.gymtracker.ui.utils.stepKgText(weight, 2.5); weightError = false },
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            val tempoInvalid = tempo.isNotBlank() && com.vitoroliveira.gymtracker.presentation.workout.parseTempo(tempo) == null
            OutlinedTextField(
                value = tempo,
                onValueChange = {
                    tempo = it.filter { c -> c.isDigit() || c == ' ' || c == '-' || c == 'x' || c == '.' }.take(9)
                    if (tempo.isBlank() || com.vitoroliveira.gymtracker.presentation.workout.parseTempo(tempo) != null) TempoStore.putDefault(tempoCtx, exercise.id, tempo)
                },
                label = { Text("Cadência (opcional, ex.: 3-1-2-0)") },
                isError = tempoInvalid,
                supportingText = if (tempoInvalid) ({ Text("4 dígitos: descida, pausa embaixo, subida, pausa em cima") }) else null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            if (linkable) {
                Spacer(modifier = Modifier.height(4.dp))
                FilterChip(
                    selected = linked,
                    onClick = onToggleLink,
                    label = { Text(if (linked) "Ligado ao próximo (supersérie)" else "Ligar ao próximo (supersérie / bi-set)") }
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

            if (showTimerOptions && completedSets.isNotEmpty()) {
                val lastSet = completedSets.last()
                val rpeContext = androidx.compose.ui.platform.LocalContext.current
                var rpe by remember(lastSet.id) { mutableIntStateOf(RpeStore.get(rpeContext, lastSet.id)) }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = if (rpe > 0) "Esforço da série ${completedSets.size}: ${RpeStore.describe(rpe)}" else "Quanto esforço foi a série ${completedSets.size}? (opcional)",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                // Degraus 6..10 no estilo "hyaline-level" (OriginKit): arraste e o nome rola sobre o cursor
                com.vitoroliveira.gymtracker.ui.components.HyalineLevel(
                    names = listOf("6 · leve", "7 · moderada", "8 · forte", "9 · muito forte", "10 · no limite"),
                    selected = if (rpe > 0) rpe - 6 else -1,
                    onSelect = { s -> rpe = s + 6; RpeStore.put(rpeContext, lastSet.id, s + 6) },
                    startCaption = "Leve", endCaption = "Falha",
                    description = "Esforço da série"
                )
            }

            if (showTimerOptions) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = if (rememberedRest > 0) "Descanso automático. Toque para mudar:" else "Iniciar descanso:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    restOptions.forEach { seconds ->
                        val label = restLabel(seconds)
                        FilterChip(
                            selected = seconds == rememberedRest,
                            onClick = {
                                rememberedRest = seconds
                                restPrefs.edit().putInt("rest_seconds", seconds).apply()
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
fun WorkoutSummaryDialog(summary: WorkoutSummary, onDone: () -> Unit, onShare: (() -> Unit)? = null, groupName: String? = null) {
    val shareContext = androidx.compose.ui.platform.LocalContext.current
    val cs = MaterialTheme.colorScheme
    val colors = ShareColors(
        bgTop = cs.background.toArgb(), bgBottom = cs.surfaceContainerHighest.compositeOver(cs.background).toArgb(),
        text = cs.onBackground.toArgb(), muted = cs.onSurfaceVariant.toArgb(),
        accent = cs.primary.toArgb(), onAccent = cs.onPrimary.toArgb()
    )
    val date = java.time.LocalDate.now().let { "%02d/%02d/%d".format(it.dayOfMonth, it.monthValue, it.year) }
            val shareStats = ShareStats(
                title = groupName?.takeIf { it.isNotBlank() }?.let { com.vitoroliveira.gymtracker.ui.components.prettyGroupName(it) } ?: "Treino livre",
                dateText = date,
                subtitle = "Treino concluído",
                blocks = listOf(
                    "DURAÇÃO" to "${summary.durationMinutes} min",
                    "SÉRIES" to "${summary.totalSets}",
                    "VOLUME" to formatVolume(summary.totalVolumeKg)
                ),
                records = summary.newRecords,
                footer = "${summary.totalReps} repetições · feito no GymTracker"
            )
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
                // #355: o treino vira uma imagem vertical nas cores do tema, pronta para Stories/WhatsApp
                OutlinedButton(
                    onClick = {
                        runCatching { shareContext.shareWorkoutImage(renderWorkoutImage(shareStats, colors)) }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Compartilhar como imagem") }
                PhotoShareButton(shareStats, colors)
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
