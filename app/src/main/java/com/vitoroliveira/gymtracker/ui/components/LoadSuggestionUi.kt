package com.vitoroliveira.gymtracker.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vitoroliveira.gymtracker.presentation.body.BodyData
import com.vitoroliveira.gymtracker.presentation.body.LoadSuggester
import com.vitoroliveira.gymtracker.presentation.body.LoadSuggestion
import com.vitoroliveira.gymtracker.ui.utils.formatKg

fun currentYear(): Int = java.time.LocalDate.now().year
fun todayEpochDay(): Int = java.time.LocalDate.now().toEpochDay().toInt()

fun suggestLoad(
    body: BodyData, name: String, lastKg: Double?, lastReps: Int?, targetReps: Int
): LoadSuggestion? = LoadSuggester.suggest(
    name, lastKg, lastReps, targetReps, body.latestKg, body.profile.sex, body.profile.ageAt(currentYear())
)

/** Dados corporais do usuário, fornecidos na raiz do app. Sem provedor (previews/testes): vazio. */
val LocalBodyData = androidx.compose.runtime.compositionLocalOf { BodyData() }

/** Lê os dados corporais e calcula a sugestão de carga para o exercício. */
@Composable
fun rememberLoadSuggestion(name: String, lastKg: Double?, lastReps: Int?, targetReps: Int): LoadSuggestion? {
    val body = LocalBodyData.current
    return remember(body, name, lastKg, lastReps, targetReps) { suggestLoad(body, name, lastKg, lastReps, targetReps) }
}

/** Botão de sugestão: um toque preenche a carga. */
@Composable
fun LoadSuggestionChip(suggestion: LoadSuggestion, onApply: (Double) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier) {
        AssistChip(
            onClick = { onApply(suggestion.weightKg) },
            label = { Text("Sugestão: ${formatKg(suggestion.weightKg)} kg") },
            leadingIcon = { Icon(Icons.Default.AutoAwesome, null) }
        )
        Text(
            suggestion.reason, style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 4.dp, top = 2.dp)
        )
    }
}
