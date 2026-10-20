package com.vitoroliveira.gymtracker.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.vitoroliveira.gymtracker.ui.theme.Intensity
import com.vitoroliveira.gymtracker.ui.theme.LocalIntensity
import com.vitoroliveira.gymtracker.ui.theme.LocalThemeExtras
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** Explosão de partículas nas cores do tema. Dispara quando [trigger] muda (e é > 0). Quantidade segue a intensidade. */
@Composable
fun ConfettiBurst(trigger: Int, modifier: Modifier = Modifier.fillMaxSize(), originY: Float = 0.62f) {
    val intensity = LocalIntensity.current
    val cs = MaterialTheme.colorScheme
    val colors = listOf(cs.primary, cs.tertiary, cs.secondary, LocalThemeExtras.current.success)
    val progress = remember { Animatable(1f) }
    var seed by remember { mutableIntStateOf(0) }
    LaunchedEffect(trigger) {
        if (trigger > 0) {
            seed = trigger
            progress.snapTo(0f)
            progress.animateTo(1f, tween(if (intensity == Intensity.WILD) 1500 else 950, easing = LinearEasing))
        }
    }
    val count = when (intensity) { Intensity.CALM -> 10; Intensity.NORMAL -> 30; Intensity.WILD -> 80 }
    Canvas(modifier) {
        val p = progress.value
        if (p >= 1f) return@Canvas
        val rnd = Random(seed)
        val ox = size.width / 2; val oy = size.height * originY
        repeat(count) { i ->
            val ang = (-PI * (0.08 + rnd.nextDouble() * 0.84)).toFloat()
            val speed = (380f + rnd.nextFloat() * 900f) * (if (intensity == Intensity.WILD) 1.25f else 1f)
            val x = ox + cos(ang) * speed * p
            val y = oy + sin(ang) * speed * p + 1100f * p * p
            val s = 6.dp.toPx() + rnd.nextFloat() * 6.dp.toPx()
            val c = colors[i % colors.size].copy(alpha = (1f - p).coerceIn(0f, 1f))
            rotate(p * 540f * (if (i % 2 == 0) 1 else -1), Offset(x, y)) {
                drawRoundRect(c, Offset(x - s / 2, y - s / 2), Size(s, s * 0.6f), CornerRadius(2.dp.toPx()))
            }
        }
    }
}

/** Barras de equalizador. Se [playing], dançam; senão ficam baixas. (Animação decorativa, não é análise do áudio.) */
@Composable
fun EqualizerBars(playing: Boolean, color: Color, modifier: Modifier = Modifier.size(22.dp)) {
    val tr = rememberInfiniteTransition(label = "eq")
    val periods = listOf(420, 560, 360, 640)
    val vals = periods.mapIndexed { i, ms ->
        tr.animateFloat(0.2f, 1f, infiniteRepeatable(tween(ms, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "b$i")
    }
    Canvas(modifier) {
        val n = vals.size
        val gap = size.width * 0.12f
        val w = (size.width - gap * (n - 1)) / n
        vals.forEachIndexed { i, v ->
            val h = size.height * if (playing) v.value else 0.18f
            drawRoundRect(color, Offset(i * (w + gap), size.height - h), Size(w, h), CornerRadius(w / 2))
        }
    }
}

/** Texto com glitch RGB que "estoura" por um instante a cada poucos segundos. */
@Composable
fun GlitchText(text: String, style: TextStyle, color: Color, modifier: Modifier = Modifier, active: Boolean = true) {
    if (!active) { Text(text, modifier, color = color, style = style); return }
    val tr = rememberInfiniteTransition(label = "glitch")
    val phase by tr.animateFloat(0f, 1f, infiniteRepeatable(tween(3800, easing = LinearEasing)), label = "p")
    fun amount(p: Float) = if (p > 0.92f) sin(p * 220f) * 9f else 0f
    Box(modifier) {
        Text(text, color = Color(0xFFFF2B6B), style = style, modifier = Modifier.graphicsLayer {
            translationX = amount(phase); alpha = if (phase > 0.92f) 0.75f else 0f
        })
        Text(text, color = Color(0xFF22E3FF), style = style, modifier = Modifier.graphicsLayer {
            translationX = -amount(phase); alpha = if (phase > 0.92f) 0.75f else 0f
        })
        Text(text, color = color, style = style, modifier = Modifier.graphicsLayer { translationY = if (phase > 0.95f) 2f else 0f })
    }
}

/** Halter desenhado que sobe e desce sozinho. */
@Composable
fun LiftingDumbbell(color: Color, modifier: Modifier = Modifier.size(width = 140.dp, height = 78.dp)) {
    val tr = rememberInfiniteTransition(label = "dumbbell")
    val up by tr.animateFloat(0f, 1f, infiniteRepeatable(tween(850, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "up")
    Canvas(modifier) {
        val w = size.width; val h = size.height
        val lift = h * 0.30f * up
        val cy = h * 0.52f - lift
        val barH = h * 0.10f
        // sombra
        drawOval(Color.Black.copy(alpha = 0.25f * (1f - up * 0.6f)), Offset(w * 0.18f + up * w * 0.04f, h * 0.92f), Size(w * 0.64f - up * w * 0.08f, h * 0.07f))
        // barra
        drawRoundRect(color.copy(alpha = 0.85f), Offset(w * 0.12f, cy - barH / 2), Size(w * 0.76f, barH), CornerRadius(barH / 2))
        // anilhas (grandes por dentro, pequenas por fora)
        listOf(0.17f to 0.58f, 0.25f to 0.42f).forEach { (x, f) ->
            val ph = h * f; val pw = w * 0.065f
            drawRoundRect(color, Offset(w * x - pw / 2, cy - ph / 2), Size(pw, ph), CornerRadius(pw / 2.5f))
            drawRoundRect(color, Offset(w * (1f - x) - pw / 2, cy - ph / 2), Size(pw, ph), CornerRadius(pw / 2.5f))
        }
    }
}
