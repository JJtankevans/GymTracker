package com.vitoroliveira.gymtracker.ui.muscleGroup

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vitoroliveira.gymtracker.data.model.SharedRoutine
import com.vitoroliveira.gymtracker.presentation.share.SharedCode
import com.vitoroliveira.gymtracker.presentation.share.shareInvite

/** Mostra o código gerado, com copiar e mandar pelo WhatsApp etc. */
@Composable
fun ShareCodeDialog(shared: SharedCode, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Compartilhar \"${shared.groupName}\"") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Seu amigo abre Grupos > Importar treino e digita este código:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.padding(6.dp))
                Box(
                    Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Text(
                        shared.code.chunked(4).joinToString("-"),
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 28.sp,
                        letterSpacing = 2.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Spacer(Modifier.padding(4.dp))
                TextButton(onClick = {
                    clipboard.setText(AnnotatedString(shared.code))
                    copied = true
                }) { Text(if (copied) "Copiado!" else "Copiar código") }
                Text(
                    "O código leva uma cópia do treino de agora; mudanças depois não chegam para quem já importou.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val send = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, shareInvite(shared))
                }
                context.startActivity(Intent.createChooser(send, "Mandar treino"))
            }) { Text("Mandar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Fechar") } }
    )
}

/** Campo do código e, depois de achar, a prévia do treino para confirmar. */
@Composable
fun ImportRoutineDialog(
    preview: SharedRoutine?,
    isWorking: Boolean,
    error: String?,
    onLookup: (String) -> Unit,
    onConfirm: () -> Unit,
    onBackToCode: () -> Unit,
    onDismiss: () -> Unit
) {
    var input by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (preview == null) "Importar treino" else preview.name) },
        text = {
            if (preview == null) {
                Column {
                    Text(
                        "Cole o código (ou a mensagem inteira) que seu amigo mandou.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.padding(6.dp))
                    OutlinedTextField(
                        value = input,
                        onValueChange = { input = it },
                        label = { Text("Código") },
                        placeholder = { Text("Ex: K7F2-Q9XA") },
                        isError = error != null,
                        supportingText = error?.let { { Text(it) } },
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            } else {
                RoutinePreview(preview, error)
            }
        },
        confirmButton = {
            if (isWorking) {
                CircularProgressIndicator(Modifier.size(28.dp))
            } else if (preview == null) {
                Button(onClick = { onLookup(input) }, enabled = input.isNotBlank()) { Text("Buscar") }
            } else {
                Button(onClick = onConfirm) { Text("Importar") }
            }
        },
        dismissButton = {
            if (preview == null) TextButton(onClick = onDismiss) { Text("Cancelar") }
            else TextButton(onClick = onBackToCode) { Text("Voltar") }
        }
    )
}

@Composable
private fun RoutinePreview(routine: SharedRoutine, error: String?) {
    val color = runCatching { Color(android.graphics.Color.parseColor(routine.colorHex)) }
        .getOrDefault(MaterialTheme.colorScheme.primary)
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(14.dp).clip(CircleShape).background(color))
            Spacer(Modifier.width(8.dp))
            val owner = routine.ownerUsername?.let { "de @$it · " }.orEmpty()
            val imports = if (routine.imports == 1) "1 importação" else "${routine.imports} importações"
            Text(
                "$owner${routine.exercises.size} exercícios · $imports",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.padding(4.dp))
        LazyColumn(Modifier.heightIn(max = 280.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(routine.exercises) { ex ->
                Column {
                    Text(ex.name, fontWeight = FontWeight.Medium)
                    val notes = ex.notes?.takeIf { it.isNotBlank() }?.let { " · $it" }.orEmpty()
                    Text(
                        "${ex.sets} x ${ex.reps}$notes",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        Spacer(Modifier.padding(4.dp))
        Text(
            "Vira um grupo novo na sua lista. Exercícios com o mesmo nome dos seus são reaproveitados (seu histórico continua junto).",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        error?.let {
            Spacer(Modifier.padding(4.dp))
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
    }
}
