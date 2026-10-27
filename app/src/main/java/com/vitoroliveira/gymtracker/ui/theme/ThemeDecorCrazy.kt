package com.vitoroliveira.gymtracker.ui.theme

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/*
 * Decorações dos temas "Loucos" e das melhorias de Oceano, Floresta e Ouro.
 * Mesmo contrato de ThemeDecor.kt: [t] vai de 0 a 1 em 60 s e toda animação usa múltiplos inteiros de t.
 */

private const val TAU = (2 * PI).toFloat()

// ───────────────────────── Matrix ─────────────────────────
private const val GLYPHS = "ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀﾁﾂﾃﾄﾅﾆﾇﾈﾉﾊﾋﾌﾍﾎﾏﾐﾑﾒﾓﾔﾕﾖﾗﾘﾙﾚﾛﾜ0123456789Z:+=*<>"
private val glyphPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = Typeface.MONOSPACE }

internal fun DrawScope.drawMatrixRain(colors: List<Color>, t: Float) {
    val head = colors.getOrElse(0) { Color(0xFFD5FFE0) }
    val body = colors.getOrElse(1) { Color(0xFF00FF41) }
    val cell = 18.dp.toPx()
    glyphPaint.textSize = cell * 0.85f
    val cols = (size.width / cell).toInt() + 1
    val rows = (size.height / cell).toInt() + 1
    val rnd = Random(3)
    val tick = (t * 600f).toInt()                    // 10 trocas de letra por segundo
    drawIntoCanvas { canvas ->
        val nc = canvas.nativeCanvas
        for (c in 0 until cols) {
            val speed = 5 + rnd.nextInt(7)           // passadas por minuto
            val phase = rnd.nextFloat()
            val len = 6 + rnd.nextInt(16)
            if (rnd.nextFloat() < 0.25f) continue    // colunas vazias deixam respirar
            val p = (t * speed + phase) % 1f
            val headRow = (p * (rows + len)).toInt()
            for (k in 0 until len) {
                val r = headRow - k
                if (r < 0 || r >= rows) continue
                val flicker = if (k % 4 == 0) tick else 0
                val idx = abs(c * 31 + r * 17 + flicker) % GLYPHS.length
                val alpha = if (k == 0) 0.85f else 0.5f * (1f - k.toFloat() / len)
                glyphPaint.color = (if (k == 0) head else body).copy(alpha = alpha).toArgb()
                nc.drawText(GLYPHS, idx, idx + 1, c * cell, (r + 1) * cell, glyphPaint)
            }
        }
    }
    drawRect(Brush.radialGradient(0.5f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.5f), center = center, radius = size.maxDimension * 0.75f))
}

// ───────────────────────── Hipnose ─────────────────────────
internal fun DrawScope.drawSpiral(colors: List<Color>, t: Float) {
    val a = colors.getOrElse(0) { Color.Magenta }
    val b = colors.getOrElse(1) { Color.Cyan }
    val c = Offset(size.width / 2f, size.height * 0.42f)
    val maxR = size.maxDimension * 0.85f
    val turns = 7f
    val stroke = 22.dp.toPx()
    // 2 braços intercalados girando (1 volta a cada 10 s)
    rotate(t * 360f * 6f, c) {
        for (arm in 0 until 2) {
            val path = Path()
            val steps = 260
            for (i in 0..steps) {
                val f = i / steps.toFloat()
                val th = f * turns * TAU + arm * PI.toFloat()
                val r = f * maxR
                val p = Offset(c.x + r * cos(th), c.y + r * sin(th))
                if (i == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
            }
            drawPath(path, (if (arm == 0) a else b).copy(alpha = 0.30f), style = Stroke(stroke, cap = StrokeCap.Round))
        }
    }
    // anéis pulsando para fora
    val rings = 5
    for (k in 0 until rings) {
        val p = ((t * 12f) + k / rings.toFloat()) % 1f
        drawCircle(Color.White.copy(alpha = 0.10f * (1f - p)), radius = p * maxR, center = c, style = Stroke(2.dp.toPx()))
    }
    drawCircle(Brush.radialGradient(listOf(Color.White.copy(alpha = 0.35f), Color.Transparent), center = c, radius = 60.dp.toPx()), 60.dp.toPx(), c)
    drawRect(Color.Black.copy(alpha = 0.42f))
}

// ───────────────────────── Hiperespaço ─────────────────────────
internal fun DrawScope.drawWarp(colors: List<Color>, t: Float) {
    val tint = colors.getOrElse(0) { Color(0xFF9BE7FF) }
    val c = Offset(size.width / 2f, size.height * 0.45f)
    val maxR = sqrt(size.width * size.width + size.height * size.height) * 0.6f
    drawCircle(Brush.radialGradient(listOf(tint.copy(alpha = 0.25f), Color.Transparent), center = c, radius = maxR * 0.5f), maxR * 0.5f, c)
    val rnd = Random(11)
    repeat(110) { i ->
        val ang = rnd.nextFloat() * TAU
        val speed = 8 + rnd.nextInt(10)
        val phase = rnd.nextFloat()
        val p = (t * speed + phase) % 1f
        val e = p * p * p
        val d = e * maxR
        val len = 4f + e * maxR * 0.22f
        val dir = Offset(cos(ang), sin(ang))
        val col = if (i % 5 == 0) tint else Color.White
        drawLine(
            col.copy(alpha = (0.15f + 0.75f * p).coerceAtMost(0.9f)),
            c + dir * max(d - len, 0f), c + dir * d,
            strokeWidth = 0.8f + 2.2f * p, cap = StrokeCap.Round
        )
    }
}

// ───────────────────────── Glitch ─────────────────────────
internal fun DrawScope.drawGlitch(colors: List<Color>, t: Float) {
    val c1 = colors.getOrElse(0) { Color(0xFF00F0FF) }
    val c2 = colors.getOrElse(1) { Color(0xFFFF2BD6) }
    val w = size.width; val h = size.height
    // linhas de varredura fixas
    val gap = 4.dp.toPx()
    var y = 0f
    while (y < h) { drawLine(Color.White.copy(alpha = 0.03f), Offset(0f, y), Offset(w, y), 1f); y += gap }
    // a cada 1/8 s sorteia se dá "pane": só às vezes, e com pouca opacidade (sem piscar forte)
    val frame = (t * 480f).toInt()
    val rnd = Random(frame)
    if (rnd.nextFloat() < 0.32f) {
        repeat(1 + rnd.nextInt(4)) {
            val by = rnd.nextFloat() * h
            val bh = (4 + rnd.nextInt(36)).dp.toPx()
            val shift = (rnd.nextFloat() - 0.5f) * 60.dp.toPx()
            drawRect(c1.copy(alpha = 0.16f), topLeft = Offset(shift, by), size = Size(w, bh))
            drawRect(c2.copy(alpha = 0.16f), topLeft = Offset(-shift * 0.6f, by + bh * 0.3f), size = Size(w, bh * 0.6f))
        }
    }
    // blocos de "pixel morto" que mudam a cada segundo
    val slow = Random((t * 60f).toInt())
    repeat(14) {
        val s = (6 + slow.nextInt(18)).dp.toPx()
        drawRect(
            (if (it % 2 == 0) c1 else c2).copy(alpha = 0.10f),
            topLeft = Offset(slow.nextFloat() * w, slow.nextFloat() * h), size = Size(s * (1 + slow.nextInt(4)), s * 0.4f)
        )
    }
}

// ───────────────────────── Lâmpada de lava ─────────────────────────
internal fun DrawScope.drawLava(colors: List<Color>, t: Float) {
    val palette = colors.ifEmpty { listOf(Color(0xFFFF5E3A), Color(0xFFFF2E93)) }
    val w = size.width; val h = size.height
    val rnd = Random(17)
    repeat(7) { i ->
        val col = palette[i % palette.size]
        val speed = 1 + rnd.nextInt(3)
        val phase = rnd.nextFloat()
        val cx = w * (0.15f + 0.7f * rnd.nextFloat()) + w * 0.06f * sin(TAU * (t * 2f + phase))
        val cy = h * (0.5f + 0.48f * sin(TAU * (t * speed + phase)))
        val r = w * (0.16f + 0.14f * rnd.nextFloat()) * (1f + 0.12f * sin(TAU * (t * 4f + phase)))
        drawCircle(
            Brush.radialGradient(
                0f to col.copy(alpha = 0.75f), 0.62f to col.copy(alpha = 0.55f), 1f to Color.Transparent,
                center = Offset(cx, cy), radius = r
            ),
            radius = r, center = Offset(cx, cy)
        )
    }
    // calor embaixo e véu para o texto
    drawRect(Brush.verticalGradient(listOf(Color.Transparent, palette[0].copy(alpha = 0.25f)), startY = h * 0.7f, endY = h))
    drawRect(Color.Black.copy(alpha = 0.30f))
}

// ───────────────────────── Arcade 8-bit ─────────────────────────
private val INVADER = listOf(
    "..X.....X..",
    "...X...X...",
    "..XXXXXXX..",
    ".XX.XXX.XX.",
    "XXXXXXXXXXX",
    "X.XXXXXXX.X",
    "X.X.....X.X",
    "...XX.XX..."
)
private val INVADER_B = listOf(
    "..X.....X..",
    "X..X...X..X",
    "X.XXXXXXX.X",
    "XXX.XXX.XXX",
    "XXXXXXXXXXX",
    ".XXXXXXXXX.",
    "..X.....X..",
    ".X.......X."
)

internal fun DrawScope.drawArcade(colors: List<Color>, t: Float) {
    val palette = colors.ifEmpty { listOf(Color(0xFF39FF14), Color(0xFFFF3EA5), Color(0xFF00E0FF)) }
    val w = size.width; val h = size.height
    val px = 3.dp.toPx()
    // estrelas de pixel piscando
    val rnd = Random(8)
    repeat(60) { i ->
        val on = sin(TAU * (t * (3 + i % 4) + i * 0.21f)) > -0.2f
        if (on) drawRect(Color.White.copy(alpha = 0.35f), Offset((rnd.nextFloat() * w / px).toInt() * px, (rnd.nextFloat() * h / px).toInt() * px), Size(px, px))
    }
    // a frota marcha de lado a lado, 2 passos por segundo, trocando de pose
    val step = (t * 120f).toInt()
    val march = step % 16
    val dx = (if (march < 8) march else 16 - march) * 4f * px
    val pose = if (step % 2 == 0) INVADER else INVADER_B
    val spriteW = 11 * px
    val cols = ((w * 0.8f) / (spriteW + 6 * px)).toInt().coerceAtLeast(3)
    for (row in 0 until 3) {
        val col = palette[row % palette.size].copy(alpha = 0.22f)
        for (k in 0 until cols) {
            val ox = w * 0.05f + dx + k * (spriteW + 6 * px)
            val oy = h * 0.08f + row * 12 * px
            pose.forEachIndexed { yy, line ->
                line.forEachIndexed { xx, ch ->
                    if (ch == 'X') drawRect(col, Offset(ox + xx * px, oy + yy * px), Size(px, px))
                }
            }
        }
    }
    // chão quadriculado
    val ground = h - 10 * px
    var x = 0f
    var i = 0
    while (x < w) {
        drawRect(palette[0].copy(alpha = if (i % 2 == 0) 0.25f else 0.12f), Offset(x, ground), Size(2 * px, 2 * px))
        x += 2 * px; i++
    }
}

// ───────────────────────── Carnaval ─────────────────────────
internal fun DrawScope.drawCarnival(colors: List<Color>, t: Float) {
    val palette = colors.ifEmpty { listOf(Color(0xFFFFC400), Color(0xFF00D26A), Color(0xFFFF2E93), Color(0xFF2E9BFF)) }
    val w = size.width; val h = size.height
    // serpentinas onduladas descendo
    val stroke = 3.dp.toPx()
    for (s in 0 until 6) {
        val col = palette[s % palette.size].copy(alpha = 0.40f)
        val baseX = w * (0.08f + s * 0.17f)
        val path = Path()
        val shift = t * 4f + s * 0.13f
        var yy = -20f
        path.moveTo(baseX, yy)
        while (yy < h + 20f) {
            yy += 8f
            path.lineTo(baseX + 16.dp.toPx() * sin(TAU * (yy / (h * 0.35f) + shift)), yy)
        }
        drawPath(path, col, style = Stroke(stroke, cap = StrokeCap.Round))
    }
    // confete caindo e girando (a largura encolhe com o cosseno: parece virar no ar)
    val rnd = Random(42)
    repeat(70) { i ->
        val speed = 3 + rnd.nextInt(5)
        val phase = rnd.nextFloat()
        val p = (t * speed + phase) % 1f
        val x = rnd.nextFloat() * w + 14f * sin(TAU * (p * 3f + phase))
        val y = p * (h + 40f) - 20f
        val sz = (4 + rnd.nextInt(5)).dp.toPx()
        val flip = abs(cos(TAU * (t * (20 + i % 7) + phase)))
        rotate(rnd.nextFloat() * 360f + p * 720f, Offset(x, y)) {
            drawRect(palette[i % palette.size].copy(alpha = 0.75f), Offset(x - sz * flip / 2, y - sz / 3), Size(sz * flip + 1f, sz * 0.66f))
        }
    }
    // lantejoulas brilhando
    val spark = Random(5)
    repeat(24) { i ->
        val b = 0.5f + 0.5f * sin(TAU * (t * (6 + i % 5) + i * 0.3f))
        val pt = Offset(spark.nextFloat() * w, spark.nextFloat() * h)
        val r = 1.5f.dp.toPx() + 3.dp.toPx() * b
        drawLine(Color.White.copy(alpha = 0.6f * b), pt - Offset(r * 2, 0f), pt + Offset(r * 2, 0f), 1.2f)
        drawLine(Color.White.copy(alpha = 0.6f * b), pt - Offset(0f, r * 2), pt + Offset(0f, r * 2), 1.2f)
    }
}

// ───────────────────────── Ondas (Oceano) ─────────────────────────
internal fun DrawScope.drawWaves(colors: List<Color>, t: Float) {
    val palette = colors.ifEmpty { listOf(Color(0xFF38BDF8), Color(0xFF2DD4BF)) }
    val w = size.width; val h = size.height
    // luz vindo da superfície
    drawRect(Brush.verticalGradient(listOf(palette[0].copy(alpha = 0.16f), Color.Transparent), endY = h * 0.45f))
    for (layer in 0 until 4) {
        val base = h * (0.70f + layer * 0.08f)
        val amp = (10 + layer * 6).dp.toPx()
        val len = w * (1.1f - layer * 0.15f)
        val dir = if (layer % 2 == 0) 1f else -1f
        val shift = t * (3 + layer) * dir
        val path = Path().apply {
            moveTo(0f, h)
            var x = 0f
            lineTo(0f, base)
            while (x <= w + 8f) {
                lineTo(x, base + amp * sin(TAU * (x / len + shift)))
                x += 8f
            }
            lineTo(w, h); close()
        }
        drawPath(path, palette[layer % palette.size].copy(alpha = 0.07f + layer * 0.025f))
    }
    // bolhas subindo
    val rnd = Random(23)
    repeat(22) {
        val speed = 2 + rnd.nextInt(4)
        val phase = rnd.nextFloat()
        val p = (t * speed + phase) % 1f
        val x = rnd.nextFloat() * w + 8f * sin(TAU * (p * 4f + phase))
        val y = h * (1f - p)
        drawCircle(Color.White.copy(alpha = 0.18f * (1f - p)), (2 + rnd.nextInt(5)).dp.toPx(), Offset(x, y), style = Stroke(1.2f))
    }
}

// ───────────────────────── Vaga-lumes (Floresta) ─────────────────────────
internal fun DrawScope.drawFireflies(colors: List<Color>, t: Float) {
    val glow = colors.getOrElse(0) { Color(0xFFD9F99D) }
    val leaf = colors.getOrElse(1) { Color(0xFF2FBF7A) }
    val w = size.width; val h = size.height
    drawRect(Brush.radialGradient(listOf(leaf.copy(alpha = 0.18f), Color.Transparent), center = Offset(w / 2f, -h * 0.05f), radius = w * 1.1f))
    // silhueta de mato no pé da tela
    val rnd = Random(4)
    val grass = Path().apply {
        moveTo(0f, h)
        var x = 0f
        while (x < w) {
            val bw = (6 + rnd.nextInt(10)).dp.toPx()
            val bh = (18 + rnd.nextInt(46)).dp.toPx()
            lineTo(x + bw / 2, h - bh)
            lineTo(x + bw, h)
            x += bw
        }
        close()
    }
    drawPath(grass, Color.Black.copy(alpha = 0.35f))
    repeat(30) { i ->
        val phase = rnd.nextFloat()
        val bx = rnd.nextFloat() * w
        val by = h * (0.25f + 0.7f * rnd.nextFloat())
        val x = bx + 30.dp.toPx() * sin(TAU * (t * (1 + i % 3) + phase))
        val y = by + 20.dp.toPx() * cos(TAU * (t * (2 + i % 2) + phase * 1.3f))
        val blink = (sin(TAU * (t * (8 + i % 6) + phase))).coerceAtLeast(0f)
        if (blink > 0f) {
            drawCircle(glow.copy(alpha = 0.22f * blink), 9.dp.toPx(), Offset(x, y))
            drawCircle(glow.copy(alpha = 0.9f * blink), 1.8f.dp.toPx(), Offset(x, y))
        }
    }
}

// ───────────────────────── Art déco (Ouro) ─────────────────────────
internal fun DrawScope.drawDeco(colors: List<Color>) {
    val gold = colors.getOrElse(0) { Color(0xFFD8B25A) }
    val w = size.width; val h = size.height
    val c = Offset(w / 2f, -h * 0.02f)
    drawRect(Brush.radialGradient(listOf(gold.copy(alpha = 0.16f), Color.Transparent), center = c, radius = w * 1.1f))
    // leque de raios do topo
    val rays = 24
    val reach = h * 0.55f
    for (i in 0..rays) {
        val ang = PI.toFloat() * (0.08f + 0.84f * i / rays)
        val end = c + Offset(cos(ang), sin(ang)) * reach
        drawLine(gold.copy(alpha = if (i % 2 == 0) 0.13f else 0.06f), c, end, if (i % 2 == 0) 1.4f else 0.8f)
    }
    // arcos concêntricos
    for (k in 1..3) drawCircle(gold.copy(alpha = 0.10f), radius = w * 0.18f * k, center = c, style = Stroke(1f))
    // moldura dupla com cantos escalonados
    val m = 10.dp.toPx(); val g = 4.dp.toPx(); val step = 14.dp.toPx()
    listOf(m, m + g).forEach { inset ->
        drawRect(gold.copy(alpha = 0.22f), Offset(inset, inset), Size(w - 2 * inset, h - 2 * inset), style = Stroke(1f))
    }
    listOf(Offset(m, m) to Offset(1f, 1f), Offset(w - m, m) to Offset(-1f, 1f), Offset(m, h - m) to Offset(1f, -1f), Offset(w - m, h - m) to Offset(-1f, -1f))
        .forEach { (p, d) ->
            drawLine(gold.copy(alpha = 0.35f), p + Offset(step * d.x, 0f), p + Offset(step * d.x, step * d.y), 1.4f)
            drawLine(gold.copy(alpha = 0.35f), p + Offset(0f, step * d.y), p + Offset(step * d.x, step * d.y), 1.4f)
        }
}
