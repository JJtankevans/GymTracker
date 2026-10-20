package com.vitoroliveira.gymtracker.ui.muscleGroup

import com.vitoroliveira.gymtracker.ui.theme.ThemedCard
import com.vitoroliveira.gymtracker.ui.theme.ThemedScaffold
import com.vitoroliveira.gymtracker.ui.theme.ThemedTopAppBar
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.vitoroliveira.gymtracker.data.model.MuscleGroup
import com.vitoroliveira.gymtracker.presentation.viewmodel.MuscleGroupUiState
import com.vitoroliveira.gymtracker.presentation.viewmodel.MuscleGroupViewModel
import com.vitoroliveira.gymtracker.ui.theme.GymTrackerTheme
import org.koin.compose.viewmodel.koinViewModel

// Cores disponíveis para o usuário escolher
val availableColors = listOf(
    "#F44336", "#E91E63", "#9C27B0", "#3F51B5",
    "#2196F3", "#00BCD4", "#4CAF50", "#FF9800",
    "#FF5722", "#607D8B", "#795548", "#6200EE"
)

@Composable
fun MuscleGroupScreen(
    onNavigateToExercises: (String, String) -> Unit,
    onBack: () -> Unit = {},
    viewModel: MuscleGroupViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    MuscleGroupScreenContent(
        uiState = uiState,
        onNavigateToExercises = onNavigateToExercises,
        onBack = onBack,
        onCreate = { name, color -> viewModel.createMuscleGroup(name, color) },
        onUpdate = { muscleGroup -> viewModel.updateMuscleGroup(muscleGroup) },
        onDelete = { id -> viewModel.deleteMuscleGroup(id) },
        onClearMessages = { viewModel.clearMessages() },
        onCreateFromTemplate = { viewModel.createFromTemplate(it) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MuscleGroupScreenContent(
    uiState: MuscleGroupUiState,
    onNavigateToExercises: (String, String) -> Unit,
    onBack: () -> Unit = {},
    onCreate: (String, String) -> Unit,
    onUpdate: (MuscleGroup) -> Unit,
    onDelete: (String) -> Unit,
    onClearMessages: () -> Unit,
    onCreateFromTemplate: (com.vitoroliveira.gymtracker.data.catalog.WorkoutTemplate) -> Unit = {}
) {
    var showTemplates by remember { mutableStateOf(false) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var groupToEdit by remember { mutableStateOf<MuscleGroup?>(null) }
    var groupToDelete by remember { mutableStateOf<MuscleGroup?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }

    // Exibir erros e sucessos via Snackbar
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

    ThemedScaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            ThemedTopAppBar(
                title = { Text("Grupos Musculares") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreateDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Adicionar grupo")
            }
        }
    ) { padding ->

        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (uiState.muscleGroups.isEmpty()) {
            // Empty state
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Nenhum grupo cadastrado",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Toque no + para criar seu primeiro grupo",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { showTemplates = true }) { Text("Começar com um treino pronto") }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                item(key = "templates") {
                    OutlinedButton(onClick = { showTemplates = true }, modifier = Modifier.fillMaxWidth()) {
                        Text("Treinos prontos (Push, Pull, Pernas…)")
                    }
                }
                items(uiState.muscleGroups, key = { it.id ?: it.name }) { group ->
                    MuscleGroupItem(
                        muscleGroup = group,
                        onClick = {
                            group.id?.let { id ->
                                onNavigateToExercises(id, group.name)
                            }
                        },
                        onEdit = { groupToEdit = group },
                        onDelete = { groupToDelete = group }
                    )
                }
            }
        }
    }

    if (showTemplates) {
        TemplateSheet(
            onPick = { onCreateFromTemplate(it); showTemplates = false },
            onDismiss = { showTemplates = false }
        )
    }

    // Dialog de criação
    if (showCreateDialog) {
        MuscleGroupDialog(
            title = "Novo Grupo",
            onConfirm = { name, color ->
                onCreate(name, color)
                showCreateDialog = false
            },
            onDismiss = { showCreateDialog = false }
        )
    }

    // Dialog de edição
    groupToEdit?.let { group ->
        MuscleGroupDialog(
            title = "Editar Grupo",
            initialName = group.name,
            initialColor = group.colorHex,
            onConfirm = { name, color ->
                onUpdate(group.copy(name = name, colorHex = color))
                groupToEdit = null
            },
            onDismiss = { groupToEdit = null }
        )
    }

    // Dialog de confirmação de exclusão
    groupToDelete?.let { group ->
        AlertDialog(
            onDismissRequest = { groupToDelete = null },
            title = { Text("Remover grupo") },
            text = {
                Text("Deseja remover o grupo \"${group.name}\"? Os exercícios associados não serão deletados.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        group.id?.let { onDelete(it) }
                        groupToDelete = null
                    }
                ) {
                    Text("Remover", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { groupToDelete = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun MuscleGroupItem(
    muscleGroup: MuscleGroup,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val color = runCatching {
        Color(android.graphics.Color.parseColor(muscleGroup.colorHex))
    }.getOrDefault(MaterialTheme.colorScheme.primary)

    ThemedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Indicador de cor
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(color)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = muscleGroup.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )

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

@Composable
fun MuscleGroupDialog(
    title: String,
    initialName: String = "",
    initialColor: String = "#6200EE",
    onConfirm: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var selectedColor by remember { mutableStateOf(initialColor) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome do grupo") },
                    placeholder = { Text("Ex: Treino A, Peito e Tríceps...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Cor",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Color picker
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    availableColors.take(6).forEach { hex ->
                        ColorPickerItem(
                            hex = hex,
                            isSelected = selectedColor == hex,
                            onClick = { selectedColor = hex }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    availableColors.drop(6).forEach { hex ->
                        ColorPickerItem(
                            hex = hex,
                            isSelected = selectedColor == hex,
                            onClick = { selectedColor = hex }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name, selectedColor) },
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

@Composable
fun ColorPickerItem(
    hex: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val color = runCatching {
        Color(android.graphics.Color.parseColor(hex))
    }.getOrDefault(Color.Gray)

    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(if (isSelected) Color.White else color)
            .padding(if (isSelected) 3.dp else 0.dp)
            .clip(CircleShape)
            .background(color)
            .clickable { onClick() }
    )
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun MuscleGroupScreenEmptyPreview() {
    GymTrackerTheme {
        MuscleGroupScreenContent(
            uiState = MuscleGroupUiState(),
            onNavigateToExercises = { _, _ -> },
            onBack = {},
            onCreate = { _, _ -> },
            onUpdate = {},
            onDelete = {},
            onClearMessages = {}
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun MuscleGroupScreenWithDataPreview() {
    GymTrackerTheme {
        MuscleGroupScreenContent(
            uiState = MuscleGroupUiState(
                muscleGroups = listOf(
                    MuscleGroup(id = "1", name = "Treino A", colorHex = "#2196F3"),
                    MuscleGroup(id = "2", name = "Treino B", colorHex = "#F44336"),
                    MuscleGroup(id = "3", name = "Treino C", colorHex = "#4CAF50"),
                )
            ),
            onNavigateToExercises = { _, _ -> },
            onBack = {},
            onCreate = { _, _ -> },
            onUpdate = {},
            onDelete = {},
            onClearMessages = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun MuscleGroupDialogPreview() {
    GymTrackerTheme {
        MuscleGroupDialog(
            title = "Novo Grupo",
            onConfirm = { _, _ -> },
            onDismiss = {}
        )
    }
}