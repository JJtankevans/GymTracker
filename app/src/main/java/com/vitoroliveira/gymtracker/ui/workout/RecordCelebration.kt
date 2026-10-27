package com.vitoroliveira.gymtracker.ui.workout

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vitoroliveira.gymtracker.ui.theme.Intensity
import com.vitoroliveira.gymtracker.ui.theme.LocalIntensity
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private val PHRASES = listOf(
    "NOVO RECORDE!", "SUBIU A AURA!", "MOGGOU A BARRA!", "MODO GIGA CHAD", "O FERRO OBEDECEU"
)

/**
 * Explosão de confete + frase quando [records] aumenta (bateu recorde de carga no treino).
 * Não intercepta toques: é só desenho por cima da tela. No modo "Calmo" fica só a frase.
 */
@Composable
fun RecordCelebration(records: Int, modifier: Modifier = Modifier) {
    var seen by remember { mutableIntStateOf(records) }
    var burst by remember { mutableIntStateOf(0) }
    var phrase by remember { mutableStateOf(PHRASES[0]) }
    val progress = remember { Animatable(1f) }
    val haptics = LocalHapticFeedback.current
    val calm = LocalIntensity.current == Intensity.CALM

    LaunchedEffect(records) {
        if (records > seen) {
            burst++
            phrase = if (burst == 1) PHRASES[0] else PHRASES.random()
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            progress.snapTo(0f)
            progress.animateTo(1f, tween(2600, easing = LinearEasing))
        }
        seen = records
    }

    val p = progress.value
    if (p >= 1f) return
    val colors = listOf(
        MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.tertiary, Color(0xFFFFD23F), Color.White
    )
    Box(modifier.fillMaxSize()) {
        if (!calm) {
            Canvas(Modifier.fillMaxSize()) {
                val rnd = Random(burst)
                val origin = Offset(size.width / 2f, size.height * 0.38f)
                val gravity = size.height * 1.1f
                repeat(90) { i ->
                    val ang = rnd.nextFloat() * 2f * PI.toFloat()
                    val speed = size.minDimension * (0.5f + rnd.nextFloat() * 0.9f)
                    val t = p * 1.6f
                    val pos = origin + Offset(cos(ang) * speed * t, sin(ang) * speed * t + 0.5f * gravity * t * t)
                    val s = (5 + rnd.nextInt(7)).dp.toPx()
                    val fade = (1f - p).coerceIn(0f, 1f)
                    rotate(rnd.nextFloat() * 360f + p * 900f * (if (i % 2 == 0) 1 else -1), pos) {
                        drawRect(colors[i % colors.size].copy(alpha = fade), pos - Offset(s / 2, s / 4), Size(s, s / 2))
                    }
                }
            }
        }
        // a frase entra grande, assenta e some
        val scale = when {
            p < 0.12f -> 0.4f + (p / 0.12f) * 0.8f
            p < 0.22f -> 1.2f - ((p - 0.12f) / 0.10f) * 0.2f
            else -> 1f
        }
        val alpha = if (p > 0.75f) (1f - p) / 0.25f else 1f
        Text(
            phrase,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 24.dp)
                .scale(scale)
                .alpha(alpha)
                .graphicsLayer { rotationZ = -6f },
            color = MaterialTheme.colorScheme.primary,
            fontSize = 40.sp,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.displaySmall.copy(
                shadow = androidx.compose.ui.graphics.Shadow(Color.Black.copy(alpha = 0.6f), Offset(4f, 6f), 8f)
            )
        )
    }
}
