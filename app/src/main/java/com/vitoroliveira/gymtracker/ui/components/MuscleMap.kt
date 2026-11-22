package com.vitoroliveira.gymtracker.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.vitoroliveira.gymtracker.data.catalog.CatalogMuscle
import com.vitoroliveira.gymtracker.presentation.body.MuscleSets

/**
 * Mapa muscular da semana (#353): duas silhuetas (frente e costas) com cada músculo pintado pela quantidade de
 * séries dos últimos 7 dias, de cinza (nenhuma) até a cor de destaque do tema (meta de hipertrofia ou mais).
 * Desenhado em Canvas com formas simples (cantos arredondados): leve, nítido em qualquer tela e
 * sem imagem para baixar. A cor vem do tema, então combina com qualquer um dos 29.
 */
@Composable
fun MuscleMap(rows: List<MuscleSets>, fullAtSets: Int, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    val neutral = cs.onSurface.copy(alpha = 0.10f)
    val body = cs.onSurface.copy(alpha = 0.06f)
    val setsBy = rows.mapNotNull { r -> r.muscle?.let { it to r.sets } }.toMap()
    fun tint(m: CatalogMuscle): Color {
        val sets = setsBy[m] ?: 0
        if (sets <= 0) return neutral
        val t = (sets.toFloat() / fullAtSets).coerceIn(0f, 1f)
        return lerp(cs.primary.copy(alpha = 0.30f), cs.primary, t)
    }

    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Canvas(Modifier.fillMaxWidth().height(250.dp)) {
            val figW = size.width / 2f
            val s = minOf(figW / 100f, size.height / 200f)
            val left = Offset((figW - 100f * s) / 2f, 0f)
            val right = Offset(figW + (figW - 100f * s) / 2f, 0f)
            drawFront(left, s, body, ::tint)
            drawBack(right, s, body, ::tint)
        }
        Row(Modifier.fillMaxWidth()) {
            Text("Frente", Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelMedium, color = cs.onSurfaceVariant)
            Text("Costas", Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelMedium, color = cs.onSurfaceVariant)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Text("Cinza = nenhuma série · cor cheia = $fullAtSets séries ou mais", style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant)
        }
    }
}

// Todas as medidas em "unidades de figura" (100 × 200); `o` é o canto da figura e `s` a escala.
private fun DrawScope.part(o: Offset, s: Float, x: Float, y: Float, w: Float, h: Float, c: Color, r: Float = 6f) {
    drawRoundRect(c, Offset(o.x + x * s, o.y + y * s), Size(w * s, h * s), CornerRadius(r * s, r * s))
}

private fun DrawScope.silhouette(o: Offset, s: Float, c: Color) {
    drawCircle(c, 11f * s, Offset(o.x + 50f * s, o.y + 14f * s))      // cabeça
    part(o, s, 45f, 24f, 10f, 8f, c, 3f)                              // pescoço
    part(o, s, 27f, 32f, 46f, 84f, c, 14f)                            // tronco
    part(o, s, 12f, 40f, 15f, 60f, c, 7f); part(o, s, 73f, 40f, 15f, 60f, c, 7f)       // braços
    part(o, s, 10f, 98f, 11f, 34f, c, 5f); part(o, s, 79f, 98f, 11f, 34f, c, 5f)       // antebraços
    part(o, s, 31f, 114f, 38f, 24f, c, 10f)                           // quadril
    part(o, s, 30f, 130f, 18f, 68f, c, 8f); part(o, s, 52f, 130f, 18f, 68f, c, 8f)    // pernas
}

private fun DrawScope.drawFront(o: Offset, s: Float, base: Color, tint: (CatalogMuscle) -> Color) {
    silhouette(o, s, base)
    part(o, s, 17f, 34f, 18f, 18f, tint(CatalogMuscle.OMBROS), 9f); part(o, s, 65f, 34f, 18f, 18f, tint(CatalogMuscle.OMBROS), 9f)
    part(o, s, 34f, 40f, 15f, 18f, tint(CatalogMuscle.PEITO), 7f); part(o, s, 51f, 40f, 15f, 18f, tint(CatalogMuscle.PEITO), 7f)
    part(o, s, 12f, 54f, 13f, 26f, tint(CatalogMuscle.BICEPS), 6f); part(o, s, 75f, 54f, 13f, 26f, tint(CatalogMuscle.BICEPS), 6f)
    part(o, s, 38f, 62f, 24f, 40f, tint(CatalogMuscle.ABDOMEN), 8f)
    part(o, s, 30f, 120f, 18f, 46f, tint(CatalogMuscle.QUADRICEPS), 8f); part(o, s, 52f, 120f, 18f, 46f, tint(CatalogMuscle.QUADRICEPS), 8f)
    part(o, s, 31f, 170f, 15f, 28f, tint(CatalogMuscle.PANTURRILHA), 6f); part(o, s, 54f, 170f, 15f, 28f, tint(CatalogMuscle.PANTURRILHA), 6f)
}

private fun DrawScope.drawBack(o: Offset, s: Float, base: Color, tint: (CatalogMuscle) -> Color) {
    silhouette(o, s, base)
    part(o, s, 17f, 34f, 18f, 18f, tint(CatalogMuscle.OMBROS), 9f); part(o, s, 65f, 34f, 18f, 18f, tint(CatalogMuscle.OMBROS), 9f)
    part(o, s, 34f, 36f, 32f, 44f, tint(CatalogMuscle.COSTAS), 10f)
    part(o, s, 12f, 54f, 13f, 26f, tint(CatalogMuscle.TRICEPS), 6f); part(o, s, 75f, 54f, 13f, 26f, tint(CatalogMuscle.TRICEPS), 6f)
    part(o, s, 33f, 100f, 17f, 20f, tint(CatalogMuscle.GLUTEOS), 8f); part(o, s, 50f, 100f, 17f, 20f, tint(CatalogMuscle.GLUTEOS), 8f)
    part(o, s, 30f, 122f, 18f, 44f, tint(CatalogMuscle.POSTERIOR), 8f); part(o, s, 52f, 122f, 18f, 44f, tint(CatalogMuscle.POSTERIOR), 8f)
    part(o, s, 31f, 170f, 15f, 28f, tint(CatalogMuscle.PANTURRILHA), 6f); part(o, s, 54f, 170f, 15f, 28f, tint(CatalogMuscle.PANTURRILHA), 6f)
}
