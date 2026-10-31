package com.vitoroliveira.gymtracker.ui.theme

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/*
 * Terceira leva dos temas "Loucos" (28/10/2026, madrugada). Mesmo contrato de ThemeDecor.kt:
 * [t] vai de 0 a 1 em 60 s e toda animação usa múltiplos inteiros de t (o loop fecha sem salto).
 */

private const val TAU = (2 * PI).toFloat()

private fun frac(x: Float) = x - kotlin.math.floor(x)

// ───────────────────────── Placa-mãe ─────────────────────────
internal fun DrawScope.drawCircuit(colors: List<Color>, t: Float) {
    val trace = colors.getOrElse(0) { Color(0xFF00E08A) }
    val pulse = colors.getOrElse(1) { Color(0xFFB6FF3B) }
    val w = size.width; val h = size.height
    val cell = 28.dp.toPx()
    val rnd = Random(12)
    // trilhas: segmentos em L partindo de pontos da grade
    repeat(26) { i ->
        val x0 = (rnd.nextInt((w / cell).toInt().coerceAtLeast(1)) + 0.5f) * cell
        val y0 = (rnd.nextInt((h / cell).toInt().coerceAtLeast(1)) + 0.5f) * cell
        val dx = (rnd.nextInt(7) - 3) * cell
        val dy = (rnd.nextInt(9) - 4) * cell
        val mid = Offset(x0 + dx, y0)
        val end = Offset(x0 + dx, y0 + dy)
        drawLine(trace.copy(alpha = 0.22f), Offset(x0, y0), mid, 2.dp.toPx(), cap = StrokeCap.Round)
        drawLine(trace.copy(alpha = 0.22f), mid, end, 2.dp.toPx(), cap = StrokeCap.Round)
        drawCircle(trace.copy(alpha = 0.45f), 3.dp.toPx(), Offset(x0, y0), style = Stroke(1.5f.dp.toPx()))
        drawCircle(trace.copy(alpha = 0.45f), 3.dp.toPx(), end, style = Stroke(1.5f.dp.toPx()))
        // pulso de energia correndo pela trilha
        val len = abs(dx) + abs(dy)
        if (len > 0f) {
            val p = frac(t * (6 + i % 5) + i * 0.17f) * len
            val pos = if (p <= abs(dx)) Offset(x0 + p * (if (dx >= 0) 1 else -1), y0)
            else Offset(x0 + dx, y0 + (p - abs(dx)) * (if (dy >= 0) 1 else -1))
            drawCircle(pulse.copy(alpha = 0.25f), 7.dp.toPx(), pos)
            drawCircle(pulse.copy(alpha = 0.9f), 2.dp.toPx(), pos)
        }
    }
    // "chips" retangulares
    val chips = Random(3)
    repeat(4) {
        val cw = (3 + chips.nextInt(3)) * cell * 0.6f
        val ch = cw * 0.6f
        val o = Offset(chips.nextFloat() * (w - cw), chips.nextFloat() * (h - ch))
        drawRect(Color.Black.copy(alpha = 0.35f), o, Size(cw, ch))
        drawRect(trace.copy(alpha = 0.35f), o, Size(cw, ch), style = Stroke(1.5f.dp.toPx()))
        var px = o.x + 6.dp.toPx()
        while (px < o.x + cw - 4.dp.toPx()) {
            drawLine(trace.copy(alpha = 0.35f), Offset(px, o.y), Offset(px, o.y - 5.dp.toPx()), 1.5f)
            drawLine(trace.copy(alpha = 0.35f), Offset(px, o.y + ch), Offset(px, o.y + ch + 5.dp.toPx()), 1.5f)
            px += 7.dp.toPx()
        }
    }
}

// ───────────────────────── Nevasca ─────────────────────────
internal fun DrawScope.drawSnow(colors: List<Color>, t: Float) {
    val glow = colors.getOrElse(0) { Color(0xFFBDE7FF) }
    val w = size.width; val h = size.height
    drawRect(Brush.verticalGradient(listOf(glow.copy(alpha = 0.10f), Color.Transparent), endY = h * 0.5f))
    val rnd = Random(91)
    // flocos em três profundidades: os da frente são maiores, mais rápidos e mais fortes
    repeat(110) { i ->
        val depth = i % 3
        val speed = 2 + depth * 2 + rnd.nextInt(2)
        val phase = rnd.nextFloat()
        val p = frac(t * speed + phase)
        val x = frac(rnd.nextFloat() + 0.05f * sin(TAU * (p * 2f + phase)) + t * 2f) * w
        val y = p * (h + 20f) - 10f
        val r = (1f + depth * 1.2f).dp.toPx()
        drawCircle(Color.White.copy(alpha = 0.35f + depth * 0.2f), r, Offset(x, y))
    }
    // floquinhos de seis pontas girando
    repeat(7) { i ->
        val phase = rnd.nextFloat()
        val p = frac(t * (2 + i % 2) + phase)
        val c = Offset(w * (0.1f + 0.8f * rnd.nextFloat()), p * (h + 60f) - 30f)
        val r = (7 + i % 3 * 3).dp.toPx()
        rotate(t * 360f * (4 + i), c) {
            for (k in 0 until 6) {
                val a = TAU * k / 6
                val e = c + Offset(cos(a), sin(a)) * r
                drawLine(Color.White.copy(alpha = 0.65f), c, e, 1.4f, cap = StrokeCap.Round)
                val m = c + Offset(cos(a), sin(a)) * r * 0.6f
                drawLine(Color.White.copy(alpha = 0.5f), m, m + Offset(cos(a + 0.6f), sin(a + 0.6f)) * r * 0.3f, 1.2f)
                drawLine(Color.White.copy(alpha = 0.5f), m, m + Offset(cos(a - 0.6f), sin(a - 0.6f)) * r * 0.3f, 1.2f)
            }
        }
    }
    // neve acumulada no pé da tela
    val drift = Path().apply {
        moveTo(0f, h)
        var x = 0f
        while (x <= w + 10f) { lineTo(x, h - 14.dp.toPx() - 8.dp.toPx() * sin(TAU * (x / w * 1.5f))); x += 10f }
        lineTo(w, h); close()
    }
    drawPath(drift, Color.White.copy(alpha = 0.18f))
}

// ───────────────────────── Bolhas de sabão ─────────────────────────
internal fun DrawScope.drawBubbles(colors: List<Color>, t: Float) {
    val palette = colors.ifEmpty { listOf(Color(0xFFFF9AE6), Color(0xFF9AE6FF), Color(0xFFFFF09A)) }
    val w = size.width; val h = size.height
    val rnd = Random(55)
    repeat(16) { i ->
        val speed = 1 + rnd.nextInt(3)
        val phase = rnd.nextFloat()
        val p = frac(t * speed + phase)
        val r = (14 + rnd.nextInt(34)).dp.toPx()
        val x = w * rnd.nextFloat() + 24.dp.toPx() * sin(TAU * (p * 2f + phase))
        val y = h + r - p * (h + 2 * r)
        val c = Offset(x, y)
        // película iridescente: anel com gradiente que gira
        rotate(t * 360f * (2 + i % 3), c) {
            drawCircle(
                Brush.sweepGradient(palette + palette.first(), center = c),
                radius = r, center = c, alpha = 0.45f, style = Stroke(2.2f.dp.toPx())
            )
        }
        drawCircle(Brush.radialGradient(listOf(Color.Transparent, palette[i % palette.size].copy(alpha = 0.10f)), c, r), r, c)
        // reflexo da luz
        drawCircle(Color.White.copy(alpha = 0.55f), r * 0.16f, c + Offset(-r * 0.4f, -r * 0.4f))
    }
}

// ───────────────────────── Miragem (deserto) ─────────────────────────
internal fun DrawScope.drawDesert(colors: List<Color>, t: Float) {
    val sun = colors.getOrElse(0) { Color(0xFFFFB347) }
    val sand = colors.getOrElse(1) { Color(0xFFD9824A) }
    val w = size.width; val h = size.height
    val sc = Offset(w * 0.72f, h * 0.2f)
    drawCircle(Brush.radialGradient(listOf(sun.copy(alpha = 0.55f), Color.Transparent), sc, w * 0.55f), w * 0.55f, sc)
    drawCircle(sun.copy(alpha = 0.85f), 40.dp.toPx(), sc)
    // calor tremendo: linhas onduladas que sobem
    for (k in 0 until 6) {
        val y = h * (0.42f + k * 0.04f) - frac(t * 10f + k / 6f) * h * 0.05f
        val path = Path().apply {
            moveTo(0f, y)
            var x = 0f
            while (x <= w) { lineTo(x, y + 3.dp.toPx() * sin(TAU * (x / (w * 0.25f) + t * 30f + k))); x += 8f }
        }
        drawPath(path, Color.White.copy(alpha = 0.06f), style = Stroke(1.5f.dp.toPx()))
    }
    // dunas em camadas (as de trás mais claras)
    for (layer in 0 until 3) {
        val base = h * (0.62f + layer * 0.12f)
        val amp = (26 - layer * 6).dp.toPx()
        val shift = t * (1 + layer)
        val path = Path().apply {
            moveTo(0f, h)
            var x = 0f
            lineTo(0f, base)
            while (x <= w + 8f) { lineTo(x, base - amp * sin(TAU * (x / (w * (1.3f - layer * 0.2f)) + shift))); x += 8f }
            lineTo(w, h); close()
        }
        drawPath(path, sand.copy(alpha = 0.18f + layer * 0.12f))
    }
}

// ───────────────────────── Origami ─────────────────────────
internal fun DrawScope.drawOrigami(colors: List<Color>, t: Float) {
    val palette = colors.ifEmpty { listOf(Color(0xFFE8604C), Color(0xFF2F8F83), Color(0xFFF2B33D), Color(0xFF3B5BA9)) }
    val w = size.width; val h = size.height
    val rnd = Random(29)
    // pássaros/aviões de papel: dois triângulos (asa clara e escura) planando
    repeat(9) { i ->
        val speed = 1 + rnd.nextInt(3)
        val phase = rnd.nextFloat()
        val p = frac(t * speed + phase)
        val x = p * (w + 120f) - 60f
        val y = h * (0.1f + 0.75f * rnd.nextFloat()) + 20.dp.toPx() * sin(TAU * (p * 2f + phase))
        val s = (12 + rnd.nextInt(14)).dp.toPx()
        val flap = 0.5f + 0.5f * sin(TAU * (t * 40f + i * 0.3f))
        val col = palette[i % palette.size]
        val nose = Offset(x + s, y)
        val tail = Offset(x - s, y)
        val wingUp = Offset(x - s * 0.3f, y - s * (0.4f + 0.5f * flap))
        val wingDown = Offset(x - s * 0.2f, y + s * 0.35f)
        drawPath(Path().apply { moveTo(nose.x, nose.y); lineTo(tail.x, tail.y); lineTo(wingUp.x, wingUp.y); close() }, col.copy(alpha = 0.55f))
        drawPath(Path().apply { moveTo(nose.x, nose.y); lineTo(tail.x, tail.y); lineTo(wingDown.x, wingDown.y); close() }, col.copy(alpha = 0.85f))
    }
    // dobras do papel no fundo: diagonais suaves
    val fold = Random(4)
    repeat(6) {
        val a = Offset(fold.nextFloat() * w, 0f)
        val b = Offset(fold.nextFloat() * w, h)
        drawLine(Color.Black.copy(alpha = 0.035f), a, b, 1.2f)
    }
}

// ───────────────────────── Cidade neon ─────────────────────────
internal fun DrawScope.drawNeonCity(colors: List<Color>, t: Float) {
    val palette = colors.ifEmpty { listOf(Color(0xFFFF2E88), Color(0xFF2EE6FF), Color(0xFFFFD12E)) }
    val w = size.width; val h = size.height
    // céu com brilho no horizonte
    drawRect(Brush.verticalGradient(listOf(Color.Transparent, palette[0].copy(alpha = 0.18f)), startY = h * 0.3f, endY = h * 0.75f))
    val rnd = Random(77)
    // prédios: duas fileiras (a de trás mais escura e alta)
    for (row in 0 until 2) {
        var x = -10f
        while (x < w) {
            val bw = (28 + rnd.nextInt(40)).dp.toPx()
            val bh = h * (0.18f + 0.3f * rnd.nextFloat()) * (if (row == 0) 1.25f else 1f)
            val top = h - bh
            drawRect(Color.Black.copy(alpha = if (row == 0) 0.55f else 0.8f), Offset(x, top), Size(bw, bh))
            // janelas acendendo e apagando
            val win = 5.dp.toPx(); val gap = 7.dp.toPx()
            var wy = top + gap
            var k = 0
            while (wy < h - gap) {
                var wx = x + gap
                while (wx < x + bw - gap) {
                    val on = sin(TAU * (t * (2 + (k % 5)) + k * 0.37f + row)) > 0.55f
                    if (on) drawRect(palette[(k + row) % palette.size].copy(alpha = if (row == 0) 0.25f else 0.55f), Offset(wx, wy), Size(win, win))
                    wx += win + gap; k++
                }
                wy += win + gap
            }
            // letreiro neon no topo de alguns prédios
            if (row == 1 && rnd.nextFloat() < 0.35f) {
                val col = palette[rnd.nextInt(palette.size)]
                val blink = if (sin(TAU * (t * 30f + x)) > -0.7f) 1f else 0.2f
                drawRect(col.copy(alpha = 0.8f * blink), Offset(x + bw * 0.2f, top - 6.dp.toPx()), Size(bw * 0.6f, 3.dp.toPx()))
                drawRect(col.copy(alpha = 0.2f * blink), Offset(x + bw * 0.1f, top - 12.dp.toPx()), Size(bw * 0.8f, 14.dp.toPx()))
            }
            x += bw + (2 + rnd.nextInt(8)).dp.toPx()
        }
    }
    // carros voadores: rastros de luz cruzando o céu
    repeat(5) { i ->
        val p = frac(t * (5 + i * 2) + i * 0.21f)
        val y = h * (0.15f + 0.08f * i)
        val dir = if (i % 2 == 0) 1f else -1f
        val x = if (dir > 0) p * (w + 80f) - 40f else w + 40f - p * (w + 80f)
        val col = palette[i % palette.size]
        drawLine(col.copy(alpha = 0.6f), Offset(x, y), Offset(x - dir * 40.dp.toPx(), y), 2.dp.toPx(), cap = StrokeCap.Round)
        drawCircle(Color.White.copy(alpha = 0.9f), 2.dp.toPx(), Offset(x, y))
    }
}
