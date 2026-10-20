package com.vitoroliveira.gymtracker.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.vitoroliveira.gymtracker.ui.utils.formatKg
import kotlin.math.abs
import kotlin.math.roundToInt

/** Linha horizontal de referência (ex.: "Intermediário") desenhada atrás da série. */
data class ChartReference(val label: String, val value: Double, val color: Color? = null)

/**
 * Gráfico de linha com área em gradiente, animação de entrada e seleção por
 * toque/arraste. Usa só cores do tema, então combina com qualquer um deles.
 */
@Composable
fun LineChart(
    values: List<Double>,
    xLabels: List<String>,
    modifier: Modifier = Modifier,
    valueText: (Double) -> String = { "${formatKg(it)} kg" },
    lineColor: Color = MaterialTheme.colorScheme.primary,
    accentColor: Color = MaterialTheme.colorScheme.tertiary,
    references: List<ChartReference> = emptyList(),
    /** Faixa sombreada (ex.: peso "normal" pelo IMC). */
    band: ClosedFloatingPointRange<Double>? = null,
    bandLabel: String? = null
) {
    if (values.isEmpty()) return
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val textColor = MaterialTheme.colorScheme.onSurface
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)
    val labelStyle = MaterialTheme.typography.labelSmall.copy(color = labelColor)
    val tipStyle = MaterialTheme.typography.labelLarge.copy(color = textColor)

    var selected by remember(values) { mutableIntStateOf(values.lastIndex) }
    var widthPx by remember { mutableFloatStateOf(0f) }
    val reveal = remember(values) { Animatable(0f) }
    LaunchedEffect(values) { reveal.animateTo(1f, tween(900, easing = FastOutSlowInEasing)) }

    val leftPx = with(density) { 44.dp.toPx() }
    val rightPx = with(density) { 14.dp.toPx() }

    fun xAt(i: Int, w: Float): Float {
        val inner = w - leftPx - rightPx
        return if (values.size == 1) leftPx + inner / 2 else leftPx + inner * i / (values.size - 1)
    }

    Canvas(
        modifier = modifier
            .clipToBounds()
            .onSizeChanged { widthPx = it.width.toFloat() }
            .pointerInput(values) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    fun pick(x: Float) {
                        selected = values.indices.minByOrNull { abs(xAt(it, size.width.toFloat()) - x) } ?: selected
                    }
                    pick(down.position.x)
                    drag(down.id) { pick(it.position.x); it.consume() }
                }
            }
    ) {
        val topPad = 30.dp.toPx()
        val bottomPad = 24.dp.toPx()
        val chartTop = topPad
        val chartBottom = size.height - bottomPad
        val chartH = chartBottom - chartTop

        val extra = references.map { it.value } + listOfNotNull(band?.start, band?.endInclusive)
        var lo = (values + extra).min()
        var hi = (values + extra).max()
        if (hi - lo < 1e-6) { lo -= 1.0; hi += 1.0 }
        val pad = (hi - lo) * 0.15
        lo -= pad; hi += pad
        fun yOf(v: Double) = chartBottom - ((v - lo) / (hi - lo)).toFloat() * chartH

        // faixa de referência (fica atrás de tudo)
        if (band != null) {
            val yTop = yOf(band.endInclusive); val yBot = yOf(band.start)
            drawRect(accentColor.copy(alpha = 0.12f), Offset(leftPx, yTop), Size(size.width - leftPx - rightPx, yBot - yTop))
            if (bandLabel != null) {
                val bm = measurer.measure(bandLabel, labelStyle)
                drawText(bm, topLeft = Offset(size.width - rightPx - bm.size.width - 4.dp.toPx(), yTop + 2.dp.toPx()))
            }
        }
        references.forEach { r ->
            val y = yOf(r.value)
            val c = r.color ?: accentColor
            drawLine(c.copy(alpha = 0.8f), Offset(leftPx, y), Offset(size.width - rightPx, y), 1.5.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 8f)))
            val rm = measurer.measure("${r.label} ${formatKg(r.value)}", labelStyle.copy(color = c))
            drawText(rm, topLeft = Offset(size.width - rightPx - rm.size.width - 4.dp.toPx(), y - rm.size.height - 1.dp.toPx()))
        }

        // grade + rótulos do eixo Y
        for (t in 0..3) {
            val v = lo + (hi - lo) * t / 3
            val y = yOf(v)
            drawLine(gridColor, Offset(leftPx, y), Offset(size.width - rightPx, y), 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f)))
            val txt = formatKg((v * 2).roundToInt() / 2.0)
            val m = measurer.measure(txt, labelStyle)
            drawText(m, topLeft = Offset(leftPx - m.size.width - 8.dp.toPx(), y - m.size.height / 2f))
        }

        // rótulos do eixo X (no máximo ~5, sem se sobrepor)
        val step = (values.size / 5).coerceAtLeast(1)
        for (i in values.indices step step) {
            val m = measurer.measure(xLabels.getOrElse(i) { "" }, labelStyle)
            drawText(m, topLeft = Offset(xAt(i, size.width) - m.size.width / 2f, chartBottom + 6.dp.toPx()))
        }

        val pts = values.mapIndexed { i, v -> Offset(xAt(i, size.width), yOf(v)) }

        val line = Path().apply {
            moveTo(pts[0].x, pts[0].y)
            for (i in 1 until pts.size) {
                val xm = (pts[i - 1].x + pts[i].x) / 2
                cubicTo(xm, pts[i - 1].y, xm, pts[i].y, pts[i].x, pts[i].y)
            }
        }
        val area = Path().apply {
            addPath(line)
            lineTo(pts.last().x, chartBottom)
            lineTo(pts.first().x, chartBottom)
            close()
        }

        val revealX = leftPx + (size.width - leftPx) * reveal.value
        clipRect(left = 0f, top = 0f, right = revealX, bottom = size.height) {
            drawPath(area, Brush.verticalGradient(
                listOf(lineColor.copy(alpha = 0.38f), lineColor.copy(alpha = 0f)), startY = chartTop, endY = chartBottom))
            if (pts.size > 1) {
                drawPath(line, lineColor, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
            val maxIdx = values.indices.maxByOrNull { values[it] }!!
            pts.forEachIndexed { i, p ->
                val isMax = i == maxIdx
                drawCircle(if (isMax) accentColor else lineColor, radius = if (isMax) 5.dp.toPx() else 3.5.dp.toPx(), center = p)
            }
        }

        // seleção: guia vertical, anel e tooltip
        val s = selected.coerceIn(0, pts.lastIndex)
        val sp = pts[s]
        drawLine(lineColor.copy(alpha = 0.45f), Offset(sp.x, chartTop), Offset(sp.x, chartBottom), 1.5.dp.toPx())
        drawCircle(lineColor, radius = 9.dp.toPx(), center = sp, style = Stroke(2.dp.toPx()))
        val tip = "${valueText(values[s])}  ·  ${xLabels.getOrElse(s) { "" }}"
        val tm: androidx.compose.ui.text.TextLayoutResult = measurer.measure(tip, tipStyle)
        val tx = (sp.x - tm.size.width / 2f).coerceIn(leftPx - 8.dp.toPx(), size.width - tm.size.width - 4.dp.toPx())
        drawText(tm, topLeft = Offset(tx, 2.dp.toPx()))
    }
}

/**
 * Barras (ex.: treinos por semana), com linha tracejada opcional de meta.
 * A última barra é a "atual" e usa a cor de destaque.
 */
@Composable
fun BarChart(
    values: List<Int>,
    xLabels: List<String>,
    modifier: Modifier = Modifier,
    goal: Int? = null,
    barColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
    currentColor: Color = MaterialTheme.colorScheme.primary,
    goalColor: Color = MaterialTheme.colorScheme.tertiary
) {
    if (values.isEmpty()) return
    val measurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
    val valueStyle = MaterialTheme.typography.labelMedium.copy(color = MaterialTheme.colorScheme.onSurface)
    val reveal = remember(values) { Animatable(0f) }
    LaunchedEffect(values) { reveal.animateTo(1f, tween(700, easing = FastOutSlowInEasing)) }

    Canvas(modifier = modifier.clipToBounds().fillMaxSize()) {
        val bottomPad = 22.dp.toPx()
        val topPad = 18.dp.toPx()
        val chartBottom = size.height - bottomPad
        val chartH = chartBottom - topPad
        val maxV = maxOf(values.max(), goal ?: 0, 1).toFloat()

        val gap = 8.dp.toPx()
        val barW = (size.width - gap * (values.size - 1)) / values.size

        goal?.let {
            val y = chartBottom - it / maxV * chartH
            drawLine(goalColor, Offset(0f, y), Offset(size.width, y), 1.5.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f)))
        }

        values.forEachIndexed { i, v ->
            val h = v / maxV * chartH * reveal.value
            val x = i * (barW + gap)
            val isCurrent = i == values.lastIndex
            val color = if (isCurrent) currentColor else barColor
            if (v == 0) {
                drawRoundRect(color.copy(alpha = 0.25f), Offset(x, chartBottom - 3.dp.toPx()), Size(barW, 3.dp.toPx()), CornerRadius(2.dp.toPx()))
            } else {
                drawRoundRect(color, Offset(x, chartBottom - h), Size(barW, h), CornerRadius(6.dp.toPx()))
                val t = measurer.measure(v.toString(), valueStyle)
                drawText(t, topLeft = Offset(x + barW / 2 - t.size.width / 2f, chartBottom - h - t.size.height - 2.dp.toPx()))
            }
            if (i % 2 == values.lastIndex % 2) {
                val lm = measurer.measure(xLabels.getOrElse(i) { "" }, labelStyle)
                drawText(lm, topLeft = Offset(x + barW / 2 - lm.size.width / 2f, chartBottom + 5.dp.toPx()))
            }
        }
    }
}
