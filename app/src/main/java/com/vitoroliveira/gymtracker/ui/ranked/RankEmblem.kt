package com.vitoroliveira.gymtracker.ui.ranked

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.vitoroliveira.gymtracker.presentation.ranked.RankTier
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Cor do elo. */
fun RankTier.color(): Color = Color(colorHex)

/**
 * Emblema do elo, desenhado: escudo na cor do elo, gema no centro, [division] vira marcas em cima (III = 1 marca,
 * II = 2, I = 3), asas a partir da Prata e, na Lenda, coroa e raios girando.
 */
@Composable
fun RankEmblem(tier: RankTier, division: Int, size: Dp = 120.dp, modifier: Modifier = Modifier) {
    val color = tier.color()
    val dark = lerp(color, Color.Black, 0.6f)
    val tr = rememberInfiniteTransition(label = "emblem")
    val glow by tr.animateFloat(0.35f, 0.95f, infiniteRepeatable(tween(1700, easing = LinearEasing), RepeatMode.Reverse), label = "glow")
    val spin by tr.animateFloat(0f, 360f, infiniteRepeatable(tween(16000, easing = LinearEasing)), label = "spin")
    Canvas(modifier.size(size)) {
        val w = this.size.width; val h = this.size.height
        val c = Offset(w / 2, h / 2)
        drawCircle(Brush.radialGradient(listOf(color.copy(alpha = 0.40f * glow), Color.Transparent), c, w * 0.62f), w * 0.62f, c)
        if (tier == RankTier.LEGEND) {
            rotate(spin, c) {
                for (i in 0 until 12) {
                    val a = i * 30f * PI.toFloat() / 180f
                    drawLine(color.copy(alpha = 0.55f), Offset(c.x + cos(a) * w * 0.46f, c.y + sin(a) * w * 0.46f), Offset(c.x + cos(a) * w * 0.62f, c.y + sin(a) * w * 0.62f), 3.dp.toPx(), StrokeCap.Round)
                }
            }
        }
        // asas: duas penas de cada lado por elo acima do Bronze
        val wings = (tier.ordinal - 1).coerceIn(0, 4)
        for (side in listOf(-1f, 1f)) {
            for (k in 0 until wings) {
                val y = h * (0.30f + 0.10f * k)
                val reach = w * (0.20f - 0.02f * k)
                val p = Path().apply {
                    moveTo(c.x + side * w * 0.36f, y)
                    lineTo(c.x + side * (w * 0.36f + reach), y - h * 0.06f)
                    lineTo(c.x + side * (w * 0.36f + reach * 0.7f), y + h * 0.03f)
                    lineTo(c.x + side * w * 0.36f, y + h * 0.05f)
                    close()
                }
                drawPath(p, color.copy(alpha = 0.75f))
            }
        }
        // escudo
        val shield = Path().apply {
            moveTo(w * 0.5f, h * 0.08f); lineTo(w * 0.86f, h * 0.20f); lineTo(w * 0.82f, h * 0.58f)
            quadraticTo(w * 0.76f, h * 0.82f, w * 0.5f, h * 0.94f)
            quadraticTo(w * 0.24f, h * 0.82f, w * 0.18f, h * 0.58f); lineTo(w * 0.14f, h * 0.20f); close()
        }
        drawPath(shield, Brush.verticalGradient(listOf(color, dark), h * 0.08f, h * 0.94f))
        drawPath(shield, Color.White.copy(alpha = 0.35f), style = Stroke(2.dp.toPx()))
        // miolo mais escuro
        val inner = Path().apply {
            moveTo(w * 0.5f, h * 0.16f); lineTo(w * 0.77f, h * 0.25f); lineTo(w * 0.74f, h * 0.56f)
            quadraticTo(w * 0.70f, h * 0.75f, w * 0.5f, h * 0.85f)
            quadraticTo(w * 0.30f, h * 0.75f, w * 0.26f, h * 0.56f); lineTo(w * 0.23f, h * 0.25f); close()
        }
        drawPath(inner, dark.copy(alpha = 0.85f))
        // gema central
        val gem = Path().apply {
            moveTo(w * 0.5f, h * 0.34f); lineTo(w * 0.64f, h * 0.52f); lineTo(w * 0.5f, h * 0.72f); lineTo(w * 0.36f, h * 0.52f); close()
        }
        drawPath(gem, Brush.linearGradient(listOf(Color.White.copy(alpha = 0.9f), color), Offset(w * 0.4f, h * 0.34f), Offset(w * 0.6f, h * 0.72f)))
        drawPath(gem, Color.White.copy(alpha = 0.6f), style = Stroke(1.5.dp.toPx()))
        // divisão: marcas (chevrons) em cima; Lenda ganha coroa
        if (tier == RankTier.LEGEND) {
            val crown = Path().apply {
                moveTo(w * 0.34f, h * 0.30f); lineTo(w * 0.40f, h * 0.18f); lineTo(w * 0.5f, h * 0.27f); lineTo(w * 0.60f, h * 0.18f); lineTo(w * 0.66f, h * 0.30f); close()
            }
            drawPath(crown, Color.White.copy(alpha = 0.95f))
        } else {
            val marks = 4 - division.coerceIn(1, 3)
            val startX = c.x - (marks - 1) * w * 0.045f
            for (m in 0 until marks) {
                val mx = startX + m * w * 0.09f
                val chevron = Path().apply { moveTo(mx - w * 0.035f, h * 0.30f); lineTo(mx, h * 0.25f); lineTo(mx + w * 0.035f, h * 0.30f) }
                drawPath(chevron, Color.White.copy(alpha = 0.95f), style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round))
            }
        }
    }
}
