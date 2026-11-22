package com.vitoroliveira.gymtracker.ui.home

import com.vitoroliveira.gymtracker.ui.theme.ThemedCard
import com.vitoroliveira.gymtracker.ui.components.BarChart
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.vitoroliveira.gymtracker.ui.theme.ThemedScaffold
import com.vitoroliveira.gymtracker.ui.theme.ThemedTopAppBar
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.toLocalDateTime
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.vitoroliveira.gymtracker.presentation.viewmodel.HomeUiState
import com.vitoroliveira.gymtracker.presentation.viewmodel.HomeViewModel
import com.vitoroliveira.gymtracker.ui.theme.GymTrackerTheme
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun HomeScreen(
    onNavigateToMuscleGroups: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onStartWorkoutClick: () -> Unit,
    onNavigateToThemes: () -> Unit = {},
    onNavigateToEvolution: () -> Unit = {},
    onNavigateToRecords: () -> Unit = {},
    onNavigateToSocial: () -> Unit = {},
    onStartGroup: (String, String) -> Unit = { _, _ -> },
    onResumeWorkout: (String?, String) -> Unit = { _, _ -> },
    viewModel: HomeViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Recarrega o dashboard toda vez que a Home volta a ficar visível
    // (ex: ao voltar de um treino finalizado).
    LaunchedEffect(Unit) {
        viewModel.loadDashboard()
    }

    // Agenda semanal (Grupos → calendário): se há grupo marcado para hoje, ele vira o "Próximo treino".
    val schedule: com.vitoroliveira.gymtracker.presentation.schedule.ScheduleRepository = org.koin.compose.koinInject()
    val plan by schedule.plan.collectAsState()
    val shown = remember(uiState, plan) {
        val zone = kotlinx.datetime.TimeZone.currentSystemDefault()
        val today = kotlin.time.Clock.System.now().toLocalDateTime(zone).date
        val planned = com.vitoroliveira.gymtracker.presentation.schedule.plannedGroupFor(plan, today.dayOfWeek.isoDayNumber, uiState.groupNames.keys)
        if (planned != null) uiState.copy(suggestedGroupId = planned, suggestedGroupName = uiState.groupNames[planned], suggestedFromSchedule = true) else uiState
    }

    val appContext = androidx.compose.ui.platform.LocalContext.current
    LaunchedEffect(uiState.hasActiveWorkout, uiState.activeWorkoutStartedAt, uiState.isLoading) {
        if (!uiState.isLoading) {
            val ongoing = com.vitoroliveira.gymtracker.ui.workout.WorkoutOngoingNotifier(appContext)
            if (uiState.hasActiveWorkout) ongoing.show(uiState.activeWorkoutStartedAt, uiState.activeWorkoutGroupName) else ongoing.cancel()
        }
    }

    // Widget da tela inicial: próximo treino e, se houver, o treino aberto (#357)
    LaunchedEffect(shown.suggestedGroupName, shown.hasActiveWorkout, shown.activeWorkoutStartedAt, shown.isLoading) {
        if (!shown.isLoading) com.vitoroliveira.gymtracker.ui.widget.WorkoutWidgetState.save(
            appContext,
            shown.suggestedGroupName?.let { com.vitoroliveira.gymtracker.ui.components.prettyGroupName(it) },
            if (shown.hasActiveWorkout) shown.activeWorkoutStartedAt else null,
            shown.activeWorkoutGroupName?.let { com.vitoroliveira.gymtracker.ui.components.prettyGroupName(it) }
        )
    }

    HomeScreenContent(
        uiState = shown,
        onNavigateToMuscleGroups = onNavigateToMuscleGroups,
        onNavigateToHistory = onNavigateToHistory,
        onNavigateToProfile = onNavigateToProfile,
        onStartWorkoutClick = onStartWorkoutClick,
        onNavigateToThemes = onNavigateToThemes,
        onNavigateToEvolution = onNavigateToEvolution,
        onNavigateToRecords = onNavigateToRecords,
        onNavigateToSocial = onNavigateToSocial,
        onStartGroup = onStartGroup,
        onResumeWorkout = onResumeWorkout,
        onClearError = { viewModel.clearError() }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreenContent(
    uiState: HomeUiState,
    onNavigateToMuscleGroups: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onStartWorkoutClick: () -> Unit,
    onClearError: () -> Unit,
    onNavigateToThemes: () -> Unit = {},
    onNavigateToEvolution: () -> Unit = {},
    onNavigateToRecords: () -> Unit = {},
    onNavigateToSocial: () -> Unit = {},
    onStartGroup: (String, String) -> Unit = { _, _ -> },
    onResumeWorkout: (String?, String) -> Unit = { _, _ -> },
    topSlot: @Composable () -> Unit = {}
) {
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
                title = { Text("GymTracker") },
                actions = {
                    IconButton(onClick = onNavigateToRecords) {
                        Icon(Icons.Default.EmojiEvents, contentDescription = "Recordes")
                    }
                    IconButton(onClick = onNavigateToSocial) {
                        BadgedBox(badge = {
                            if (uiState.pendingFollowRequests > 0) Badge { Text("${uiState.pendingFollowRequests}") }
                        }) {
                            Icon(
                                Icons.Default.Groups,
                                contentDescription = if (uiState.pendingFollowRequests > 0) "Social, ${uiState.pendingFollowRequests} pedidos para seguir" else "Social"
                            )
                        }
                    }
                    IconButton(onClick = onNavigateToProfile) {
                        Icon(Icons.Default.Person, contentDescription = "Perfil")
                    }
                    var menuOpen by remember { mutableStateOf(false) }
                    Box {
                        IconButton(onClick = { menuOpen = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Mais")
                        }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(
                                text = { Text("Evolução") },
                                leadingIcon = { Icon(Icons.Default.ShowChart, null) },
                                onClick = { menuOpen = false; onNavigateToEvolution() }
                            )
                            DropdownMenuItem(
                                text = { Text("Aparência") },
                                leadingIcon = { Icon(Icons.Default.Palette, null) },
                                onClick = { menuOpen = false; onNavigateToThemes() }
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (!com.vitoroliveira.gymtracker.ui.theme.LocalThemeExtras.current.homeLayout.hasOwnStart()) ExtendedFloatingActionButton(
                onClick = onStartWorkoutClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                icon = { Icon(Icons.Default.PlayArrow, contentDescription = null) },
                text = { Text(if (uiState.suggestedGroupName != null) "Escolher treino" else "Iniciar Treino") }
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            // Esqueleto no lugar do spinner: mostra onde o conteúdo vai ficar (DESIGN.md §5).
            com.vitoroliveira.gymtracker.ui.components.HomeSkeleton(Modifier.padding(padding))
        } else {
            val layout = com.vitoroliveira.gymtracker.ui.theme.LocalThemeExtras.current.homeLayout
            Column(Modifier.fillMaxSize().padding(padding)) {
            val actions = HomeActions(onNavigateToMuscleGroups, onNavigateToHistory, onStartWorkoutClick, onStartGroup, onResumeWorkout)
            Box(Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp)) { topSlot() }
            // Aviso de treino em andamento: fica acima de qualquer layout de Home, em todos os temas.
            if (uiState.hasActiveWorkout) Box(Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp)) {
                com.vitoroliveira.gymtracker.ui.home.ResumeWorkoutBanner(uiState, actions)
            }
            HomeBody(
                layout = layout,
                state = uiState,
                actions = actions,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            )
            }
        }
    }
}

@Composable
fun WeeklyHistoryCard(uiState: HomeUiState) {
    ThemedCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Frequência",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    if (uiState.totalWorkouts == 1) "1 treino no total" else "${uiState.totalWorkouts} treinos no total",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "Últimas 8 semanas · linha tracejada = meta",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            BarChart(
                values = uiState.weeklyHistory.map { it.count },
                xLabels = uiState.weeklyHistory.map { "${it.weekStart.dayOfMonth}/${it.weekStart.monthNumber}" },
                goal = uiState.weeklyGoal,
                modifier = Modifier.fillMaxWidth().height(150.dp)
            )
        }
    }
}

@Composable
fun WeeklyGoalCard(weeklyCount: Int, weeklyGoal: Int) {
    val progress = if (weeklyGoal <= 0) 0f
    else (weeklyCount.toFloat() / weeklyGoal.toFloat()).coerceIn(0f, 1f)

    ThemedCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.primaryContainer
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
            Text(
                text = "Meta semanal",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "$weeklyCount de $weeklyGoal treinos essa semana",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Spacer(modifier = Modifier.height(12.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(8.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
            )
        }
    }
}

@Composable
fun StreakCard(streakDays: Int, modifier: Modifier = Modifier) {
    ThemedCard(
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.Star,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (streakDays == 1) "1 dia" else "$streakDays dias",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "seguidos",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun LastWorkoutCard(date: String?, muscleGroupName: String?, modifier: Modifier = Modifier) {
    ThemedCard(
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))

            if (date != null) {
                Text(
                    text = date,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = muscleGroupName ?: "Treino livre",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    text = "Nenhum treino ainda",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun QuickAccessRow(
    onNavigateToMuscleGroups: () -> Unit,
    onNavigateToHistory: () -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedButton(
            onClick = onNavigateToMuscleGroups,
            modifier = Modifier.weight(1f)
        ) {
            Icon(Icons.Default.List, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Grupos")
        }

        OutlinedButton(
            onClick = onNavigateToHistory,
            modifier = Modifier.weight(1f)
        ) {
            Icon(Icons.Default.DateRange, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Histórico")
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun HomeScreenPreview() {
    GymTrackerTheme {
        HomeScreenContent(
            uiState = HomeUiState(
                weeklyCount = 2,
                weeklyGoal = 4,
                streakDays = 3,
                lastWorkoutDate = "12/08",
                lastWorkoutMuscleGroupName = "Treino A"
            ),
            onNavigateToMuscleGroups = {},
            onNavigateToHistory = {},
            onNavigateToProfile = {},
            onStartWorkoutClick = {},
            onClearError = {}
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun HomeScreenEmptyPreview() {
    GymTrackerTheme {
        HomeScreenContent(
            uiState = HomeUiState(weeklyGoal = 3),
            onNavigateToMuscleGroups = {},
            onNavigateToHistory = {},
            onNavigateToProfile = {},
            onStartWorkoutClick = {},
            onClearError = {}
        )
    }
}