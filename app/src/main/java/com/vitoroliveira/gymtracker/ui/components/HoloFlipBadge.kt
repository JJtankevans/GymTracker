package com.vitoroliveira.gymtracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.fillMaxSize
import com.vitoroliveira.gymtracker.ui.theme.animationsEnabled
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * PORTE FIEL (miolo) do `holo-flip-badge` do OriginKit (cache em Runtime/docs/originkit-cache).
 * Mantido do original: molas do giro (rigidez 60, amortecimento 16, em graus), do balanço (48 e 20/10, chute 70),
 * meia-volta (180°) por toque somando voltas, aresta do balanço em repouso (1°), integração por passo fixo de 1/240 s,
 * verso holográfico com gradiente 125° (rosa, azul, menta, branco, amarelo, lavanda) que desliza com o giro (pos = 50 + fs*30)
 * e brilho (glare) diagonal. Trocado: React/DOM por Compose, inclinação por ponteiro (não existe em toque),
 * texto do cartão (aqui o verso é um selo) e o fundo (tokens do tema na frente).
 */
@Composable
fun HoloFlipBadge(
    icon: ImageVector,
    front: Color,
    onFront: Color,
    size: Dp,
    modifier: Modifier = Modifier,
    description: String = "",
) {
    val context = LocalContext.current
    val reduce = remember(context) { !animationsEnabled(context) }
    var flip by remember { mutableFloatStateOf(0f) }
    var swing by remember { mutableFloatStateOf(REST) }
    var flipTarget by remember { mutableFloatStateOf(0f) }
    var kick by remember { mutableFloatStateOf(0f) }
    var tick by remember { mutableStateOf(0) }

    LaunchedEffect(reduce) {
        var flipV = 0f
        var swingV = 0f
        var last = -1L
        while (true) {
            androidx.compose.runtime.withFrameNanos { now ->
                val dt = if (last < 0) 0f else ((now - last) / 1e9f).coerceIn(0f, 1f / 20f)
                last = now
                if (kick != 0f) { swingV += kick; kick = 0f }
                // Dá voltas inteiras fora para o ângulo ficar pequeno (igual ao original)
                val wrap = Math.round(flipTarget / 360f) * 360f
                if (wrap != 0f) { flipTarget -= wrap; flip -= wrap }
                if (reduce) {
                    flip = flipTarget; flipV = 0f; swing = REST; swingV = 0f
                } else {
                    val n = maxOf(1, Math.ceil((dt / (1f / 240f)).toDouble()).toInt())
                    val h = dt / n
                    repeat(n) {
                        flipV += (-FLIP_K * (flip - flipTarget) - FLIP_C * flipV) * h
                        flip += flipV * h
                        swingV += (-SWING_K * (swing - REST) - SWING_C * swingV) * h
                        swing += swingV * h
                    }
                }
                tick++
            }
        }
    }

    val density = LocalDensity.current.density
    val rad = (flip * PI / 180.0).toFloat()
    val showFront = cos(rad) > 0f
    val fs = sin(rad)
    val shape = CircleShape
    Box(
        modifier
            .size(size)
            .graphicsLayer {
                rotationY = flip
                rotationZ = swing
                cameraDistance = 12f * density
                transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 0f)
            }
            .clip(shape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() }, indication = null, role = Role.Button,
                onClickLabel = "Virar selo"
            ) {
                // Mais um meio giro, sempre no mesmo sentido (toque não tem lado, ao contrário do ponteiro do original)
                flipTarget += 180f
                kick += SWING_KICK
            },
        contentAlignment = Alignment.Center
    ) {
        if (showFront) {
            Box(Modifier.fillMaxSize().background(front, shape), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = description, tint = onFront, modifier = Modifier.size(size * 0.58f))
            }
        } else {
            // Verso: o conteúdo gira 180° para não aparecer espelhado
            val pos = (50f + fs * 30f).coerceIn(0f, 100f) / 100f
            val span = 600f
            val dx = -span * pos
            Box(
                Modifier.fillMaxSize()
                    .graphicsLayer { rotationY = 180f }
                    .background(
                        Brush.linearGradient(
                            0f to HOLO_PINK, 0.18f to HOLO_BLUE, 0.34f to HOLO_MINT, 0.48f to Color.White,
                            0.60f to HOLO_YELLOW, 0.78f to HOLO_LAVENDER, 1f to HOLO_PINK,
                            start = androidx.compose.ui.geometry.Offset(dx, dx), end = androidx.compose.ui.geometry.Offset(dx + span, dx + span),
                            tileMode = androidx.compose.ui.graphics.TileMode.Mirror
                        ), shape
                    ),
                contentAlignment = Alignment.Center
            ) {
                // Brilho diagonal: o mesmo gradiente de 115° do original, que anda no sentido contrário
                val gpos = (50f + fs * 40f).coerceIn(0f, 100f) / 100f
                Box(
                    Modifier.fillMaxSize().background(
                        Brush.linearGradient(
                            0.38f to Color.Transparent, 0.5f to Color.White.copy(alpha = 0.8f * 0.35f), 0.62f to Color.Transparent,
                            start = androidx.compose.ui.geometry.Offset(size.value * density * (1f - gpos) * -1f, 0f),
                            end = androidx.compose.ui.geometry.Offset(size.value * density * (2f - gpos), size.value * density)
                        )
                    )
                )
                Icon(icon, contentDescription = null, tint = INK, modifier = Modifier.size(size * 0.5f))
            }
        }
    }
    @Suppress("UNUSED_EXPRESSION") tick
}

private const val FLIP_K = 60f
private const val FLIP_C = 16f
private const val SWING_K = 48f
private const val SWING_C = 20f / 10f
private const val SWING_KICK = 70f
private const val REST = 1f
private val HOLO_MINT = Color(0xFFCFF1E3)
private val HOLO_BLUE = Color(0xFFC6DDF6)
private val HOLO_PINK = Color(0xFFEFC6E9)
private val HOLO_YELLOW = Color(0xFFF1EEC0)
private val HOLO_LAVENDER = Color(0xFFD8CBF3)
private val INK = Color(0xFF1E1B22)
