package com.vitoroliveira.gymtracker.ui.achievements

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
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vitoroliveira.gymtracker.presentation.achievements.Achievement
import com.vitoroliveira.gymtracker.presentation.achievements.AchievementsUiState
import com.vitoroliveira.gymtracker.presentation.achievements.AchievementsViewModel
import com.vitoroliveira.gymtracker.ui.theme.ThemedCard
import com.vitoroliveira.gymtracker.ui.theme.ThemedScaffold
import com.vitoroliveira.gymtracker.ui.theme.ThemedTopAppBar
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AchievementsScreen(onBack: () -> Unit, viewModel: AchievementsViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsState()
    AchievementsContent(state, onBack, onRetry = viewModel::load)
}

@Composable
fun AchievementsContent(state: AchievementsUiState, onBack: () -> Unit, onRetry: () -> Unit = {}) {
    ThemedScaffold(
        topBar = {
            ThemedTopAppBar(
                title = { Text("Conquistas") },
                navigationIcon = { IconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar") } }
            )
        }
    ) { padding ->
        when {
            state.isLoading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            state.errorMessage != null -> Column(
                Modifier.fillMaxSize().padding(padding).padding(32.dp),
                verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(state.errorMessage, color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(onRetry) { Text("Tentar de novo") }
            }
            else -> LazyColumn(
                Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item(key = "summary") {
                    Text(
                        "${state.unlockedCount} de ${state.achievements.size} desbloqueadas",
                        style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold
                    )
                }
                state.achievements.groupBy { it.family }.forEach { (family, list) ->
                    item(key = "f-" + family.name) {
                        Text(
                            family.label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                    items(list, key = { it.id }) { AchievementCard(it) }
                }
            }
        }
    }
}

@Composable
private fun AchievementCard(a: Achievement) {
    val cs = MaterialTheme.colorScheme
    ThemedCard(
        Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium,
        containerColor = if (a.unlocked) cs.primaryContainer else cs.surfaceContainerHighest
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (a.unlocked) Icons.Default.EmojiEvents else Icons.Default.Lock,
                contentDescription = if (a.unlocked) "Desbloqueada" else "Bloqueada",
                tint = if (a.unlocked) cs.primary else cs.outline,
                modifier = Modifier.size(32.dp)
            )
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    a.title, fontWeight = FontWeight.Bold,
                    color = if (a.unlocked) cs.onPrimaryContainer else cs.onSurface
                )
                Text(a.description, style = MaterialTheme.typography.bodySmall, color = if (a.unlocked) cs.onPrimaryContainer else cs.onSurfaceVariant)
                if (!a.unlocked) {
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        LinearProgressIndicator(progress = { a.fraction }, modifier = Modifier.weight(1f).height(6.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("${a.progress.coerceAtMost(a.target)}/${a.target}", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}
