package com.vitoroliveira.gymtracker.ui.workout

import com.vitoroliveira.gymtracker.ui.theme.ThemedCard
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StartWorkoutSheet(
    onStartWorkout: (muscleGroupId: String?, muscleGroupName: String?) -> Unit,
    onDismiss: () -> Unit,
    onStartCircuit: (muscleGroupId: String?, muscleGroupName: String?) -> Unit = { _, _ -> },
    viewModel: MuscleGroupViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    ModalBottomSheet(onDismissRequest = onDismiss) {
        StartWorkoutSheetContent(
            uiState = uiState,
            onStartWorkout = onStartWorkout,
            onStartCircuit = onStartCircuit,
            onDismiss = onDismiss
        )
    }
}

@Composable
fun StartWorkoutSheetContent(
    uiState: MuscleGroupUiState,
    onStartWorkout: (muscleGroupId: String?, muscleGroupName: String?) -> Unit,
    onDismiss: () -> Unit,
    onStartCircuit: (muscleGroupId: String?, muscleGroupName: String?) -> Unit = { _, _ -> }
) {
    var circuit by remember { mutableStateOf(false) }
    val start: (String?, String?) -> Unit = { id, name -> if (circuit) onStartCircuit(id, name) else onStartWorkout(id, name) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp)
    ) {
        Text(
            text = "Iniciar Treino",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Selecione o grupo muscular do dia ou inicie sem grupo",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Modo circuito (beta)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(
                    "O app conta as séries e o descanso sozinho e aprende seu ritmo.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(checked = circuit, onCheckedChange = { circuit = it })
        }
        Spacer(modifier = Modifier.height(16.dp))

        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxWidth().height(120.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            // Opção sem grupo muscular
            OutlinedButton(
                onClick = { start(null, null) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Treino livre (sem grupo)")
            }

            if (uiState.muscleGroups.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Ou escolha um grupo",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.heightIn(max = 300.dp)
                ) {
                    items(uiState.muscleGroups, key = { it.id ?: it.name }) { group ->
                        MuscleGroupSelectItem(
                            muscleGroup = group,
                            onClick = { start(group.id, group.name) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MuscleGroupSelectItem(
    muscleGroup: MuscleGroup,
    onClick: () -> Unit
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
            Box(
                modifier = Modifier
                    .size(32.dp)
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

            Icon(
                Icons.Default.PlayArrow,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun StartWorkoutSheetPreview() {
    GymTrackerTheme {
        StartWorkoutSheetContent(
            uiState = MuscleGroupUiState(
                muscleGroups = listOf(
                    MuscleGroup(id = "1", name = "Treino A", colorHex = "#2196F3"),
                    MuscleGroup(id = "2", name = "Treino B", colorHex = "#F44336"),
                    MuscleGroup(id = "3", name = "Treino C", colorHex = "#4CAF50"),
                )
            ),
            onStartWorkout = { _, _ -> },
            onDismiss = {}
        )
    }
}