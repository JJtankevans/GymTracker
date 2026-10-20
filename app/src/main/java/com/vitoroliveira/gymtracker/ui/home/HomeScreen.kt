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
    viewModel: HomeViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Recarrega o dashboard toda vez que a Home volta a ficar visível
    // (ex: ao voltar de um treino finalizado).
    LaunchedEffect(Unit) {
        viewModel.loadDashboard()
    }

    HomeScreenContent(
        uiState = uiState,
        onNavigateToMuscleGroups = onNavigateToMuscleGroups,
        onNavigateToHistory = onNavigateToHistory,
        onNavigateToProfile = onNavigateToProfile,
        onStartWorkoutClick = onStartWorkoutClick,
        onNavigateToThemes = onNavigateToThemes,
        onNavigateToEvolution = onNavigateToEvolution,
        onNavigateToRecords = onNavigateToRecords,
        onNavigateToSocial = onNavigateToSocial,
        onClearError = { viewModel.clearError() },
        topSlot = { com.vitoroliveira.gymtracker.ui.music.MiniPlayer() }
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
                        Icon(Icons.Default.Groups, contentDescription = "Social")
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
                icon = { Icon(Icons.Default.PlayArrow, contentDescription = null) },
                text = { Text("Iniciar Treino") }
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
            val layout = com.vitoroliveira.gymtracker.ui.theme.LocalThemeExtras.current.homeLayout
            Column(Modifier.fillMaxSize().padding(padding)) {
            Box(Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp)) { topSlot() }
            HomeBody(
                layout = layout,
                state = uiState,
                actions = HomeActions(onNavigateToMuscleGroups, onNavigateToHistory, onStartWorkoutClick),
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