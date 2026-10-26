package com.vitoroliveira.gymtracker.ui.muscleGroup

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vitoroliveira.gymtracker.data.model.MuscleGroup
import com.vitoroliveira.gymtracker.presentation.schedule.ScheduleStore
import com.vitoroliveira.gymtracker.presentation.schedule.WEEKDAY_LABELS
import kotlinx.serialization.json.Json

/** Agenda da semana no aparelho (SharedPreferences). */
class PrefsScheduleStore(context: Context) : ScheduleStore {
    private val prefs = context.applicationContext.getSharedPreferences("weekly_schedule", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    override fun load(): Map<Int, String> =
        runCatching { json.decodeFromString<Map<Int, String>>(prefs.getString("plan", null) ?: return emptyMap()) }.getOrDefault(emptyMap())

    override fun save(plan: Map<Int, String>) {
        prefs.edit().putString("plan", json.encodeToString(plan)).apply()
    }
}

/** Escolhe o grupo de cada dia da semana (ou descanso). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ScheduleDialog(
    groups: List<MuscleGroup>,
    plan: Map<Int, String>,
    onSet: (day: Int, groupId: String?) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Agenda da semana") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "O grupo do dia aparece primeiro ao iniciar o treino. Dias sem grupo são descanso.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                WEEKDAY_LABELS.forEachIndexed { i, label ->
                    val day = i + 1
                    Column(Modifier.fillMaxWidth()) {
                        Text(label, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 4.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilterChip(selected = plan[day] == null, onClick = { onSet(day, null) }, label = { Text("Descanso") })
                            groups.forEach { g ->
                                val id = g.id ?: return@forEach
                                FilterChip(selected = plan[day] == id, onClick = { onSet(day, id) }, label = { Text(g.name) })
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { Button(onClick = onDismiss) { Text("Pronto") } }
    )
}
