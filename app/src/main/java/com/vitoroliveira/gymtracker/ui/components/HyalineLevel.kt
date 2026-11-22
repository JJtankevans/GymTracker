package com.vitoroliveira.gymtracker.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vitoroliveira.gymtracker.ui.theme.animationsEnabled
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * PORTE FIEL (miolo) do `hyaline-level` do OriginKit (cache em Runtime/docs/originkit-cache/hyaline-level.txt):
 * controle em degraus numa trilha quase preta com pontos, preenchimento de vidro claro, "conta" de vidro como cursor,
 * marcas em cada degrau, nome do degrau que rola sobre o cursor enquanto se arrasta e legendas nas pontas em repouso.
 *
 * Mantido do original (medidas em pt do clipe, escaladas pela largura): trilha 160×20 raio 3; cursor 12×16 com folga 2;
 * preenchimento vai até 2 depois da borda direita do cursor; marcas 4×0,7 no centro de cada degrau (primeira a 8 da ponta);
 * pontos de grade passo 2 / lado 0,3; vidro = tinta + faixa de luz no alto (sheen 30) + aro claro no alto (rim 70, 0,35);
 * sombra suave 0,35 sob o cursor; ao soltar, assenta no degrau mais próximo em 0,18 s com easeOutCubic (0,33; 1; 0,68; 1);
 * o nome rola 1,2 alturas de caixa-alta e some/entra por distância à posição da roda; ao soltar o nome some em 70 ms e as
 * legendas voltam. Trocado: React/framer-motion por Compose, cores do tema só no tom do vidro (a trilha é escura em qualquer
 * tema, como no original), borrão do nome (não há `blur` barato) e teclado (o toque usa `setProgress` para o TalkBack).
 */
@Composable
fun HyalineLevel(
    names: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    startCaption: String,
    endCaption: String,
    description: String,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    val context = LocalContext.current
    val reduce = remember(context) { !animationsEnabled(context) }
    val n = names.size
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current.density
    val pos = remember { Animatable(if (selected in 0 until n) selected.toFloat() else 0f) }
    val active = remember { Animatable(0f) }
    var chosen by remember { mutableStateOf(selected in 0 until n) }
    val ease = remember { CubicBezierEasing(0.33f, 1f, 0.68f, 1f) }

    // Segue o que veio de fora (outra série selecionada) quando não há gesto em curso
    LaunchedEffect(selected) {
        if (selected in 0 until n) { chosen = true; pos.snapTo(selected.toFloat()) } else { chosen = false; pos.snapTo(0f) }
    }

    BoxWithConstraints(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        val w = minOf(maxWidth, 300.dp)
        val k = w.value / TRACK_W // dp por pt
        val totalH = (NAME_H + TRACK_H) * k
        val widthPx = w.value * density

        fun centerPt(step: Float): Float {
            val first = INSET + THUMB_W / 2
            val last = TRACK_W - INSET - THUMB_W / 2
            return first + (last - first) * (if (n > 1) step / (n - 1) else 0f)
        }
        fun stepAt(xPx: Float): Float {
            val pt = xPx / (widthPx / TRACK_W)
            val first = centerPt(0f)
            val last = centerPt((n - 1).toFloat())
            return (((pt - first) / (last - first)) * (n - 1)).coerceIn(0f, (n - 1).toFloat())
        }

        Box(
            Modifier
                .widthIn(max = w)
                .height(totalH.dp)
                .semantics {
                    contentDescription = description
                    stateDescription = if (chosen) names[pos.value.roundToInt().coerceIn(0, n - 1)] else "sem resposta"
                    progressBarRangeInfo = ProgressBarRangeInfo(pos.value, 0f..(n - 1).toFloat(), n - 2)
                    setProgress { target ->
                        val s = target.roundToInt().coerceIn(0, n - 1)
                        chosen = true
                        scope.launch { pos.snapTo(s.toFloat()) }
                        onSelect(s)
                        true
                    }
                }
                .pointerInput(n) {
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        chosen = true
                        scope.launch { active.animateTo(1f, tween(if (reduce) 0 else 70)) }
                        scope.launch { pos.snapTo(stepAt(down.position.x)) }
                        drag(down.id) { change ->
                            scope.launch { pos.snapTo(stepAt(change.position.x)) }
                            change.consume()
                        }
                        val to = pos.value.roundToInt().coerceIn(0, n - 1)
                        scope.launch {
                            pos.animateTo(to.toFloat(), tween(if (reduce) 0 else 180, easing = ease))
                        }
                        scope.launch { active.animateTo(0f, tween(if (reduce) 0 else 70)) }
                        onSelect(to)
                    }
                }
        ) {
            val dim = if (chosen) 1f else 0.45f
            Canvas(Modifier.fillMaxWidth().height(totalH.dp)) {
                val s = size.width / TRACK_W // px por pt
                val top = NAME_H * s
                drawTrack(s, top)
                val cx = centerPt(pos.value) * s
                val fillRight = (cx + THUMB_W / 2 * s + INSET * s).coerceAtMost(TRACK_W * s)
                // Marcas na trilha (fracas)
                for (i in 0 until n) tick(centerPt(i.toFloat()) * s, top, s, TICK_DARK)
                // Preenchimento de vidro (com as marcas mais claras por baixo e o recorte da trilha)
                val glassTint = cs.primary.copy(alpha = 0.40f)
                val trackPath = Path().apply {
                    addRoundRect(androidx.compose.ui.geometry.RoundRect(0f, top, fillRight, top + TRACK_H * s, CornerRadius(RADIUS * s)))
                }
                clipPath(trackPath) {
                    drawRect(Color.White.copy(alpha = 0.14f * dim), Offset(0f, top), Size(fillRight, TRACK_H * s))
                    drawRect(glassTint.copy(alpha = glassTint.alpha * dim), Offset(0f, top), Size(fillRight, TRACK_H * s))
                    for (i in 0 until n) tick(centerPt(i.toFloat()) * s, top, s, TICK_LIGHT)
                    glassSheen(0f, top, fillRight, TRACK_H * s, s, dim)
                }
                // Cursor: conta de vidro (esconde a marca sob ela), sombra suave e o dobro de vidro
                val tw = THUMB_W * s
                val th = THUMB_H * s
                val tx = cx - tw / 2
                val ty = top + (TRACK_H - THUMB_H) / 2 * s
                drawRoundRect(Color.Black.copy(alpha = SHADOW * dim), Offset(tx, ty + SHADOW_DROP * s), Size(tw, th), CornerRadius((RADIUS - INSET) * s + SHADOW_BLUR * s))
                drawRoundRect(TRACK, Offset(tx, ty), Size(tw, th), CornerRadius(maxOf(1f, (RADIUS - INSET)) * s))
                val thumbPath = Path().apply { addRoundRect(androidx.compose.ui.geometry.RoundRect(tx, ty, tx + tw, ty + th, CornerRadius(maxOf(1f, (RADIUS - INSET)) * s))) }
                clipPath(thumbPath) {
                    repeat(2) {
                        drawRect(Color.White.copy(alpha = 0.34f * dim), Offset(tx, ty), Size(tw, th))
                        drawRect(glassTint.copy(alpha = glassTint.alpha * dim), Offset(tx, ty), Size(tw, th))
                        glassSheen(tx, ty, tx + tw, th, s, dim)
                    }
                }
            }
            // Legendas das pontas (em repouso) e o nome que rola sobre o cursor (ao arrastar)
            val a = active.value
            val capSp = (8f * k).sp
            Text(
                startCaption, color = MUTED, fontSize = capSp, fontWeight = FontWeight.Medium,
                modifier = Modifier.align(Alignment.TopStart).graphicsLayer {
                    translationX = (INSET + 2f) * k * density
                    translationY = (NAME_H + (TRACK_H - 8f) / 2f - 0.5f) * k * density
                    alpha = 1f - a
                }
            )
            Text(
                endCaption, color = MUTED, fontSize = capSp, fontWeight = FontWeight.Medium,
                modifier = Modifier.align(Alignment.TopEnd).graphicsLayer {
                    translationX = -(INSET + 2f) * k * density
                    translationY = (NAME_H + (TRACK_H - 8f) / 2f - 0.5f) * k * density
                    alpha = 1f - a
                }
            )
            val travel = CAP * 10.5f * k * 1.2f * density
            val p = pos.value
            for (i in 0 until n) {
                val d = abs(p - i)
                if (d >= 1f) continue
                Text(
                    names[i], color = cs.onSurface, fontSize = capSp * 1.1f, fontWeight = FontWeight.Medium, maxLines = 1,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.align(Alignment.TopStart).width(120.dp).graphicsLayer {
                        // A roda: quem sobe vem de baixo, quem desce vem de cima; centrado no cursor (e dentro da trilha)
                        translationX = (centerPt(p) * k * density - 60f * density)
                            .coerceIn(0f, (TRACK_W * k - 120f) * density)
                        translationY = (NAME_H - 8.6f - 12f) * k * density + (i - p) * travel
                        alpha = (1f - d) * active.value
                    }
                )
            }
        }
    }
}

private fun DrawScope.drawTrack(s: Float, top: Float) {
    drawRoundRect(TRACK, Offset(0f, top), Size(TRACK_W * s, TRACK_H * s), CornerRadius(RADIUS * s))
    // Pontos da grade (a mesma rede aparece através do vidro, por isso fica na trilha toda)
    val pitch = maxOf(1f, DOT_PITCH * s)
    val side = maxOf(1f, DOT_SIZE * s)
    val cols = ((TRACK_W * s - side) / pitch).toInt()
    val rows = ((TRACK_H * s - side) / pitch).toInt()
    val ox = (TRACK_W * s - (cols - 1) * pitch - side) / 2f
    val oy = top + (TRACK_H * s - (rows - 1) * pitch - side) / 2f
    for (j in 0 until rows) for (i in 0 until cols) {
        val x = ox + i * pitch
        val y = oy + j * pitch
        // pontos que o canto arredondado corta ficam de fora
        val r = RADIUS * s
        val ex = maxOf(0f, r - minOf(x + side / 2, TRACK_W * s - x - side / 2))
        val ey = maxOf(0f, r - minOf(y - top + side / 2, TRACK_H * s - (y - top) - side / 2))
        if (ex > 0 && ey > 0 && Math.hypot(ex.toDouble(), ey.toDouble()) > r - side) continue
        drawRect(DOTS, Offset(x, y), Size(side, side))
    }
    drawRoundRect(EDGE, Offset(0f, top), Size(TRACK_W * s, TRACK_H * s), CornerRadius(RADIUS * s), style = androidx.compose.ui.graphics.drawscope.Stroke(maxOf(1f, 0.5f * s)))
}

private fun DrawScope.tick(cx: Float, top: Float, s: Float, color: Color) {
    val h = TICK_H * s
    val tw = maxOf(1f, TICK_W * s)
    drawRect(color, Offset(cx - tw / 2, top + (TRACK_H * s - h) / 2), Size(tw, h))
}

private fun DrawScope.glassSheen(x0: Float, top: Float, x1: Float, h: Float, s: Float, dim: Float) {
    // faixa de luz no alto, pé mais escuro e aro claro ao longo do alto
    drawRect(
        Brush.verticalGradient(
            0f to Color.White.copy(alpha = 0.30f * 0.30f * 2f * dim), 0.5f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.18f * dim),
            startY = top, endY = top + h
        ),
        Offset(x0, top), Size(x1 - x0, h)
    )
    drawRect(Color.White.copy(alpha = 0.70f * dim), Offset(x0, top), Size(x1 - x0, maxOf(1f, RIM_W * s)))
}

private const val TRACK_W = 160f
private const val TRACK_H = 20f
private const val RADIUS = 3f
private const val INSET = 2f
private const val THUMB_W = 12f
private const val THUMB_H = 16f
private const val TICK_H = 4f
private const val TICK_W = 0.7f
private const val DOT_PITCH = 2f
private const val DOT_SIZE = 0.3f
private const val RIM_W = 0.35f
private const val SHADOW = 0.35f
private const val SHADOW_DROP = 0.6f
private const val SHADOW_BLUR = 1f
private const val CAP = 0.727f
private const val NAME_H = 22f // espaço acima da trilha para o nome (10,5 + folga de 8,6)
private val TRACK = Color(0xFF121214)
private val EDGE = Color.White.copy(alpha = 0.08f)
private val DOTS = Color.White.copy(alpha = 0.12f)
private val TICK_DARK = Color.White.copy(alpha = 0.24f)
private val TICK_LIGHT = Color.White.copy(alpha = 0.45f)
private val MUTED = Color(0xFF8E8E93)
