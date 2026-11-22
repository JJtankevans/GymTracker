package com.vitoroliveira.gymtracker.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Número que "rola" até o valor novo (porte do `rolling-number` do Cult UI, `Runtime/docs/cultui-cache`).
 *
 * Miolo igual ao original: mola com massa 0,8, rigidez 75 e amortecimento 15 (`useSpring`), texto = valor
 * animado arredondado. O Compose usa massa 1 nas molas, então a rigidez equivalente é k/m = 75/0,8 e a razão
 * de amortecimento é c / (2·√(k·m)).
 *
 * Também conta de 0 até o valor na primeira vez que aparece (bom para a Home abrindo). Respeita o tamanho de
 * fonte do tema; números tabulares para os dígitos não "dançarem" de largura.
 */
@Composable
fun RollingNumber(
    value: Int,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    fontWeight: FontWeight? = null,
    suffix: String = ""
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    // DESIGN.md §3: respeitar "remover animações" do sistema (o número aparece direto, sem rolar).
    val reduceMotion = remember(context) { !com.vitoroliveira.gymtracker.ui.theme.animationsEnabled(context) }
    val animated = remember { Animatable(if (reduceMotion) value.toFloat() else 0f) }
    LaunchedEffect(value, reduceMotion) {
        if (reduceMotion) animated.snapTo(value.toFloat())
        else animated.animateTo(
            value.toFloat(),
            spring(dampingRatio = SPRING_DAMPING_RATIO, stiffness = SPRING_STIFFNESS)
        )
    }
    val shown = animated.value.roundToInt().coerceAtLeast(0)
    Row(modifier) {
        Text(
            text = shown.toString() + suffix,
            style = style.copy(fontFeatureSettings = "tnum"),
            color = color,
            fontWeight = fontWeight
        )
    }
}

// Cult UI: massa .8 · rigidez 75 · amortecimento 15 (framer-motion) → equivalentes no Compose (massa 1).
private const val MASS = 0.8f
private const val STIFFNESS = 75f
private const val DAMPING = 15f
private val SPRING_STIFFNESS = STIFFNESS / MASS
private val SPRING_DAMPING_RATIO = DAMPING / (2f * sqrt(STIFFNESS * MASS))
