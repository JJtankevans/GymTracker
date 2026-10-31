package com.vitoroliveira.gymtracker.ui.theme

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.random.Random

/*
 * Os temas "antigos" (Profissionais, Ousados e Abstratos) ganham cenários próprios, no nível dos Loucos
 * (31/10/2026). Primeira parte: Sistema, Studio, Carbon, Oceano, Tinta, Nórdico, Obsidiana, Cobalto e Floresta.
 *
 * Mesmo contrato de ThemeDecor.kt: [t] vai de 0 a 1 em 60 s e toda animação usa múltiplos inteiros de t (o loop
 * fecha sem salto). [level] é a intensidade: 0 = Calmo, 1 = Normal, 2 = Insano (mais elementos; o Insano também
 * roda o relógio 3x mais rápido, feito por quem chama).
 */

private const val TAU = (2 * PI).toFloat()

private fun frac(x: Float) = x - floor(x)
private fun lv(level: Int, calm: Int, normal: Int, wild: Int) = when (level) { 0 -> calm; 1 -> normal; else -> wild }
private fun Color.a(alpha: Float) = copy(alpha = alpha)
private fun List<Color>.at(i: Int, fallback: Color) = getOrElse(i) { fallback }

// ───────────────────────── Sistema: formas do Material You ─────────────────────────
internal fun DrawScope.drawMaterial(colors: List<Color>, t: Float, level: Int) {
    val w = size.width; val h = size.height
    val c = listOf(colors.at(0, Color(0xFF7C9CFF)), colors.at(1, Color(0xFFC58BFF)), colors.at(2, Color(0xFF5BE3C4)))
    // manchas de cor que passeiam
    for (i in 0 until lv(level, 3, 4, 6)) {
        val col = c[i % 3]
        val cx = w * (0.5f + 0.40f * sin(t * (1 + i % 2) * TAU + i * 1.9f))
        val cy = h * (0.5f + 0.40f * cos(t * (1 + (i + 1) % 2) * TAU + i * 2.3f))
        val r = maxOf(w, h) * (0.30f + 0.05f * (i % 3))
        drawCircle(Brush.radialGradient(listOf(col.a(0.32f), Color.Transparent), Offset(cx, cy), r), r, Offset(cx, cy))
    }
    // formas "expressivas" girando devagar, cada uma com cantos diferentes
    for (i in 0 until lv(level, 2, 3, 5)) {
        val col = c[(i + 1) % 3]
        val cx = w * (0.12f + 0.76f * frac(0.37f * i + 0.21f))
        val cy = h * (0.10f + 0.80f * frac(0.61f * i + 0.33f))
        val s = w * (0.14f + 0.05f * (i % 2))
        val dir = if (i % 2 == 0) 1f else -1f
        rotate(t * 360f * dir, Offset(cx, cy)) {
            val corner = s * (0.18f + 0.17f * (i % 3))
            drawRoundRect(col.a(0.11f), Offset(cx - s / 2, cy - s / 2), Size(s, s), CornerRadius(corner))
            drawRoundRect(col.a(0.24f), Offset(cx - s / 2, cy - s / 2), Size(s, s), CornerRadius(corner), style = Stroke(1.5.dp.toPx()))
        }
    }
}

// ───────────────────────── Studio: refletores de palco ─────────────────────────
internal fun DrawScope.drawSpotlight(colors: List<Color>, t: Float, level: Int) {
    val w = size.width; val h = size.height
    val warm = colors.at(0, Color(0xFFFFF0CC)); val cool = colors.at(1, Color(0xFF9CC9FF))
    val n = lv(level, 2, 3, 4)
    for (i in 0 until n) {
        val ox = w * (0.12f + 0.76f * i / (n - 1).coerceAtLeast(1))
        val oy = -h * 0.03f
        val ang = sin(t * (1 + i % 2) * TAU + i * 2.1f) * 0.45f // radianos desde a vertical
        val len = h * 1.15f
        val spread = 0.11f
        val left = Offset(ox + sin(ang - spread) * len, oy + cos(ang - spread) * len)
        val right = Offset(ox + sin(ang + spread) * len, oy + cos(ang + spread) * len)
        val col = if (i % 2 == 0) warm else cool
        val cone = Path().apply { moveTo(ox, oy); lineTo(left.x, left.y); lineTo(right.x, right.y); close() }
        drawPath(cone, Brush.linearGradient(listOf(col.a(0.26f), Color.Transparent), Offset(ox, oy), Offset(ox + sin(ang) * len, oy + cos(ang) * len)))
        drawCircle(col.a(0.55f), 5.dp.toPx(), Offset(ox, 6.dp.toPx()))
    }
    // poeira flutuando no feixe
    val rnd = Random(21)
    repeat(lv(level, 12, 22, 44)) {
        val x = w * rnd.nextFloat(); val sp = 1 + rnd.nextInt(3); val ph = rnd.nextFloat(); val rr = 1f + rnd.nextFloat() * 1.4f
        val y = h * (1f - frac(t * sp + ph))
        val a = 0.15f + 0.35f * (0.5f + 0.5f * sin(t * (2 + it % 3) * TAU + ph * 9f))
        drawCircle(warm.a(a), rr.dp.toPx(), Offset(x + sin(t * TAU + ph * 6f) * 8.dp.toPx(), y))
    }
}

// ───────────────────────── Carbon: fibra de carbono com brilho e HUD ─────────────────────────
internal fun DrawScope.drawCarbon(colors: List<Color>, t: Float, level: Int) {
    val w = size.width; val h = size.height
    val accent = colors.at(0, Color(0xFFC6F432)); val steel = colors.at(1, Color(0xFF7DD3FC))
    // trama: duas hachuras diagonais cruzadas
    val step = 9.dp.toPx()
    var x = -h
    while (x < w) {
        drawLine(Color.White.a(0.030f), Offset(x, 0f), Offset(x + h, h), 2.dp.toPx())
        drawLine(Color.White.a(0.022f), Offset(x, h), Offset(x + h, 0f), 2.dp.toPx())
        x += step
    }
    // brilho de verniz varrendo a peça
    for (k in 0 until lv(level, 1, 1, 2)) {
        val p = frac(t * 4f + k * 0.5f)
        val cx = -w * 0.3f + p * w * 1.6f
        val band = w * 0.35f
        drawRect(Brush.linearGradient(listOf(Color.Transparent, accent.a(0.11f), Color.Transparent), Offset(cx - band, 0f), Offset(cx + band, h * 0.6f)))
    }
    // colchetes de HUD nos cantos, pulsando
    val pulse = 0.35f + 0.25f * (0.5f + 0.5f * sin(t * 6f * TAU))
    val m = 14.dp.toPx(); val len = 26.dp.toPx(); val sw = 2.dp.toPx()
    fun corner(cx: Float, cy: Float, sx: Float, sy: Float) {
        drawLine(accent.a(pulse), Offset(cx, cy), Offset(cx + sx * len, cy), sw)
        drawLine(accent.a(pulse), Offset(cx, cy), Offset(cx, cy + sy * len), sw)
    }
    corner(m, m, 1f, 1f); corner(w - m, m, -1f, 1f); corner(m, h - m, 1f, -1f); corner(w - m, h - m, -1f, -1f)
    // régua de telemetria na borda direita
    if (level >= 1) {
        val tick = 14.dp.toPx()
        val off = frac(t * 30f) * tick
        var y = -tick + off
        var idx = 0
        while (y < h) {
            val long = idx % 5 == 0
            drawLine(steel.a(if (long) 0.30f else 0.14f), Offset(w, y), Offset(w - (if (long) 12.dp.toPx() else 6.dp.toPx()), y), 1.dp.toPx())
            y += tick; idx++
        }
    }
}

// ───────────────────────── Oceano: raios de sol, ondas, cardume e bolhas ─────────────────────────
internal fun DrawScope.drawTide(colors: List<Color>, t: Float, level: Int) {
    val w = size.width; val h = size.height
    val sky = colors.at(0, Color(0xFF38BDF8)); val teal = colors.at(1, Color(0xFF2DD4BF)); val indigo = colors.at(2, Color(0xFF818CF8))
    // raios de sol atravessando a água
    val rays = lv(level, 3, 5, 8)
    for (i in 0 until rays) {
        val x0 = w * (0.05f + 0.9f * i / rays) + sin(t * 2 * TAU + i) * w * 0.03f
        val spread = w * (0.06f + 0.03f * (i % 3))
        val p = Path().apply {
            moveTo(x0 - spread * 0.3f, 0f); lineTo(x0 + spread * 0.3f, 0f)
            lineTo(x0 + spread * 2.2f + h * 0.18f, h * 0.8f); lineTo(x0 - spread * 2.2f + h * 0.18f, h * 0.8f); close()
        }
        val shimmer = 0.6f + 0.4f * sin(t * 3 * TAU + i * 1.3f)
        drawPath(p, Brush.verticalGradient(listOf(sky.a(0.15f * shimmer), Color.Transparent), 0f, h * 0.8f))
    }
    // ondas em camadas, cada uma numa velocidade
    val layers = lv(level, 2, 3, 4)
    val tints = listOf(sky, teal, indigo, sky)
    for (k in 0 until layers) {
        val base = h * (0.70f + 0.07f * k)
        val amp = h * (0.012f + 0.004f * k)
        val dir = if (k % 2 == 0) 1 else -1
        val path = Path().apply {
            moveTo(0f, h)
            for (i in 0..24) {
                val x = w * i / 24f
                lineTo(x, base + sin(x / w * (1.3f + 0.4f * k) * TAU + t * (k + 1) * dir * TAU) * amp)
            }
            lineTo(w, h); close()
        }
        drawPath(path, Brush.verticalGradient(listOf(tints[k].a(0.20f + 0.05f * k), Color.Transparent), base - amp, h))
    }
    // cardume cruzando
    val rnd = Random(5)
    repeat(lv(level, 0, 6, 12)) {
        val row = 0.22f + 0.42f * rnd.nextFloat(); val sp = 2 + rnd.nextInt(2); val ph = rnd.nextFloat()
        val dir = if (it % 2 == 0) 1f else -1f
        val x = -20.dp.toPx() + (w + 40.dp.toPx()) * frac(dir * t * sp + ph)
        val y = h * row + sin(t * 4 * TAU + ph * 9f) * h * 0.01f
        val fw = 14.dp.toPx(); val fh = 6.dp.toPx()
        drawOval(teal.a(0.32f), Offset(x - fw / 2, y - fh / 2), Size(fw, fh))
        val tail = Path().apply {
            moveTo(x - dir * fw * 0.4f, y)
            lineTo(x - dir * fw * 0.9f, y - fh * 0.8f)
            lineTo(x - dir * fw * 0.9f, y + fh * 0.8f); close()
        }
        drawPath(tail, teal.a(0.32f))
    }
    // bolhas subindo
    val r2 = Random(11)
    repeat(lv(level, 8, 14, 28)) {
        val bx = w * r2.nextFloat(); val sp = 1 + r2.nextInt(2); val ph = r2.nextFloat(); val rr = 2f + r2.nextFloat() * 4f
        val y = h * (1.05f - 1.1f * frac(t * sp + ph))
        val x = bx + sin(t * 3 * TAU + ph * 8f) * 6.dp.toPx()
        drawCircle(sky.a(0.28f), rr.dp.toPx(), Offset(x, y), style = Stroke(1.dp.toPx()))
        drawCircle(Color.White.a(0.30f), (rr * 0.25f).dp.toPx(), Offset(x - rr.dp.toPx() * 0.3f, y - rr.dp.toPx() * 0.3f))
    }
}

// ───────────────────────── Tinta: gotas de nanquim se espalhando ─────────────────────────
internal fun DrawScope.drawInk(colors: List<Color>, t: Float, level: Int) {
    val w = size.width; val h = size.height
    val ink = colors.at(0, Color(0xFF111111)); val ink2 = colors.at(1, Color(0xFF3B4A6B))
    val drops = lv(level, 2, 3, 5)
    for (i in 0 until drops) {
        val cyc = t * 8f + i.toFloat() / drops
        val p = frac(cyc)
        val rnd = Random((floor(cyc).toInt() % 8) * 31 + i)
        val cx = w * (0.15f + 0.7f * rnd.nextFloat()); val cy = h * (0.12f + 0.76f * rnd.nextFloat())
        val col = if (i % 2 == 0) ink else ink2
        val r = w * (0.04f + 0.20f * kotlin.math.sqrt(p)) * (0.8f + 0.4f * rnd.nextFloat())
        val fade = (1f - p) * (1f - p)
        // mancha: círculo central e satélites irregulares
        drawCircle(col.a(0.10f * fade + 0.02f), r, Offset(cx, cy))
        for (s in 0 until 7) {
            val ang = rnd.nextFloat() * TAU
            val d = r * (0.55f + 0.5f * rnd.nextFloat()); val rr = r * (0.28f + 0.3f * rnd.nextFloat())
            drawCircle(col.a(0.08f * fade + 0.01f), rr, Offset(cx + cos(ang) * d, cy + sin(ang) * d))
        }
        drawCircle(col.a(0.22f * fade), r * 1.35f, Offset(cx, cy), style = Stroke(1.2.dp.toPx()))
        // a gota caindo no começo
        if (p < 0.08f) drawCircle(col.a(0.5f), 3.dp.toPx(), Offset(cx, cy - h * 0.25f * (0.08f - p) / 0.08f))
    }
}

// ───────────────────────── Nórdico: fiorde com montanhas, névoa e sol baixo ─────────────────────────
internal fun DrawScope.drawFjord(colors: List<Color>, t: Float, level: Int) {
    val w = size.width; val h = size.height
    val pine = colors.at(0, Color(0xFF2F5D50)); val slate = colors.at(1, Color(0xFF6B7C85)); val sun = colors.at(2, Color(0xFFE8B96A))
    // sol baixo, sobe e desce devagar
    val sc = Offset(w * 0.72f, h * (0.30f - 0.03f * sin(t * TAU)))
    drawCircle(Brush.radialGradient(listOf(sun.a(0.35f), Color.Transparent), sc, w * 0.35f), w * 0.35f, sc)
    drawCircle(sun.a(0.42f), w * 0.07f, sc)
    // montanhas em três camadas
    for (k in 0 until 3) {
        val rnd = Random(30 + k)
        val base = h * (0.60f + 0.10f * k)
        val path = Path().apply {
            moveTo(0f, h); lineTo(0f, base)
            var x = 0f
            while (x < w) {
                x += w * (0.10f + 0.10f * rnd.nextFloat())
                lineTo(x.coerceAtMost(w), base - h * (0.05f + 0.13f * rnd.nextFloat()) * (1f - 0.25f * k))
                x += w * (0.08f + 0.08f * rnd.nextFloat())
                lineTo(x.coerceAtMost(w), base + h * 0.01f)
            }
            lineTo(w, h); close()
        }
        drawPath(path, (if (k == 0) slate else pine).a(0.09f + 0.06f * k))
    }
    // névoa à deriva
    repeat(lv(level, 2, 3, 5)) { i ->
        val y = h * (0.62f + 0.07f * i)
        val x = w * (frac(t * (1 + i % 2) + i * 0.3f) * 1.6f - 0.3f)
        drawOval(
            Brush.horizontalGradient(listOf(Color.Transparent, slate.a(0.14f), Color.Transparent), x - w * 0.5f, x + w * 0.5f),
            Offset(x - w * 0.5f, y - h * 0.02f), Size(w, h * 0.04f)
        )
    }
    // pássaros
    repeat(lv(level, 1, 3, 5)) { i ->
        val bx = w * (frac(t * (1 + i % 2) * 0.5f + i * 0.27f) * 1.3f - 0.15f)
        val by = h * (0.14f + 0.07f * i) + sin(t * 4 * TAU + i) * 4.dp.toPx()
        val flap = 3.dp.toPx() * sin(t * 30 * TAU + i)
        val s = 8.dp.toPx()
        drawLine(pine.a(0.40f), Offset(bx - s, by + flap), Offset(bx, by), 1.5.dp.toPx(), StrokeCap.Round)
        drawLine(pine.a(0.40f), Offset(bx, by), Offset(bx + s, by + flap), 1.5.dp.toPx(), StrokeCap.Round)
    }
}

// ───────────────────────── Obsidiana: vidro vulcânico com brilhos nas arestas ─────────────────────────
internal fun DrawScope.drawObsidian(colors: List<Color>, t: Float, level: Int) {
    val w = size.width; val h = size.height
    val shine = colors.at(0, Color.White)
    val cols = 5; val rows = 10
    val pts = Array(rows + 1) { r ->
        Array(cols + 1) { c ->
            val rnd = Random(r * 31 + c)
            val jx = if (c in 1 until cols) (rnd.nextFloat() - 0.5f) * 0.7f else 0f
            val jy = if (r in 1 until rows) (rnd.nextFloat() - 0.5f) * 0.7f else 0f
            Offset(w * (c + jx) / cols, h * (r + jy) / rows)
        }
    }
    val edges = ArrayList<Pair<Offset, Offset>>()
    for (r in 0 until rows) for (c in 0 until cols) {
        val a = pts[r][c]; val b = pts[r][c + 1]; val d = pts[r + 1][c]; val e = pts[r + 1][c + 1]
        val t1 = Path().apply { moveTo(a.x, a.y); lineTo(b.x, b.y); lineTo(d.x, d.y); close() }
        val t2 = Path().apply { moveTo(b.x, b.y); lineTo(e.x, e.y); lineTo(d.x, d.y); close() }
        drawPath(t1, shine.a(0.010f + 0.040f * (((r * 7 + c * 13) % 9) / 9f)))
        drawPath(t2, shine.a(0.010f + 0.040f * (((r * 5 + c * 11 + 3) % 9) / 9f)))
        edges += a to b; edges += a to d; edges += b to d
    }
    for ((p, q) in edges) drawLine(shine.a(0.05f), p, q, 1.dp.toPx())
    // brilhos percorrendo arestas, uma de cada vez
    for (k in 0 until lv(level, 2, 3, 5)) {
        val cyc = t * 8f + k * 0.25f
        val idx = ((floor(cyc).toInt() % 8) * 17 + k * 29) % edges.size
        val glow = sin(frac(cyc) * PI.toFloat()) * 0.75f
        val (p, q) = edges[idx]
        drawLine(shine.a(0.16f * glow), p, q, 7.dp.toPx(), StrokeCap.Round)
        drawLine(shine.a(glow), p, q, 1.6.dp.toPx(), StrokeCap.Round)
    }
    // reflexo diagonal passando
    val cx = -w * 0.4f + frac(t * 3f) * w * 1.8f
    drawRect(Brush.linearGradient(listOf(Color.Transparent, shine.a(0.05f), Color.Transparent), Offset(cx - w * 0.3f, 0f), Offset(cx + w * 0.3f, h * 0.5f)))
}

// ───────────────────────── Cobalto: favo de hexágonos com ondas de dados ─────────────────────────
internal fun DrawScope.drawHexNet(colors: List<Color>, t: Float, level: Int) {
    val w = size.width; val h = size.height
    val blue = colors.at(0, Color(0xFF4D86FF)); val cyan = colors.at(1, Color(0xFF7CE0FF))
    val r = 26.dp.toPx()
    val dx = r * 1.732f; val dy = r * 1.5f
    val maxR = maxOf(w, h) * 0.9f
    // ondas: de onde partem muda a cada ciclo de 10 s (a semente repete a cada 6 ciclos, então o loop fecha)
    val ripples = (0 until lv(level, 1, 1, 2)).map { j ->
        val cyc = t * 6f + j * 0.5f
        val rnd = Random((floor(cyc).toInt() % 6) * 7 + 1 + j * 40)
        Triple(Offset(w * rnd.nextFloat(), h * rnd.nextFloat()), frac(cyc) * maxR, frac(cyc))
    }
    var row = 0
    var y = 0f
    while (y < h + r) {
        var x = if (row % 2 == 0) 0f else dx / 2
        while (x < w + r) {
            var band = 0f; var fade = 0f
            for ((center, ring, p) in ripples) {
                val d = (Offset(x, y) - center).getDistance()
                val b = (1f - abs(d - ring) / (r * 3f)).coerceIn(0f, 1f)
                if (b > band) { band = b; fade = 1f - p }
            }
            val hex = Path().apply {
                for (k in 0 until 6) {
                    val ang = (60f * k - 30f) * PI.toFloat() / 180f
                    val px = x + cos(ang) * r * 0.94f; val py = y + sin(ang) * r * 0.94f
                    if (k == 0) moveTo(px, py) else lineTo(px, py)
                }
                close()
            }
            if (band > 0f) drawPath(hex, cyan.a(0.26f * band * fade))
            drawPath(hex, blue.a(0.07f + 0.36f * band * fade), style = Stroke(1.dp.toPx(), join = StrokeJoin.Round))
            if (band > 0.5f) drawCircle(cyan.a(0.6f * band * fade), 2.dp.toPx(), Offset(x, y))
            x += dx
        }
        y += dy; row++
    }
}

// ───────────────────────── Floresta: pinheiros em camadas, luar, névoa e vaga-lumes ─────────────────────────
private fun DrawScope.pine(x: Float, baseY: Float, hgt: Float, sway: Float, color: Color) {
    for (k in 0 until 3) {
        val topY = baseY - hgt + k * hgt * 0.28f
        val halfW = hgt * 0.22f * (1f + k * 0.5f)
        val botY = topY + hgt * 0.42f
        val tipX = x + sway * (1f - k * 0.35f)
        val p = Path().apply { moveTo(tipX, topY); lineTo(x - halfW, botY); lineTo(x + halfW, botY); close() }
        drawPath(p, color)
    }
}

internal fun DrawScope.drawForest(colors: List<Color>, t: Float, level: Int) {
    val w = size.width; val h = size.height
    val glow = colors.at(0, Color(0xFFD9F99D)); val leaf = colors.at(1, Color(0xFF2FBF7A))
    // lua e halo
    val moon = Offset(w * 0.80f, h * 0.12f)
    drawCircle(Brush.radialGradient(listOf(glow.a(0.22f), Color.Transparent), moon, w * 0.4f), w * 0.4f, moon)
    drawCircle(glow.a(0.40f), w * 0.045f, moon)
    // raios de luar entre as copas
    for (i in 0 until lv(level, 2, 3, 5)) {
        val x0 = w * (0.45f + 0.12f * i)
        val sh = 0.6f + 0.4f * sin(t * 2 * TAU + i * 1.7f)
        val p = Path().apply { moveTo(x0, 0f); lineTo(x0 + w * 0.05f, 0f); lineTo(x0 - w * 0.20f + w * 0.05f, h * 0.7f); lineTo(x0 - w * 0.28f, h * 0.7f); close() }
        drawPath(p, Brush.verticalGradient(listOf(glow.a(0.07f * sh), Color.Transparent), 0f, h * 0.7f))
    }
    // pinheiros em 3 profundidades
    for (layer in 0 until 3) {
        val rnd = Random(60 + layer)
        val count = 7 - layer * 2
        val baseY = h * (0.80f + 0.07f * layer)
        repeat(count) { i ->
            val x = w * (i + 0.5f + (rnd.nextFloat() - 0.5f) * 0.5f) / count
            val hgt = h * (0.10f + 0.06f * layer) * (0.8f + 0.5f * rnd.nextFloat())
            val sway = sin(t * 2 * TAU + i + layer) * (2 + layer).dp.toPx()
            pine(x, baseY + hgt * 0.1f, hgt, sway, leaf.a(0.07f + 0.07f * layer))
        }
    }
    // névoa rasteira
    repeat(2) { i ->
        val x = w * (frac(t * (1 + i) + i * 0.4f) * 1.6f - 0.3f)
        drawOval(
            Brush.horizontalGradient(listOf(Color.Transparent, glow.a(0.07f), Color.Transparent), x - w * 0.5f, x + w * 0.5f),
            Offset(x - w * 0.5f, h * (0.78f + 0.06f * i)), Size(w, h * 0.05f)
        )
    }
    // vaga-lumes
    val rnd = Random(77)
    repeat(lv(level, 10, 18, 34)) {
        val bx = rnd.nextFloat(); val by = rnd.nextFloat(); val ph = rnd.nextFloat() * TAU; val k = 1 + rnd.nextInt(3)
        val x = w * (bx + 0.04f * sin(t * k * TAU + ph)); val y = h * (0.35f + 0.6f * by + 0.03f * cos(t * (k + 1) * TAU + ph))
        val blink = (0.5f + 0.5f * sin(t * (3 + it % 4) * TAU + ph)).let { b -> b * b }
        drawCircle(glow.a(0.20f * blink), 9.dp.toPx(), Offset(x, y))
        drawCircle(glow.a(0.35f + 0.6f * blink), 2.dp.toPx(), Offset(x, y))
    }
}
