// app/src/androidMain/kotlin/com/vitoroliveira/gymtracker/ui/exercise/ExerciseScreen.kt

package com.vitoroliveira.gymtracker.ui.exercise

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.vitoroliveira.gymtracker.data.model.Exercise
import com.vitoroliveira.gymtracker.data.model.MuscleGroup
import com.vitoroliveira.gymtracker.presentation.viewmodel.ExerciseUiState
import com.vitoroliveira.gymtracker.presentation.viewmodel.ExerciseViewModel
import com.vitoroliveira.gymtracker.presentation.viewmodel.MuscleGroupViewModel
import com.vitoroliveira.gymtracker.ui.theme.GymTrackerTheme
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ExerciseScreen(
    muscleGroupId: String?,
    muscleGroupName: String?,
    onNavigateToDetail: (String, String) -> Unit,
    onBack: () -> Unit = {},
    exerciseViewModel: ExerciseViewModel = koinViewModel(),
    muscleGroupViewModel: MuscleGroupViewModel = koinViewModel()
) {
    val exerciseState by exerciseViewModel.uiState.collectAsState()
    val groupState by muscleGroupViewModel.uiState.collectAsState()

    // Pré-filtra pelo grupo com que o usuário chegou nessa tela (se veio de um grupo específico)
    LaunchedEffect(muscleGroupId) {
        exerciseViewModel.filterByMuscleGroup(muscleGroupId)
    }

    ExerciseScreenContent(
        screenTitle = muscleGroupName?.let { "Exercícios · $it" } ?: "Exercícios",
        uiState = exerciseState,
        availableGroups = groupState.muscleGroups,
        onNavigateToDetail = onNavigateToDetail,
        onBack = onBack,
        onFilterChange = { groupId -> exerciseViewModel.filterByMuscleGroup(groupId) },
        onCreate = { name, groupId, sets, reps, notes ->
            exerciseViewModel.createExercise(name, groupId, sets, reps, notes)
        },
        onUpdate = { exercise -> exerciseViewModel.updateExercise(exercise) },
        onDelete = { id -> exerciseViewModel.deleteExercise(id) },
        onClearMessages = { exerciseViewModel.clearMessages() }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseScreenContent(
    screenTitle: String,
    uiState: ExerciseUiState,
    availableGroups: List<MuscleGroup>,
    onNavigateToDetail: (String, String) -> Unit,
    onBack: () -> Unit = {},
    onFilterChange: (String?) -> Unit,
    onCreate: (String, String?, Int, Int, String?) -> Unit,
    onUpdate: (Exercise) -> Unit,
    onDelete: (String) -> Unit,
    onClearMessages: () -> Unit
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var exerciseToEdit by remember { mutableStateOf<Exercise?>(null) }
    var exerciseToDelete by remember { mutableStateOf<Exercise?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            onClearMessages()
        }
    }
    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            onClearMessages()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(screenTitle) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreateDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Adicionar exercício")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            MuscleGroupFilterRow(
                groups = availableGroups,
                selectedGroupId = uiState.selectedMuscleGroupId,
                onSelect = onFilterChange
            )

            if (uiState.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (uiState.exercises.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Nenhum exercício cadastrado",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Toque no + para criar seu primeiro exercício",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 16.dp)
                ) {
                    items(uiState.exercises, key = { it.id ?: it.name }) { exercise ->
                        ExerciseItem(
                            exercise = exercise,
                            groupName = availableGroups.find { it.id == exercise.muscleGroupId }?.name,
                            groupColorHex = availableGroups.find { it.id == exercise.muscleGroupId }?.colorHex,
                            onClick = {
                                exercise.id?.let { id -> onNavigateToDetail(id, exercise.name) }
                            },
                            onEdit = { exerciseToEdit = exercise },
                            onDelete = { exerciseToDelete = exercise }
                        )
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        ExerciseDialog(
            title = "Novo Exercício",
            availableGroups = availableGroups,
            initialMuscleGroupId = uiState.selectedMuscleGroupId,
            onConfirm = { name, groupId, sets, reps, notes ->
                onCreate(name, groupId, sets, reps, notes)
                showCreateDialog = false
            },
            onDismiss = { showCreateDialog = false }
        )
    }

    exerciseToEdit?.let { exercise ->
        ExerciseDialog(
            title = "Editar Exercício",
            availableGroups = availableGroups,
            initialName = exercise.name,
            initialMuscleGroupId = exercise.muscleGroupId,
            initialSets = exercise.defaultSets,
            initialReps = exercise.defaultReps,
            initialNotes = exercise.notes ?: "",
            onConfirm = { name, groupId, sets, reps, notes ->
                onUpdate(exercise.copy(
                    name = name,
                    muscleGroupId = groupId,
                    defaultSets = sets,
                    defaultReps = reps,
                    notes = notes
                ))
                exerciseToEdit = null
            },
            onDismiss = { exerciseToEdit = null }
        )
    }

    exerciseToDelete?.let { exercise ->
        AlertDialog(
            onDismissRequest = { exerciseToDelete = null },
            title = { Text("Remover exercício") },
            text = {
                Text("Deseja remover \"${exercise.name}\"? Isso também apagará o histórico de séries registradas para ele.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        exercise.id?.let { onDelete(it) }
                        exerciseToDelete = null
                    }
                ) {
                    Text("Remover", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { exerciseToDelete = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun MuscleGroupFilterRow(
    groups: List<MuscleGroup>,
    selectedGroupId: String?,
    onSelect: (String?) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            FilterChip(
                selected = selectedGroupId == null,
                onClick = { onSelect(null) },
                label = { Text("Todos") }
            )
        }
        items(groups, key = { it.id ?: it.name }) { group ->
            FilterChip(
                selected = selectedGroupId == group.id,
                onClick = { onSelect(group.id) },
                label = { Text(group.name) }
            )
        }
    }
}

@Composable
fun ExerciseItem(
    exercise: Exercise,
    groupName: String?,
    groupColorHex: String?,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val color = runCatching {
        Color(android.graphics.Color.parseColor(groupColorHex ?: "#6200EE"))
    }.getOrDefault(MaterialTheme.colorScheme.primary)

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(color)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = exercise.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${exercise.defaultSets} séries × ${exercise.defaultReps} reps" +
                            (groupName?.let { " · $it" } ?: ""),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = onEdit) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "Editar",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Remover",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseDialog(
    title: String,
    availableGroups: List<MuscleGroup>,
    initialName: String = "",
    initialMuscleGroupId: String? = null,
    initialSets: Int = 3,
    initialReps: Int = 12,
    initialNotes: String = "",
    onConfirm: (name: String, groupId: String?, sets: Int, reps: Int, notes: String?) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var selectedGroupId by remember { mutableStateOf(initialMuscleGroupId) }
    var setsText by remember { mutableStateOf(initialSets.toString()) }
    var repsText by remember { mutableStateOf(initialReps.toString()) }
    var notes by remember { mutableStateOf(initialNotes) }
    var groupMenuExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome do exercício") },
                    placeholder = { Text("Ex: Supino Reto, Agachamento...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                ExposedDropdownMenuBox(
                    expanded = groupMenuExpanded,
                    onExpandedChange = { groupMenuExpanded = it }
                ) {
                    OutlinedTextField(
                        value = availableGroups.find { it.id == selectedGroupId }?.name ?: "Nenhum grupo",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Grupo muscular") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = groupMenuExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = groupMenuExpanded,
                        onDismissRequest = { groupMenuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Nenhum grupo") },
                            onClick = { selectedGroupId = null; groupMenuExpanded = false }
                        )
                        availableGroups.forEach { group ->
                            DropdownMenuItem(
                                text = { Text(group.name) },
                                onClick = { selectedGroupId = group.id; groupMenuExpanded = false }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = setsText,
                        onValueChange = { setsText = it.filter { c -> c.isDigit() } },
                        label = { Text("Séries") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = repsText,
                        onValueChange = { repsText = it.filter { c -> c.isDigit() } },
                        label = { Text("Repetições") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Observações (opcional)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        name,
                        selectedGroupId,
                        setsText.toIntOrNull() ?: 3,
                        repsText.toIntOrNull() ?: 12,
                        notes.ifBlank { null }
                    )
                },
                enabled = name.isNotBlank()
            ) {
                Text("Salvar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun ExerciseScreenEmptyPreview() {
    GymTrackerTheme {
        ExerciseScreenContent(
            screenTitle = "Exercícios",
            uiState = ExerciseUiState(),
            availableGroups = emptyList(),
            onNavigateToDetail = { _, _ -> },
            onBack = {},
            onFilterChange = {},
            onCreate = { _, _, _, _, _ -> },
            onUpdate = {},
            onDelete = {},
            onClearMessages = {}
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun ExerciseScreenWithDataPreview() {
    GymTrackerTheme {
        ExerciseScreenContent(
            screenTitle = "Exercícios · Treino A",
            uiState = ExerciseUiState(
                exercises = listOf(
                    Exercise(id = "1", name = "Supino Reto", muscleGroupId = "g1", defaultSets = 4, defaultReps = 10),
                    Exercise(id = "2", name = "Supino Inclinado", muscleGroupId = "g1", defaultSets = 3, defaultReps = 12),
                    Exercise(id = "3", name = "Crucifixo", muscleGroupId = "g1", defaultSets = 3, defaultReps = 15),
                ),
                selectedMuscleGroupId = "g1"
            ),
            availableGroups = listOf(
                MuscleGroup(id = "g1", name = "Treino A", colorHex = "#2196F3"),
                MuscleGroup(id = "g2", name = "Treino B", colorHex = "#F44336"),
            ),
            onNavigateToDetail = { _, _ -> },
            onBack = {},
            onFilterChange = {},
            onCreate = { _, _, _, _, _ -> },
            onUpdate = {},
            onDelete = {},
            onClearMessages = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ExerciseDialogPreview() {
    GymTrackerTheme {
        ExerciseDialog(
            title = "Novo Exercício",
            availableGroups = listOf(
                MuscleGroup(id = "g1", name = "Treino A", colorHex = "#2196F3")
            ),
            onConfirm = { _, _, _, _, _ -> },
            onDismiss = {}
        )
    }
}