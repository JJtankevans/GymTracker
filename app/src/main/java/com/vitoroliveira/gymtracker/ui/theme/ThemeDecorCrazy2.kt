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
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.sin
import kotlin.random.Random

/*
 * Segunda leva de decorações dos temas "Loucos" (28/10/2026).
 * Mesmo contrato de ThemeDecor.kt: [t] vai de 0 a 1 em 60 s e toda animação usa múltiplos inteiros de t,
 * então o loop fecha sem salto. Quantidade de formas contida (o fundo desenha a cada quadro).
 */

private const val TAU = (2 * PI).toFloat()

private fun frac(x: Float) = x - kotlin.math.floor(x)

// ───────────────────────── Tempestade ─────────────────────────
internal fun DrawScope.drawStorm(colors: List<Color>, t: Float) {
    val bolt = colors.getOrElse(0) { Color(0xFFBFE3FF) }
    val w = size.width; val h = size.height
    // nuvens pesadas no topo
    val rnd = Random(9)
    repeat(7) { i ->
        val cx = frac(rnd.nextFloat() + t * (1 + i % 2)) * (w + 300f) - 150f
        val cy = h * (0.02f + 0.1f * rnd.nextFloat())
        val r = w * (0.22f + 0.15f * rnd.nextFloat())
        drawCircle(Brush.radialGradient(listOf(Color(0xFF2A3446).copy(alpha = 0.55f), Color.Transparent), Offset(cx, cy), r), r, Offset(cx, cy))
    }
    // chuva inclinada
    val len = 16.dp.toPx()
    repeat(120) {
        val speed = 40 + rnd.nextInt(30)
        val phase = rnd.nextFloat()
        val p = frac(t * speed + phase)
        val x = frac(rnd.nextFloat() + p * 0.15f) * (w + 80f) - 40f
        val y = p * (h + len * 2) - len
        drawLine(Color.White.copy(alpha = 0.16f), Offset(x, y), Offset(x - len * 0.25f, y + len), 1.2f, cap = StrokeCap.Round)
    }
    // relâmpago: 10 janelas por minuto, o raio aparece em 2 delas de 3
    val cycle = (t * 10f).toInt()
    val local = frac(t * 10f)
    if (cycle % 3 != 2 && local < 0.035f) {
        val flash = 1f - local / 0.035f
        drawRect(bolt.copy(alpha = 0.22f * flash))
        val br = Random(cycle * 7 + 1)
        var x = w * (0.2f + 0.6f * br.nextFloat())
        var y = 0f
        val path = Path().apply { moveTo(x, y) }
        while (y < h * 0.62f) {
            y += h * (0.04f + 0.05f * br.nextFloat())
            x += (br.nextFloat() - 0.5f) * w * 0.12f
            path.lineTo(x, y)
        }
        drawPath(path, bolt.copy(alpha = 0.35f * flash), style = Stroke(9.dp.toPx(), cap = StrokeCap.Round))
        drawPath(path, Color.White.copy(alpha = 0.9f * flash), style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
    }
    drawRect(Color.Black.copy(alpha = 0.25f))
}

// ───────────────────────── Discoteca ─────────────────────────
internal fun DrawScope.drawDisco(colors: List<Color>, t: Float) {
    val palette = colors.ifEmpty { listOf(Color(0xFFFF3DCB), Color(0xFF3DDCFF), Color(0xFFFFE03D), Color(0xFF7CFF3D)) }
    val w = size.width; val h = size.height
    val ball = Offset(w / 2f, h * 0.06f)
    val br = 34.dp.toPx()
    // fachos coloridos saindo da bola
    rotate(t * 360f * 3f, ball) {
        for (i in 0 until 10) {
            val ang = TAU * i / 10f
            val end = ball + Offset(cos(ang), sin(ang)) * (h * 1.2f)
            val c = palette[i % palette.size]
            drawLine(Brush.linearGradient(listOf(c.copy(alpha = 0.28f), Color.Transparent), ball, end), ball, end, 26.dp.toPx())
        }
    }
    // pontos de luz varrendo a parede
    val rnd = Random(21)
    repeat(26) { i ->
        val c = palette[i % palette.size]
        val ox = rnd.nextFloat(); val oy = rnd.nextFloat()
        val x = frac(ox + t * (2 + i % 3)) * w
        val y = h * (0.15f + 0.8f * frac(oy + t * (1 + i % 2)))
        val r = (5 + rnd.nextInt(10)).dp.toPx()
        drawCircle(c.copy(alpha = 0.55f), r, Offset(x, y))
        drawCircle(c.copy(alpha = 0.15f), r * 2.4f, Offset(x, y))
    }
    // a bola espelhada: facetas em grade com brilho que gira
    drawCircle(Color(0xFFB8BCC8), br, ball)
    val facet = 7.dp.toPx()
    var fy = ball.y - br
    var row = 0
    while (fy < ball.y + br) {
        var fx = ball.x - br
        var col = 0
        while (fx < ball.x + br) {
            val cx = fx + facet / 2; val cy = fy + facet / 2
            if ((cx - ball.x) * (cx - ball.x) + (cy - ball.y) * (cy - ball.y) < br * br) {
                val shine = 0.5f + 0.5f * sin(TAU * (t * 30f + (row * 7 + col * 3) * 0.11f))
                drawRect(lerp(Color(0xFF6B7080), Color.White, shine), Offset(fx + 0.6f, fy + 0.6f), Size(facet - 1.2f, facet - 1.2f))
            }
            fx += facet; col++
        }
        fy += facet; row++
    }
    drawLine(Color.White.copy(alpha = 0.5f), Offset(ball.x, 0f), Offset(ball.x, ball.y - br), 1.5f)
    drawRect(Color.Black.copy(alpha = 0.30f))
}

// ───────────────────────── Caleidoscópio ─────────────────────────
internal fun DrawScope.drawKaleido(colors: List<Color>, t: Float) {
    val palette = colors.ifEmpty { listOf(Color(0xFFFF4D4D), Color(0xFFFFB84D), Color(0xFFFFF04D), Color(0xFF4DFF88), Color(0xFF4DB8FF), Color(0xFFB84DFF)) }
    val c = Offset(size.width / 2f, size.height * 0.4f)
    val reach = size.maxDimension
    // raios de arco-íris girando
    rotate(t * 360f * 2f, c) {
        val n = 24
        for (i in 0 until n) {
            val a0 = TAU * i / n
            val a1 = TAU * (i + 0.5f) / n
            val path = Path().apply {
                moveTo(c.x, c.y)
                lineTo(c.x + reach * cos(a0), c.y + reach * sin(a0))
                lineTo(c.x + reach * cos(a1), c.y + reach * sin(a1))
                close()
            }
            drawPath(path, palette[i % palette.size].copy(alpha = 0.22f))
        }
    }
    // polígonos concêntricos girando em sentidos alternados
    for (k in 1..6) {
        val sides = 3 + k % 4
        val r = k * 46.dp.toPx() * (1f + 0.08f * sin(TAU * (t * 8f + k * 0.2f)))
        rotate((if (k % 2 == 0) 1 else -1) * t * 360f * (1 + k), c) {
            val path = Path()
            for (s in 0..sides) {
                val a = TAU * s / sides
                val p = Offset(c.x + r * cos(a), c.y + r * sin(a))
                if (s == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
            }
            drawPath(path, palette[(k + 2) % palette.size].copy(alpha = 0.45f), style = Stroke(3.dp.toPx()))
        }
    }
    drawRect(Color.Black.copy(alpha = 0.40f))
}

// ───────────────────────── Abissal ─────────────────────────
internal fun DrawScope.drawAbyss(colors: List<Color>, t: Float) {
    val glow = colors.getOrElse(0) { Color(0xFF6CF6FF) }
    val pink = colors.getOrElse(1) { Color(0xFFFF6CF0) }
    val w = size.width; val h = size.height
    drawRect(Brush.verticalGradient(listOf(glow.copy(alpha = 0.08f), Color.Transparent), endY = h * 0.3f))
    // plâncton brilhando
    val rnd = Random(31)
    repeat(70) { i ->
        val x = frac(rnd.nextFloat() + 0.02f * sin(TAU * (t * 2f + i))) * w
        val y = frac(rnd.nextFloat() - t * (1 + i % 3)) * h
        val b = 0.4f + 0.6f * abs(sin(TAU * (t * (5 + i % 7) + i * 0.13f)))
        drawCircle((if (i % 4 == 0) pink else glow).copy(alpha = 0.55f * b), 1.4f.dp.toPx(), Offset(x, y))
    }
    // águas-vivas subindo com pulsação
    repeat(5) { i ->
        val col = if (i % 2 == 0) glow else pink
        val phase = rnd.nextFloat()
        val p = frac(t * (1 + i % 2) + phase)
        val cx = w * (0.12f + 0.76f * rnd.nextFloat()) + 18.dp.toPx() * sin(TAU * (t * 3f + phase))
        val cy = h * (1.1f - 1.3f * p)
        val pulse = 0.5f + 0.5f * sin(TAU * (t * 40f + phase))
        val rw = (22 + i * 5).dp.toPx() * (0.85f + 0.2f * pulse)
        val rh = rw * (0.75f - 0.15f * pulse)
        drawCircle(Brush.radialGradient(listOf(col.copy(alpha = 0.25f), Color.Transparent), Offset(cx, cy), rw * 2.2f), rw * 2.2f, Offset(cx, cy))
        val dome = Path().apply {
            moveTo(cx - rw, cy)
            cubicTo(cx - rw, cy - rh * 1.4f, cx + rw, cy - rh * 1.4f, cx + rw, cy)
            close()
        }
        drawPath(dome, col.copy(alpha = 0.35f))
        drawPath(dome, col.copy(alpha = 0.8f), style = Stroke(1.5f.dp.toPx()))
        for (k in 0 until 5) {
            val tx = cx - rw * 0.7f + k * rw * 0.35f
            val tent = Path().apply {
                moveTo(tx, cy)
                var yy = cy
                while (yy < cy + rw * 2.2f) {
                    yy += 6f
                    lineTo(tx + 6.dp.toPx() * sin(TAU * ((yy - cy) / (rw * 1.2f) + t * 20f + k * 0.3f)), yy)
                }
            }
            drawPath(tent, col.copy(alpha = 0.4f), style = Stroke(1.2f.dp.toPx(), cap = StrokeCap.Round))
        }
    }
}

// ───────────────────────── Galáxia ─────────────────────────
internal fun DrawScope.drawGalaxy(colors: List<Color>, t: Float) {
    val core = colors.getOrElse(0) { Color(0xFFFFD6A5) }
    val arm = colors.getOrElse(1) { Color(0xFF9D8CFF) }
    val neb = colors.getOrElse(2) { Color(0xFFFF7EB6) }
    val w = size.width; val h = size.height
    val c = Offset(w / 2f, h * 0.38f)
    val rnd = Random(77)
    // estrelas de fundo piscando
    repeat(90) { i ->
        val b = 0.3f + 0.7f * abs(sin(TAU * (t * (3 + i % 5) + i * 0.17f)))
        drawCircle(Color.White.copy(alpha = 0.5f * b), (0.6f + rnd.nextFloat()).dp.toPx(), Offset(rnd.nextFloat() * w, rnd.nextFloat() * h))
    }
    // nebulosas
    drawCircle(Brush.radialGradient(listOf(neb.copy(alpha = 0.22f), Color.Transparent), Offset(w * 0.2f, h * 0.75f), w * 0.6f), w * 0.6f, Offset(w * 0.2f, h * 0.75f))
    drawCircle(Brush.radialGradient(listOf(arm.copy(alpha = 0.2f), Color.Transparent), Offset(w * 0.9f, h * 0.2f), w * 0.5f), w * 0.5f, Offset(w * 0.9f, h * 0.2f))
    // braços espirais (pontos ao longo de duas espirais logarítmicas) girando
    rotate(t * 360f * 2f, c) {
        val maxR = w * 0.62f
        for (a in 0 until 2) {
            for (i in 0 until 140) {
                val f = i / 140f
                val th = f * 3.2f * PI.toFloat() + a * PI.toFloat()
                val r = f * maxR
                val jitter = (rnd.nextFloat() - 0.5f) * 26.dp.toPx() * f
                val p = Offset(c.x + (r + jitter) * cos(th), c.y + (r + jitter) * sin(th) * 0.55f)
                drawCircle(lerp(core, arm, f).copy(alpha = 0.7f * (1f - f * 0.6f)), (2.2f - f * 1.2f).dp.toPx(), p)
            }
        }
    }
    drawCircle(Brush.radialGradient(listOf(core.copy(alpha = 0.8f), core.copy(alpha = 0.15f), Color.Transparent), c, 70.dp.toPx()), 70.dp.toPx(), c)
    drawRect(Color.Black.copy(alpha = 0.22f))
}

// ───────────────────────── Estática (TV fora do ar) ─────────────────────────
internal fun DrawScope.drawStatic(colors: List<Color>, t: Float) {
    val w = size.width; val h = size.height
    // chuvisco: 15 quadros por segundo
    val frame = (t * 900f).toInt()
    val rnd = Random(frame)
    val cell = 4.dp.toPx()
    repeat(420) {
        val g = rnd.nextFloat()
        drawRect(Color(g, g, g, 0.10f + 0.12f * g), Offset(rnd.nextFloat() * w, rnd.nextFloat() * h), Size(cell * (1 + rnd.nextInt(3)), cell))
    }
    // faixa clara rolando para baixo
    val by = frac(t * 6f) * (h + 200f) - 100f
    drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color.White.copy(alpha = 0.08f), Color.Transparent), startY = by - 80f, endY = by + 80f))
    // barras de cor no rodapé, como fim de programação
    val bars = colors.ifEmpty { listOf(Color.White, Color.Yellow, Color.Cyan, Color.Green, Color.Magenta, Color.Red, Color.Blue) }
    val bw = w / bars.size
    val top = h - 10.dp.toPx()
    bars.forEachIndexed { i, c -> drawRect(c.copy(alpha = 0.35f), Offset(i * bw, top), Size(bw + 1f, 10.dp.toPx())) }
    drawRect(Brush.radialGradient(0.6f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.55f), center = center, radius = size.maxDimension * 0.7f))
}

// ───────────────────────── Holográfico ─────────────────────────
internal fun DrawScope.drawHolo(colors: List<Color>, t: Float) {
    val palette = colors.ifEmpty { listOf(Color(0xFFFFB3E6), Color(0xFFB3E5FF), Color(0xFFC8FFD9), Color(0xFFFFF3B3), Color(0xFFD9B3FF)) }
    val w = size.width; val h = size.height
    // reflexo perolado girando
    val c = Offset(w * (0.5f + 0.3f * sin(TAU * t * 2f)), h * (0.4f + 0.2f * cos(TAU * t * 2f)))
    rotate(t * 360f, c) {
        drawRect(Brush.sweepGradient(palette + palette.first(), center = c), alpha = 0.55f)
    }
    // faixas brilhantes diagonais deslizando
    for (k in 0 until 3) {
        val x = frac(t * 4f + k / 3f) * (w + h) * 1.4f - h
        drawRect(
            Brush.linearGradient(
                listOf(Color.Transparent, Color.White.copy(alpha = 0.55f), Color.Transparent),
                start = Offset(x, 0f), end = Offset(x + h * 0.35f, h * 0.6f)
            )
        )
    }
    // véu claro para o texto escuro ficar legível
    drawRect(Color.White.copy(alpha = 0.45f))
}

// ───────────────────────── Assombrado ─────────────────────────
internal fun DrawScope.drawHaunted(colors: List<Color>, t: Float) {
    val moon = colors.getOrElse(0) { Color(0xFFFFF1C2) }
    val fog = colors.getOrElse(1) { Color(0xFF8A7CC2) }
    val w = size.width; val h = size.height
    val mc = Offset(w * 0.78f, h * 0.12f)
    val mr = 46.dp.toPx()
    drawCircle(Brush.radialGradient(listOf(moon.copy(alpha = 0.35f), Color.Transparent), mc, mr * 3.2f), mr * 3.2f, mc)
    drawCircle(moon.copy(alpha = 0.9f), mr, mc)
    drawCircle(Color.Black.copy(alpha = 0.08f), mr * 0.22f, mc + Offset(-mr * 0.3f, -mr * 0.2f))
    drawCircle(Color.Black.copy(alpha = 0.06f), mr * 0.14f, mc + Offset(mr * 0.35f, mr * 0.3f))
    // morcegos atravessando e batendo as asas
    val rnd = Random(13)
    repeat(9) { i ->
        val speed = 3 + rnd.nextInt(4)
        val phase = rnd.nextFloat()
        val p = frac(t * speed + phase)
        val x = p * (w + 80f) - 40f
        val y = h * (0.08f + 0.4f * rnd.nextFloat()) + 18.dp.toPx() * sin(TAU * (p * 3f + phase))
        val s = (8 + rnd.nextInt(8)).dp.toPx()
        val flap = sin(TAU * (t * (90 + i * 7) + phase))
        val path = Path().apply {
            moveTo(x - s, y - s * 0.5f * flap)
            quadraticTo(x - s * 0.5f, y + s * 0.15f, x, y)
            quadraticTo(x + s * 0.5f, y + s * 0.15f, x + s, y - s * 0.5f * flap)
        }
        drawPath(path, Color.Black.copy(alpha = 0.85f), style = Stroke(2.2f.dp.toPx(), cap = StrokeCap.Round))
        drawCircle(Color.Black.copy(alpha = 0.85f), s * 0.18f, Offset(x, y))
    }
    // neblina rasteira em camadas
    for (k in 0 until 3) {
        val base = h * (0.78f + k * 0.07f)
        val shift = frac(t * (2 + k)) * w
        for (j in -1..2) {
            val cx = j * w * 0.6f + shift * (if (k % 2 == 0) 1 else -1)
            val r = w * 0.45f
            drawCircle(Brush.radialGradient(listOf(fog.copy(alpha = 0.18f), Color.Transparent), Offset(cx, base), r), r, Offset(cx, base))
        }
    }
    // cerca torta no pé da tela
    val post = 10.dp.toPx()
    var x = 0f
    var i = 0
    while (x < w) {
        val ph = (34 + (i * 37) % 22).dp.toPx()
        val tilt = ((i * 13) % 7 - 3) * 1.5f
        rotate(tilt, Offset(x + post / 2, h)) {
            val p = Path().apply {
                moveTo(x, h); lineTo(x, h - ph); lineTo(x + post / 2, h - ph - post * 0.8f); lineTo(x + post, h - ph); lineTo(x + post, h); close()
            }
            drawPath(p, Color.Black.copy(alpha = 0.7f))
        }
        x += post * 1.9f; i++
    }
}

// ───────────────────────── Réveillon (fogos) ─────────────────────────
internal fun DrawScope.drawFireworks(colors: List<Color>, t: Float) {
    val palette = colors.ifEmpty { listOf(Color(0xFFFFD447), Color(0xFFFF4D8D), Color(0xFF4DD2FF), Color(0xFF9DFF4D)) }
    val w = size.width; val h = size.height
    // 6 fogos defasados, cada um explode 4 vezes por minuto em lugares diferentes
    for (f in 0 until 6) {
        val cycleF = t * 4f + f / 6f
        val cycle = cycleF.toInt()
        val local = frac(cycleF)
        val r = Random(cycle * 97 + f * 13)
        val col = palette[(cycle + f) % palette.size]
        val cx = w * (0.15f + 0.7f * r.nextFloat())
        val cy = h * (0.12f + 0.35f * r.nextFloat())
        if (local < 0.25f) {
            // subida com rastro
            val p = local / 0.25f
            val y = h - (h - cy) * (1f - (1f - p) * (1f - p))
            drawLine(col.copy(alpha = 0.6f), Offset(cx, y), Offset(cx, y + 26.dp.toPx() * (1f - p)), 2.dp.toPx(), cap = StrokeCap.Round)
        } else {
            // explosão: partículas abrindo, caindo e apagando
            val p = (local - 0.25f) / 0.75f
            val radius = 90.dp.toPx() * (1f - exp(-4f * p))
            val fall = 40.dp.toPx() * p * p
            val alpha = (1f - p).coerceIn(0f, 1f)
            val n = 22
            for (k in 0 until n) {
                val a = TAU * k / n + r.nextFloat() * 0.2f
                val pt = Offset(cx + radius * cos(a), cy + radius * sin(a) + fall)
                drawCircle(col.copy(alpha = 0.85f * alpha), 2.2f.dp.toPx(), pt)
                drawLine(col.copy(alpha = 0.3f * alpha), Offset(cx + radius * 0.7f * cos(a), cy + radius * 0.7f * sin(a) + fall * 0.7f), pt, 1.2f)
            }
            if (p < 0.15f) drawCircle(Color.White.copy(alpha = 0.5f * (1f - p / 0.15f)), 22.dp.toPx(), Offset(cx, cy))
        }
    }
    drawRect(Color.Black.copy(alpha = 0.15f))
}

// ───────────────────────── Blocos caindo ─────────────────────────
private val PIECES = listOf(
    listOf(0 to 0, 1 to 0, 2 to 0, 3 to 0),
    listOf(0 to 0, 1 to 0, 0 to 1, 1 to 1),
    listOf(0 to 0, 1 to 0, 2 to 0, 1 to 1),
    listOf(0 to 0, 0 to 1, 0 to 2, 1 to 2),
    listOf(1 to 0, 1 to 1, 1 to 2, 0 to 2),
    listOf(1 to 0, 2 to 0, 0 to 1, 1 to 1),
    listOf(0 to 0, 1 to 0, 1 to 1, 2 to 1)
)

internal fun DrawScope.drawBlocks(colors: List<Color>, t: Float) {
    val palette = colors.ifEmpty { listOf(Color(0xFF00E5FF), Color(0xFFFFE600), Color(0xFFB000FF), Color(0xFFFF8A00), Color(0xFF0057FF), Color(0xFF00E676), Color(0xFFFF1744)) }
    val w = size.width; val h = size.height
    val cell = 18.dp.toPx()
    val cols = (w / cell).toInt()
    // grade de fundo
    var gx = 0f
    while (gx < w) { drawLine(Color.White.copy(alpha = 0.035f), Offset(gx, 0f), Offset(gx, h), 1f); gx += cell }
    // peças caindo em degraus (uma linha por "tique"), girando de vez em quando
    val rnd = Random(64)
    repeat(12) { i ->
        val speed = 3 + rnd.nextInt(4)
        val phase = rnd.nextFloat()
        val piece = PIECES[i % PIECES.size]
        val col = palette[i % palette.size]
        val rows = (h / cell).toInt() + 4
        val p = frac(t * speed + phase)
        val row = (p * rows).toInt() - 3
        val c0 = rnd.nextInt((cols - 3).coerceAtLeast(1))
        val turn = (p * 4).toInt() % 4
        piece.forEach { (dx, dy) ->
            val (rx, ry) = when (turn) { 1 -> -dy + 2 to dx; 2 -> -dx + 3 to -dy + 2; 3 -> dy to -dx + 3; else -> dx to dy }
            val o = Offset((c0 + rx) * cell, (row + ry) * cell)
            drawRect(col.copy(alpha = 0.30f), o, Size(cell - 2f, cell - 2f))
            drawRect(Color.White.copy(alpha = 0.18f), o, Size(cell - 2f, 3f))
        }
    }
    // pilha no fundo, com uma linha "piscando" como se fosse sumir
    val stack = Random(5)
    val blink = 0.5f + 0.5f * sin(TAU * t * 60f)
    for (r in 0 until 3) {
        for (c in 0 until cols) {
            if (r == 0 || stack.nextFloat() < 0.7f) {
                val col = if (r == 0) Color.White.copy(alpha = 0.10f + 0.25f * blink) else palette[stack.nextInt(palette.size)].copy(alpha = 0.22f)
                drawRect(col, Offset(c * cell, h - (r + 1) * cell), Size(cell - 2f, cell - 2f))
            }
        }
    }
}

// ───────────────────────── Cardio (monitor cardíaco) ─────────────────────────
/** Forma de um batimento (0..1 → altura relativa), como no eletrocardiograma. */
private fun beat(x: Float): Float = when {
    x < 0.10f -> 0f
    x < 0.16f -> 0.12f * sin(PI.toFloat() * (x - 0.10f) / 0.06f)     // onda P
    x < 0.22f -> 0f
    x < 0.24f -> -0.18f * (x - 0.22f) / 0.02f                         // Q
    x < 0.27f -> -0.18f + 1.18f * (x - 0.24f) / 0.03f                  // subida do R
    x < 0.30f -> 1f - 1.4f * (x - 0.27f) / 0.03f                       // descida até S
    x < 0.33f -> -0.4f + 0.4f * (x - 0.30f) / 0.03f
    x < 0.45f -> 0f
    x < 0.58f -> 0.22f * sin(PI.toFloat() * (x - 0.45f) / 0.13f)     // onda T
    else -> 0f
}

internal fun DrawScope.drawEcg(colors: List<Color>, t: Float) {
    val line = colors.getOrElse(0) { Color(0xFF39FF88) }
    val w = size.width; val h = size.height
    // grade de papel milimetrado do monitor
    val small = 12.dp.toPx()
    var x = 0f
    var i = 0
    while (x < w) { drawLine(line.copy(alpha = if (i % 5 == 0) 0.10f else 0.04f), Offset(x, 0f), Offset(x, h), 1f); x += small; i++ }
    var y = 0f; i = 0
    while (y < h) { drawLine(line.copy(alpha = if (i % 5 == 0) 0.10f else 0.04f), Offset(0f, y), Offset(w, y), 1f); y += small; i++ }
    // traço que vai sendo escrito da esquerda para a direita (80 bpm), apagando atrás
    val baseY = h * 0.72f
    val amp = h * 0.12f
    val beatW = w / 2.2f
    val head = frac(t * 20f) * w
    val path = Path()
    var started = false
    var px = 0f
    while (px <= w) {
        val v = beat(frac(px / beatW))
        val py = baseY - v * amp
        if (!started) { path.moveTo(px, py); started = true } else path.lineTo(px, py)
        px += 3f
    }
    // brilho só perto da "cabeça": desenha a linha inteira fraca e um trecho forte antes da cabeça
    drawPath(path, line.copy(alpha = 0.18f), style = Stroke(2.dp.toPx()))
    val trail = w * 0.35f
    val bright = Path()
    started = false
    px = max(0f, head - trail)
    while (px <= head) {
        val py = baseY - beat(frac(px / beatW)) * amp
        if (!started) { bright.moveTo(px, py); started = true } else bright.lineTo(px, py)
        px += 3f
    }
    drawPath(bright, line.copy(alpha = 0.25f), style = Stroke(8.dp.toPx(), cap = StrokeCap.Round))
    drawPath(bright, line.copy(alpha = 0.95f), style = Stroke(2.2f.dp.toPx(), cap = StrokeCap.Round))
    val hy = baseY - beat(frac(head / beatW)) * amp
    drawCircle(Color.White, 3.dp.toPx(), Offset(head, hy))
    drawCircle(line.copy(alpha = 0.35f), 10.dp.toPx(), Offset(head, hy))
}

// ───────────────────────── Rave (lasers) ─────────────────────────
internal fun DrawScope.drawLasers(colors: List<Color>, t: Float) {
    val palette = colors.ifEmpty { listOf(Color(0xFF39FF14), Color(0xFFFF00E6), Color(0xFF00F0FF)) }
    val w = size.width; val h = size.height
    // fumaça
    val rnd = Random(3)
    repeat(5) {
        val cx = frac(rnd.nextFloat() + t * 2f) * w
        val cy = h * (0.3f + 0.5f * rnd.nextFloat())
        drawCircle(Brush.radialGradient(listOf(Color.White.copy(alpha = 0.05f), Color.Transparent), Offset(cx, cy), w * 0.5f), w * 0.5f, Offset(cx, cy))
    }
    // canhões de laser nos dois cantos de baixo, varrendo em leque
    val sources = listOf(Offset(0f, h), Offset(w, h), Offset(w / 2f, h + 20f))
    sources.forEachIndexed { s, src ->
        val col = palette[s % palette.size]
        for (k in 0 until 5) {
            val base = when (s) { 0 -> -70f; 1 -> -110f; else -> -90f }
            val sweep = 35f * sin(TAU * (t * (12 + s * 3) + k * 0.12f))
            val ang = Math.toRadians((base + sweep + (k - 2) * 7f).toDouble()).toFloat()
            val end = src + Offset(cos(ang), sin(ang)) * (h * 1.3f)
            drawLine(col.copy(alpha = 0.10f), src, end, 7.dp.toPx())
            drawLine(col.copy(alpha = 0.7f), src, end, 1.4f.dp.toPx())
        }
    }
    // estrobo: um piscar curto 2x por segundo, fraco
    val strobe = frac(t * 120f)
    if (strobe < 0.06f) drawRect(Color.White.copy(alpha = 0.06f))
    drawRect(Color.Black.copy(alpha = 0.18f))
}
