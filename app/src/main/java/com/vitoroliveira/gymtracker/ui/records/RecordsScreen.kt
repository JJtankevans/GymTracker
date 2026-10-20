package com.vitoroliveira.gymtracker.ui.records

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vitoroliveira.gymtracker.data.catalog.ExerciseCatalog
import com.vitoroliveira.gymtracker.presentation.records.RecordRow
import com.vitoroliveira.gymtracker.presentation.records.RecordsUiState
import com.vitoroliveira.gymtracker.presentation.records.RecordsViewModel
import com.vitoroliveira.gymtracker.ui.exercise.ExerciseArt
import com.vitoroliveira.gymtracker.ui.exercise.tint
import com.vitoroliveira.gymtracker.ui.theme.ThemedCard
import com.vitoroliveira.gymtracker.ui.theme.ThemedScaffold
import com.vitoroliveira.gymtracker.ui.theme.ThemedTopAppBar
import com.vitoroliveira.gymtracker.ui.utils.formatKg
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun RecordsScreen(onBack: () -> Unit, viewModel: RecordsViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsState()
    RecordsContent(state, onBack, onRetry = viewModel::load)
}

@Composable
fun RecordsContent(state: RecordsUiState, onBack: () -> Unit, onRetry: () -> Unit = {}) {
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(state.errorMessage) { state.errorMessage?.let { snackbar.showSnackbar(it) } }

    ThemedScaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            ThemedTopAppBar(
                title = { Text("Recordes") },
                navigationIcon = { IconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar") } }
            )
        }
    ) { padding ->
        when {
            state.isLoading -> Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) { CircularProgressIndicator() }
            state.errorMessage != null && state.records.isEmpty() -> Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                com.vitoroliveira.gymtracker.ui.components.ErrorState(state.errorMessage!!, onRetry)
            }
            state.records.isEmpty() -> Box(Modifier.fillMaxSize().padding(padding).padding(32.dp), Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.EmojiEvents, null, Modifier.size(56.dp), tint = MaterialTheme.colorScheme.tertiary)
                    Spacer(Modifier.height(12.dp))
                    Text("Nenhum recorde ainda", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Registre séries com carga e o seu melhor de cada exercício aparece aqui.",
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            else -> LazyColumn(
                Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item(key = "summary") { Summary(state) }
                items(state.records, key = { it.exerciseId }) { RecordCard(it, state) }
                item(key = "foot") {
                    Text(
                        "O 1RM é uma estimativa (fórmula de Epley) a partir da carga e das repetições do dia. " +
                            "Exercícios só com peso do corpo não entram.",
                        style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun Summary(state: RecordsUiState) {
    ThemedCard(Modifier.fillMaxWidth(), containerColor = MaterialTheme.colorScheme.primaryContainer) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.EmojiEvents, null, Modifier.size(40.dp), tint = MaterialTheme.colorScheme.tertiary)
            Spacer(Modifier.width(14.dp))
            Column {
                Text(
                    "${state.records.size} ${if (state.records.size == 1) "recorde" else "recordes"}",
                    style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    if (state.recentCount > 0) "${state.recentCount} batido${if (state.recentCount > 1) "s" else ""} nos últimos 30 dias"
                    else "Nenhum recorde novo nos últimos 30 dias",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
private fun RecordCard(r: RecordRow, state: RecordsUiState) {
    val catalog = remember(r.exerciseName) {
        ExerciseCatalog.all.firstOrNull { ExerciseCatalog.isSameName(it.name, r.exerciseName) }
    }
    val recent = state.today?.let { r.isRecent(it) } == true

    ThemedCard(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            if (catalog != null) {
                val tint = catalog.muscle.tint()
                Box(
                    Modifier.size(46.dp).clip(MaterialTheme.shapes.small).background(tint.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) { ExerciseArt(catalog, tint, 32.dp, Modifier.fillMaxSize().padding(2.dp)) }
            } else {
                Box(
                    Modifier.size(46.dp).clip(MaterialTheme.shapes.small).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Default.EmojiEvents, null, tint = MaterialTheme.colorScheme.primary) }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(r.exerciseName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(
                    "1RM estimado ${formatKg(r.bestOneRmKg)} kg · ${dayMonth(r.bestDate)}",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                r.improvementKg?.takeIf { it > 0 }?.let {
                    Text(
                        "+${formatKg(it)} kg sobre o recorde anterior",
                        style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "${formatKg(r.bestWeightKg)} kg",
                    style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary
                )
                if (recent) Text("NOVO", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary, fontWeight = FontWeight.Bold)
            }
        }
    }
}

private fun dayMonth(iso: String): String =
    iso.split("-").let { if (it.size == 3) "${it[2]}/${it[1]}/${it[0].takeLast(2)}" else iso }
