package com.vitoroliveira.gymtracker.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/**
 * Par de botões "−" / "+" para ajustar um número sem abrir o teclado (reps e carga na hora do treino, com a mão suada).
 * Só desenha e avisa o clique; a conta (limites, vírgula decimal) fica em `stepRepsText`/`stepKgText` (ui/utils/FormatUtils.kt).
 */
@Composable
fun QuickSteps(
    minusLabel: String,
    plusLabel: String,
    minusDescription: String,
    plusDescription: String,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(
            onClick = onMinus,
            modifier = Modifier.weight(1f).semantics { contentDescription = minusDescription },
        ) { Text(minusLabel) }
        OutlinedButton(
            onClick = onPlus,
            modifier = Modifier.weight(1f).semantics { contentDescription = plusDescription },
        ) { Text(plusLabel) }
    }
}
