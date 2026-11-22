package com.vitoroliveira.gymtracker.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vitoroliveira.gymtracker.ui.theme.animationsEnabled
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Botão de reação com o "estalo" do `like` do OriginKit (porte fiel do miolo; `Runtime/docs/originkit-cache/like.txt`):
 * mola do toque (rigidez 600, amortecimento 30, escala 0,94), ícone que achata e estala em 5 quadros-chave
 * (escala 1 → .62 → 1,38 → .94 → 1 em 0,55 s), anel que cresce de .3 a 2,4× e some (0,55 s) e 12 partículas
 * (pontos e traços) lançadas em círculo com PRNG de semente fixa (mulberry32(2025)), como no original.
 * Sem anel nem partículas se o sistema pediu "remover animações".
 */
@Composable
fun LikeReactionButton(
    icon: ImageVector,
    label: String,
    count: Int,
    on: Boolean,
    thumbStyle: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cs = MaterialTheme.colorScheme
    val context = LocalContext.current
    val reduce = remember(context) { !animationsEnabled(context) }
    val tint = if (on) cs.primary else cs.onSurfaceVariant
    val shape = RoundedCornerShape(50)
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val press by animateFloatAsState(if (pressed) 0.94f else 1f, spring(dampingRatio = 0.61f, stiffness = 600f), label = "like-press")

    // 0 → 1 a cada mudança de estado (não dispara na primeira composição)
    val burst = remember { Animatable(1f) }
    var first by remember { mutableStateOf(true) }
    LaunchedEffect(on) {
        if (first) { first = false; return@LaunchedEffect }
        if (reduce) return@LaunchedEffect
        burst.snapTo(0f)
        burst.animateTo(1f, tween(if (on) TOTAL_ON_MS else TOTAL_OFF_MS, easing = LinearEasing))
    }
    val seconds = burst.value * (if (on) TOTAL_ON_MS else TOTAL_OFF_MS) / 1000f

    Box(modifier, contentAlignment = Alignment.CenterStart) {
        Row(
            Modifier
                .graphicsLayer { scaleX = press; scaleY = press }
                .background(if (on) cs.primary.copy(alpha = 0.16f) else Color.Transparent, shape)
                .border(BorderStroke(1.dp, if (on) cs.primary else cs.outlineVariant), shape)
                .clickable(interactionSource = source, indication = null, onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(ICON_DP.dp)) {
                // ícone: achata e estala (liga) ou só afunda e volta (desliga)
                val iconScale: Float
                val iconRot: Float
                if (on) {
                    val u = (seconds / 0.55f).coerceIn(0f, 1f)
                    iconScale = frames(u, ON_TIMES, ON_SCALE)
                    iconRot = frames(u, ON_TIMES, if (thumbStyle) ROT_THUMB else ROT_HEART)
                } else {
                    val u = (seconds / 0.22f).coerceIn(0f, 1f)
                    iconScale = frames(u, OFF_TIMES, OFF_SCALE)
                    iconRot = 0f
                }
                Icon(
                    icon, label,
                    Modifier.size(ICON_DP.dp).graphicsLayer {
                        scaleX = iconScale; scaleY = iconScale; rotationZ = iconRot
                        transformOrigin = if (thumbStyle) TransformOrigin(0.3f, 0.9f) else TransformOrigin(0.5f, 0.55f)
                    },
                    tint = tint
                )
            }
            if (count > 0) {
                Spacer(Modifier.width(6.dp))
                Text("$count", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = tint)
            }
        }
        // anel e partículas desenhados por cima, fora do recorte da pílula, centrados no ícone
        if (on && !reduce && burst.value < 1f) {
            val accent = cs.primary
            // caixa de tamanho ZERO no centro do ícone: o desenho (maior que a pílula) não mexe no layout da linha
            Box(Modifier.align(Alignment.CenterStart).offset(x = (12f + ICON_DP / 2f).dp).size(0.dp)) {
            Canvas(Modifier.requiredSize(140.dp)) {
                val c = Offset(size.width / 2f, size.height / 2f)
                val dp = density
                // anel
                val ringU = (seconds / 0.55f).coerceIn(0f, 1f)
                val ringE = BEZIER.transform(ringU)
                val ringScale = 0.3f + (2.4f - 0.3f) * ringE
                val ringAlpha = 0.55f * (1f - ringE)
                val baseR = (ICON_DP / 2f + 2f) * dp
                drawCircle(accent.copy(alpha = ringAlpha), radius = baseR * ringScale, center = c, style = Stroke(width = 2f * dp))
                // partículas
                PARTICLES.forEach { q ->
                    val pu = ((seconds - q.delay) / 0.62f).coerceIn(0f, 1f)
                    if (pu <= 0f) return@forEach
                    val e = BEZIER.transform(pu)
                    val pos = Offset(c.x + q.dx * dp * e, c.y + q.dy * dp * e)
                    val s = frames(pu, P_TIMES, P_SCALE)
                    val a = frames(pu, P_TIMES, P_ALPHA)
                    val col = accent.copy(alpha = a.coerceIn(0f, 1f))
                    if (q.dot) {
                        drawCircle(col, radius = q.size * dp * s / 2f, center = pos)
                    } else {
                        rotate(q.angleDeg, pivot = pos) {
                            val len = q.size * 1.9f * dp * s
                            drawRoundRect(col, Offset(pos.x - len / 2f, pos.y - 1f * dp), Size(len, 2f * dp), CornerRadius(1f * dp))
                        }
                    }
                }
            }
            }
        }
    }
}

// ───────── constantes e funções do original ─────────
private const val ICON_DP = 20f
private const val TOTAL_ON_MS = 660
private const val TOTAL_OFF_MS = 220
private val ON_TIMES = floatArrayOf(0f, 0.18f, 0.5f, 0.78f, 1f)
private val ON_SCALE = floatArrayOf(1f, 0.62f, 1.38f, 0.94f, 1f)
private val ROT_THUMB = floatArrayOf(0f, 8f, -22f, 4f, 0f)
private val ROT_HEART = floatArrayOf(0f, -6f, 6f, -2f, 0f)
private val OFF_TIMES = floatArrayOf(0f, 0.4f, 1f)
private val OFF_SCALE = floatArrayOf(1f, 0.82f, 1f)
private val P_TIMES = floatArrayOf(0f, 0.35f, 1f)
private val P_SCALE = floatArrayOf(0f, 1.1f, 0.2f)
private val P_ALPHA = floatArrayOf(1f, 1f, 0f)
private val BEZIER = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
private val EASE_OUT = CubicBezierEasing(0f, 0f, 0.58f, 1f)

/** Quadros-chave com easeOut por trecho (framer-motion `times` + `ease: "easeOut"`). */
private fun frames(u: Float, times: FloatArray, values: FloatArray): Float {
    for (i in 0 until times.size - 1) {
        if (u <= times[i + 1]) {
            val seg = ((u - times[i]) / (times[i + 1] - times[i])).coerceIn(0f, 1f)
            return values[i] + (values[i + 1] - values[i]) * EASE_OUT.transform(seg)
        }
    }
    return values.last()
}

private class Particle(val dx: Float, val dy: Float, val size: Float, val angleDeg: Float, val dot: Boolean, val delay: Float)

/** mulberry32 do original (aritmética de 32 bits sem sinal). */
private class Mulberry32(seed: Int) {
    private var a: UInt = seed.toUInt()
    fun next(): Float {
        a += 0x6d2b79f5u
        var t = a
        t = (t xor (t shr 15)) * (t or 1u)
        t = t xor (t + (t xor (t shr 7)) * (t or 61u))
        return ((t xor (t shr 14)).toDouble() / 4294967296.0).toFloat()
    }
}

private val PARTICLES: List<Particle> = run {
    val rand = Mulberry32(2025)
    List(12) { i ->
        val angle = (i / 12f) * (2f * PI.toFloat()) + (rand.next() - 0.5f) * 0.35f
        val dist = 30f + rand.next() * 16f
        val size = 3.5f + rand.next() * 3.5f
        rand.next() // "tint" (não usado: a partícula leva a cor de destaque do tema)
        val delay = rand.next() * 0.04f
        Particle(cos(angle) * dist, sin(angle) * dist, size, angle * 180f / PI.toFloat(), i % 2 == 0, delay)
    }
}
