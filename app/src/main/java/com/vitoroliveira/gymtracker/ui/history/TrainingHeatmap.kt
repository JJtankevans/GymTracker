package com.vitoroliveira.gymtracker.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vitoroliveira.gymtracker.presentation.workout.TrainingCalendar
import com.vitoroliveira.gymtracker.ui.theme.ThemedCard

private val DAY_LETTERS = listOf("S", "", "Q", "", "S", "", "D")

/** Mapa de calor dos dias treinados (uma coluna por semana, segunda no topo). */
@Composable
fun TrainingHeatmap(calendar: TrainingCalendar, modifier: Modifier = Modifier) {
    val empty = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    val primary = MaterialTheme.colorScheme.primary
    ThemedCard(modifier = modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium) {
        Column(Modifier.padding(16.dp)) {
            Text("Últimas ${calendar.weeks.size} semanas", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                "${calendar.activeDays} dias treinados · ${calendar.totalWorkouts} treinos",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val labelW = 14.dp
                val gap = 3.dp
                val cols = calendar.weeks.size.coerceAtLeast(1)
                val cell = ((maxWidth - labelW - gap * cols) / cols).coerceIn(6.dp, 22.dp)
                Column {
                    // meses
                    Row {
                        Spacer(Modifier.width(labelW + gap))
                        calendar.monthLabels.forEach { label ->
                            Box(Modifier.width(cell + gap)) {
                                if (label != null) Text(label, fontSize = 9.sp, maxLines = 1, softWrap = false, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    Spacer(Modifier.height(2.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                        Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                            DAY_LETTERS.forEach { l ->
                                Box(Modifier.width(labelW).height(cell), contentAlignment = Alignment.CenterStart) {
                                    Text(l, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                        calendar.weeks.forEach { week ->
                            Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                                week.forEach { day ->
                                    val color = when {
                                        day == null -> Color.Transparent
                                        day.workouts == 0 -> empty
                                        day.workouts == 1 -> primary.copy(alpha = 0.6f)
                                        else -> primary
                                    }
                                    Box(Modifier.size(cell).background(color, RoundedCornerShape(cell / 4)))
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Menos", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                listOf(empty, primary.copy(alpha = 0.6f), primary).forEach {
                    Box(Modifier.size(10.dp).background(it, RoundedCornerShape(2.dp)))
                }
                Text("Mais", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
