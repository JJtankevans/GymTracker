package com.vitoroliveira.gymtracker.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.vitoroliveira.gymtracker.ui.theme.animationsEnabled
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Wake Slider (porte fiel do OriginKit, `Runtime/docs/originkit-cache/wake-slider.txt`): um controle de faixa sem "bolinha".
 * Fileira de barras finas, cheias até o valor e fracas depois. Arrastar devagar só preenche; arrastar rápido faz
 * cada barra que o valor atravessa saltar (mais alto quanto mais rápido) e voltar: uma esteira atrás da mão.
 *
 * Miolo igual ao original: barra 3, vão 4, altura 20 (dp), `RISE` 1,6, decaimento `exp(-dt·6,5)` por quadro e
 * força `(1 - exp(-velocidade/900)) · (esteira/60)`. Cada barra guarda energia; o laço de quadros só roda enquanto
 * alguma barra ainda tem energia. Com "remover animações" ligado a esteira é desligada (só preenche).
 */
@Composable
fun WakeSlider(
    value: Int,
    onValueChange: (Int) -> Unit,
    range: IntRange,
    contentDescription: String,
    modifier: Modifier = Modifier,
    wake: Float = 60f
) {
    val cs = MaterialTheme.colorScheme
    val context = LocalContext.current
    val reduce = remember(context) { !animationsEnabled(context) }
    val density = LocalDensity.current.density
    var widthPx by remember { mutableIntStateOf(0) }
    val pitchPx = (BAR + GAP) * density
    val n = ((widthPx + GAP * density) / pitchPx).toInt().coerceIn(12, 64)
    val energy = remember(n) { FloatArray(n) }
    var tick by remember { mutableLongStateOf(0L) }
    var alive by remember { mutableStateOf(false) }

    fun fracOf(v: Int) = (v - range.first).toFloat() / (range.last - range.first).coerceAtLeast(1)
    var lastFrac by remember { mutableStateOf(fracOf(value)) }

    // laço de quadros: roda só enquanto há energia nas barras
    LaunchedEffect(alive) {
        if (!alive) return@LaunchedEffect
        var prev = androidx.compose.runtime.withFrameNanos { it }
        while (true) {
            val now = androidx.compose.runtime.withFrameNanos { it }
            val dt = min(0.05f, (now - prev) / 1_000_000_000f)
            prev = now
            val fall = exp(-dt * 6.5f)
            var any = false
            for (i in energy.indices) {
                energy[i] *= fall
                if (energy[i] < 0.002f) energy[i] = 0f else any = true
            }
            tick++
            if (!any) { alive = false; break }
        }
    }

    fun moveTo(xPx: Float, speedDpPerS: Float) {
        val f = (xPx / widthPx.coerceAtLeast(1)).coerceIn(0f, 1f)
        val next = (range.first + f * (range.last - range.first)).roundToInt().coerceIn(range.first, range.last)
        val fNext = fracOf(next)
        if (!reduce) {
            val a = (lastFrac * (n - 1)).roundToInt()
            val b = (fNext * (n - 1)).roundToInt()
            val push = (1f - exp(-speedDpPerS / 900f)) * (wake / 60f)
            for (i in min(a, b)..max(a, b)) if (i in energy.indices) energy[i] = max(energy[i], push)
            alive = true
        }
        lastFrac = fNext
        if (next != value) onValueChange(next)
        tick++
    }

    Canvas(
        modifier
            .fillMaxWidth()
            .height((H * (1 + RISE)).dp)
            .onSizeChanged { widthPx = it.width }
            .pointerInput(n, range) { detectTapGestures { moveTo(it.x, 0f) } }
            .pointerInput(n, range) {
                var lastX = 0f
                var lastT = 0L
                detectDragGestures(
                    onDragStart = { off -> lastX = off.x; lastT = System.nanoTime(); moveTo(off.x, 0f) },
                    onDrag = { change, _ ->
                        val now = System.nanoTime()
                        val dt = (now - lastT) / 1_000_000_000f
                        val speed = if (dt > 0f) abs(change.position.x - lastX) / density / dt else 0f
                        lastX = change.position.x; lastT = now
                        moveTo(change.position.x, speed)
                        change.consume()
                    }
                )
            }
            .semantics {
                this.contentDescription = contentDescription
                stateDescription = "$value"
                progressBarRangeInfo = ProgressBarRangeInfo(value.toFloat(), range.first.toFloat()..range.last.toFloat())
                setProgress { v -> onValueChange(v.roundToInt().coerceIn(range.first, range.last)); true }
            }
    ) {
        tick // lê o estado para redesenhar a cada quadro da esteira
        val barW = BAR * density
        val pitch = (BAR + GAP) * density
        val total = n * pitch - GAP * density
        val startX = (size.width - total) / 2f
        val frac = fracOf(value)
        val on = cs.primary
        val off = cs.onSurface.copy(alpha = 0.16f)
        for (i in 0 until n) {
            val x = startX + i * pitch
            val h = H * density * (1f + energy[i] * RISE)
            val y = (size.height - h) / 2f
            val filled = (i.toFloat() / (n - 1)) <= frac + 1e-4f
            drawRoundRect(if (filled) on else off, Offset(x, y), Size(barW, h), CornerRadius(barW / 2f))
        }
    }
}

// do original
private const val H = 20f      // altura da barra em repouso
private const val GAP = 4f     // passo menos a largura da barra
private const val BAR = 3f
private const val RISE = 1.6f  // altura extra com energia cheia (até 2,6×)
