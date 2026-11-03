package com.vitoroliveira.gymtracker.wear

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vitoroliveira.gymtracker.ui.theme.ThemedCard
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/** Cartão do Perfil: mostra o relógio conectado e manda as rotinas na hora (sem esperar o relógio pedir). */
@Composable
fun WatchSyncCard(bridge: WatchBridge = koinInject()) {
    val scope = rememberCoroutineScope()
    var watches by remember { mutableStateOf<List<String>?>(null) }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) { watches = runCatching { bridge.connectedWatches() }.getOrDefault(emptyList()) }

    ThemedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Watch, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("Relógio", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        when {
                            watches == null -> "Procurando..."
                            watches!!.isEmpty() -> "Nenhum relógio conectado. O app do relógio (Wear OS) recebe seus grupos e manda os treinos para cá."
                            else -> "Conectado: ${watches!!.joinToString()}"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            OutlinedButton(
                onClick = {
                    busy = true
                    scope.launch {
                        status = try {
                            val saved = bridge.importPending()
                            bridge.publishRoutines()
                            watches = runCatching { bridge.connectedWatches() }.getOrDefault(watches)
                            if (saved > 0) "Enviado ao relógio. $saved treino(s) do relógio salvos." else "Grupos e cargas enviados ao relógio."
                        } catch (e: Exception) {
                            if (e is CancellationException) throw e
                            "Não deu para falar com o relógio: ${e.message ?: "erro desconhecido"}"
                        }
                        busy = false
                    }
                },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (busy) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                else Text("Sincronizar com o relógio")
            }
            status?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
        }
    }
}
