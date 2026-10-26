package com.vitoroliveira.gymtracker.ui.workout

import com.vitoroliveira.gymtracker.ui.theme.ThemedCard
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
    val bodyVm: com.vitoroliveira.gymtracker.presentation.body.BodyViewModel = koinViewModel()
    // mesmo ViewModel da Home (o sheet abre dentro da tela Home): traz a sugestão do próximo grupo da rotação
    val homeVm: com.vitoroliveira.gymtracker.presentation.viewmodel.HomeViewModel = koinViewModel()
    val home by homeVm.uiState.collectAsState()
    val body by bodyVm.data.collectAsState()

    // skipPartiallyExpanded: só "aberto" e "fechado" (como os outros sheets do app). Com o estado parcial, arrastar
    // para cima deixava o sheet preso no topo, e a lista rolável de grupos dentro dele disputava o gesto com o sheet.
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        StartWorkoutSheetContent(
            uiState = uiState,
            onStartWorkout = onStartWorkout,
            onStartCircuit = onStartCircuit,
            onDismiss = onDismiss,
            suggestedGroupId = home.suggestedGroupId,
            weighIn = if (body.profile.askWeightEachWorkout) WeighIn(body.latestKg) { kg ->
                bodyVm.logWeight(com.vitoroliveira.gymtracker.ui.components.todayEpochDay(), kg)
            } else null
        )
    }
}

@Composable
fun StartWorkoutSheetContent(
    uiState: MuscleGroupUiState,
    onStartWorkout: (muscleGroupId: String?, muscleGroupName: String?) -> Unit,
    onDismiss: () -> Unit,
    onStartCircuit: (muscleGroupId: String?, muscleGroupName: String?) -> Unit = { _, _ -> },
    suggestedGroupId: String? = null,
    weighIn: WeighIn? = null
) {
    var circuit by remember { mutableStateOf(false) }
    var weightText by remember(weighIn?.initialKg) { mutableStateOf(weighIn?.initialKg?.let { com.vitoroliveira.gymtracker.ui.utils.formatKg(it) } ?: "") }
    val start: (String?, String?) -> Unit = { id, name ->
        // peso do dia é opcional: só registra se for um número válido
        weighIn?.let { w -> weightText.replace(',', '.').toDoubleOrNull()?.let(w.onLog) }
        if (circuit) onStartCircuit(id, name) else onStartWorkout(id, name)
    }
    // Uma única rolagem para o conteúdo todo (nada de lista rolável dentro do sheet): se não couber, rola aqui.
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
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

        if (weighIn != null) {
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = weightText, onValueChange = { weightText = it.filter { c -> c.isDigit() || c == ',' || c == '.' } },
                label = { Text("Seu peso hoje (kg) — opcional") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal)
            )
        }
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

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // o sugerido (próximo da rotação) vem primeiro, com selo
                    val ordered = uiState.muscleGroups.sortedBy { it.id != suggestedGroupId }
                    ordered.forEach { group ->
                        key(group.id ?: group.name) {
                            MuscleGroupSelectItem(
                                muscleGroup = group,
                                suggested = suggestedGroupId != null && group.id == suggestedGroupId,
                                onClick = { start(group.id, group.name) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MuscleGroupSelectItem(
    muscleGroup: MuscleGroup,
    onClick: () -> Unit,
    suggested: Boolean = false
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

            Column(Modifier.weight(1f)) {
                Text(
                    text = muscleGroup.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                if (suggested) {
                    Text(
                        "Sugerido: próximo da sua rotação",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

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
/** Campo opcional de peso do dia na hora de iniciar o treino. */
class WeighIn(val initialKg: Double?, val onLog: (Double) -> Unit)
