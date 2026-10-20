package com.vitoroliveira.gymtracker.ui.theme

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.PI
import kotlin.math.sin

/** Quão expressivo o tema fica. Vale para todos os temas. */
enum class Intensity(val label: String, val blurb: String) {
    CALM("Calmo", "Efeitos discretos: fundo suave, sem explosões."),
    NORMAL("Normal", "O visual de cada tema como foi desenhado."),
    WILD("Insano", "Tudo mais rápido e vivo: brilhos varrendo a tela, partículas, glitch, halter levantando.")
}

val LocalIntensity = compositionLocalOf { Intensity.NORMAL }

/** Camada extra do modo Insano: faixa de luz diagonal + vinheta pulsando. [t] em 0..1 (volta a cada 20 s). */
fun DrawScope.drawWildOverlay(color: Color, t: Float) {
    val span = size.width + size.height
    val x = ((t * 3f) % 1f) * (span + 400f) - 400f
    drawRect(
        Brush.linearGradient(
            listOf(Color.Transparent, color.copy(alpha = 0.34f), Color.Transparent),
            start = Offset(x, 0f), end = Offset(x + size.height * 0.7f, size.height)
        )
    )
    val pulse = 0.5f + 0.5f * sin(t * 2f * PI.toFloat() * 20f)
    drawRect(
        Brush.radialGradient(
            0.55f to Color.Transparent, 1f to color.copy(alpha = 0.18f + 0.22f * pulse),
            center = center, radius = size.maxDimension * 0.8f
        )
    )
}
