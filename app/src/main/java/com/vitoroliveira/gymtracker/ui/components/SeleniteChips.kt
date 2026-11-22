package com.vitoroliveira.gymtracker.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vitoroliveira.gymtracker.ui.theme.animationsEnabled
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/**
 * PORTE FIEL (miolo) do `selenite-chips` do OriginKit (cache em Runtime/docs/originkit-cache/selenite-chips.txt):
 * uma fileira de chips de múltipla escolha; ao marcar um, o vidro entra com um check à esquerda enquanto o chip
 * alarga (o rótulo desliza para a direita), e ao desmarcar o vidro escoa primeiro e só depois o chip estreita.
 *
 * Mantido do original (medidas em pt do clipe, aqui multiplicadas por [SCALE] para o dedo): chip 20 de altura,
 * raio 4, 7 de intervalo, folga lateral 8,2; check 8 × 6,3 com traço 1,4, a 6,3 da borda e 4,9 antes do rótulo;
 * pontos de giz (passo 2, lado 0,3); curvas e tempos: cor entra (0,1; 1; 0,2; 1) em 0,27 s e alarga
 * (0,7; 1; 0,3; 0,8) em 0,21 s; ao soltar a cor escoa (0,3; 1; 0,2; 1) em 0,14 s e estreita (0,6; 0,8; 0,4; 1) em 0,32 s;
 * o toque escurece o chip. O vidro é desenhado: tinta forte no alto, faixa de luz, pé mais escuro, aro claro no alto.
 * Trocado: fundo preto/cinza e rótulo cinza viram as cores do tema; o vidro leva a cor de destaque do tema.
 * Respeita "remover animações" (muda sem animar).
 */
private const val SCALE = 1.6f
private const val CHIP_H = 20f
private const val RADIUS = 4f
private const val GAP = 7f
private const val PAD = 8.2f
private const val CHECK_W = 8f
private const val CHECK_H = 6.3f
private const val CHECK_STROKE = 1.4f
private const val CHECK_LEFT = 6.3f
private const val CHECK_GAP = 4.9f
private const val DOT_PITCH = 2f
private const val DOT_SIZE = 0.3f
private const val TEXT_PT = 10.5f
private const val PRESS_SHADE = 0.10f

private val TintIn = CubicBezierEasing(0.1f, 1f, 0.2f, 1f)
private val WidenIn = CubicBezierEasing(0.7f, 1f, 0.3f, 0.8f)
private val TintOut = CubicBezierEasing(0.3f, 1f, 0.2f, 1f)
private val WidenOut = CubicBezierEasing(0.6f, 0.8f, 0.4f, 1f)

/** Quanto o chip alarga marcado: do check à esquerda até o rótulo, menos a folga que já existia. */
private const val EXTRA = CHECK_LEFT + CHECK_W + CHECK_GAP - PAD

@Composable
fun SeleniteChips(
    labels: List<String>,
    selected: Set<Int>,
    onToggle: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy((GAP * SCALE).dp)
    ) {
        labels.forEachIndexed { i, label -> SeleniteChip(label, i in selected) { onToggle(i) } }
    }
}

@Composable
private fun SeleniteChip(label: String, picked: Boolean, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val context = androidx.compose.ui.platform.LocalContext.current
    val animate = remember(context) { animationsEnabled(context) }
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer()
    val textStyle = TextStyle(fontSize = (TEXT_PT * SCALE).sp, fontWeight = FontWeight.Medium)
    val textW = remember(label, density) {
        with(density) { measurer.measure(label, textStyle).size.width.toDp() }
    }
    val widen = remember { Animatable(if (picked) 1f else 0f) }
    val tint = remember { Animatable(if (picked) 1f else 0f) }
    LaunchedEffect(picked, animate) {
        val toValue = if (picked) 1f else 0f
        if (!animate) { widen.snapTo(toValue); tint.snapTo(toValue); return@LaunchedEffect }
        // marcar: cor e alargamento juntos; desmarcar: a cor escoa antes, o chip estreita depois (mais lento)
        launchBoth(
            { tint.animateTo(toValue, tween(if (picked) 270 else 140, easing = if (picked) TintIn else TintOut)) },
            { widen.animateTo(toValue, tween(if (picked) 210 else 320, easing = if (picked) WidenIn else WidenOut)) }
        )
    }
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val press by androidx.compose.animation.core.animateFloatAsState(if (pressed) 1f else 0f, tween(60), label = "press")

    val padDp = (PAD * SCALE).dp
    val width: Dp = textW + padDp * 2 + ((EXTRA * SCALE) * widen.value).dp
    val base = lerp(cs.surface, cs.onSurface, 0.07f)
    val label0 = cs.onSurfaceVariant
    val labelColor = lerp(label0, cs.onSurface, tint.value)

    Box(
        Modifier
            .width(width)
            .height((CHIP_H * SCALE).dp)
            .toggleable(value = picked, interactionSource = source, indication = null, role = Role.Checkbox, onValueChange = { onClick() })
    ) {
        Canvas(Modifier.width(width).height((CHIP_H * SCALE).dp)) {
            val s = SCALE * density.density
            val w = size.width
            val h = size.height
            val r = CornerRadius(RADIUS * s)
            // chip + pontos de giz (aparecem através do vidro)
            drawRoundRect(base, size = Size(w, h), cornerRadius = r)
            val dot = (DOT_SIZE * s).coerceAtLeast(0.6f)
            val pitch = DOT_PITCH * s
            val dotColor = cs.onSurface.copy(alpha = 0.16f)
            var y = pitch / 2
            while (y < h) {
                var x = pitch / 2
                while (x < w) { drawCircle(dotColor, dot, Offset(x, y)); x += pitch }
                y += pitch
            }
            val t = tint.value
            if (t > 0f) {
                val glass = cs.primary
                // tinta: forte no alto, mais fraca no pé; faixa de luz no alto; pé mais escuro
                drawRoundRect(Brush.verticalGradient(listOf(glass.copy(alpha = 0.42f * t), glass.copy(alpha = 0.16f * t))), size = Size(w, h), cornerRadius = r)
                drawRoundRect(Brush.verticalGradient(0f to Color.White.copy(alpha = 0.22f * t), 0.5f to Color.Transparent), size = Size(w, h), cornerRadius = r)
                drawRoundRect(Brush.verticalGradient(0.7f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.18f * t)), size = Size(w, h), cornerRadius = r)
                // aro claro no alto
                drawRoundRect(Brush.verticalGradient(0f to Color.White.copy(alpha = 0.55f * t), 0.45f to Color.Transparent), size = Size(w, h), cornerRadius = r, style = Stroke(0.35f * s))
            }
            if (press > 0f) drawRoundRect(Color.Black.copy(alpha = PRESS_SHADE * press), size = Size(w, h), cornerRadius = r)
            // aresta fina
            drawRoundRect(cs.onSurface.copy(alpha = 0.10f), size = Size(w, h), cornerRadius = r, style = Stroke(0.5f * s))

            // check à esquerda (dois traços), aparece com a cor
            if (t > 0.01f) {
                val cx = CHECK_LEFT * s
                val cy = (h - CHECK_H * s) / 2
                val p = Path().apply {
                    moveTo(cx, cy + CHECK_H * s * 0.55f)
                    lineTo(cx + CHECK_W * s * 0.36f, cy + CHECK_H * s)
                    lineTo(cx + CHECK_W * s, cy)
                }
                drawPath(p, cs.onSurface.copy(alpha = t), style = Stroke(CHECK_STROKE * s, cap = StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round))
            }
            // rótulo desliza para a direita do tanto que o chip alargou
            val laid = measurer.measure(label, textStyle.copy(color = labelColor))
            drawText(laid, topLeft = Offset((PAD + EXTRA * widen.value) * s, (h - laid.size.height) / 2f))
        }
    }
}

private suspend fun launchBoth(a: suspend () -> Unit, b: suspend () -> Unit) = coroutineScope {
    launch { a() }
    launch { b() }
}
