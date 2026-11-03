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
 * Quarta leva dos temas "Loucos" (28/10/2026, madrugada). Mesmo contrato de ThemeDecor.kt:
 * [t] vai de 0 a 1 em 60 s e toda animação usa múltiplos inteiros de t (o loop fecha sem salto).
 */

private const val TAU = (2 * PI).toFloat()

private fun frac(x: Float) = x - kotlin.math.floor(x)

// ───────────────────────── Radar ─────────────────────────
internal fun DrawScope.drawRadar(colors: List<Color>, t: Float) {
    val line = colors.getOrElse(0) { Color(0xFF3DFF8A) }
    val blip = colors.getOrElse(1) { Color(0xFFFFE14D) }
    val w = size.width; val h = size.height
    val c = Offset(w * 0.5f, h * 0.58f)
    val r = maxOf(w, h) * 0.62f
    // anéis e cruz
    for (i in 1..6) drawCircle(line.copy(alpha = 0.16f), r * i / 6f, c, style = Stroke(1.dp.toPx()))
    drawLine(line.copy(alpha = 0.14f), Offset(c.x - r, c.y), Offset(c.x + r, c.y), 1.dp.toPx())
    drawLine(line.copy(alpha = 0.14f), Offset(c.x, c.y - r), Offset(c.x, c.y + r), 1.dp.toPx())
    // varredura: 12 voltas por minuto, com rastro em leque
    val sweep = frac(t * 12f) * 360f
    rotate(sweep, c) {
        for (k in 0 until 24) {
            val a = -k * 2.5f
            val rad = a * PI.toFloat() / 180f
            drawLine(
                line.copy(alpha = 0.32f * (1f - k / 24f)), c,
                Offset(c.x + cos(rad) * r, c.y + sin(rad) * r), 3.dp.toPx()
            )
        }
    }
    // alvos: acendem quando o feixe passa e apagam devagar
    val rnd = Random(77)
    repeat(9) {
        val ang = rnd.nextFloat() * 360f
        val dist = (0.15f + rnd.nextFloat() * 0.8f) * r
        val drift = sin(t * (1 + it % 3) * TAU) * 6f
        val rad = (ang + drift) * PI.toFloat() / 180f
        val p = Offset(c.x + cos(rad) * dist, c.y + sin(rad) * dist)
        val since = frac((sweep - ang - drift) / 360f) // 0 = feixe acabou de passar
        val glow = (1f - since).let { g -> g * g }
        drawCircle(blip.copy(alpha = 0.15f * glow), 12.dp.toPx(), p)
        drawCircle(blip.copy(alpha = 0.25f + 0.7f * glow), 3.dp.toPx(), p)
    }
}

// ───────────────────────── Sakura ─────────────────────────
internal fun DrawScope.drawSakura(colors: List<Color>, t: Float) {
    val petal = colors.getOrElse(0) { Color(0xFFFF9EC4) }
    val petal2 = colors.getOrElse(1) { Color(0xFFFFD1E3) }
    val branch = colors.getOrElse(2) { Color(0xFF5B3A3A) }
    val w = size.width; val h = size.height
    // galho no canto de cima, à direita
    val b = Path().apply {
        moveTo(w * 1.05f, h * 0.02f)
        cubicTo(w * 0.85f, h * 0.06f, w * 0.7f, h * 0.05f, w * 0.52f, h * 0.12f)
        moveTo(w * 0.8f, h * 0.055f)
        cubicTo(w * 0.74f, h * 0.12f, w * 0.7f, h * 0.16f, w * 0.62f, h * 0.2f)
    }
    drawPath(b, branch.copy(alpha = 0.55f), style = Stroke(5.dp.toPx(), cap = StrokeCap.Round))
    val flowers = Random(5)
    repeat(14) {
        val p = Offset(w * (0.52f + flowers.nextFloat() * 0.5f), h * (0.02f + flowers.nextFloat() * 0.17f))
        val s = (4 + flowers.nextInt(4)).dp.toPx()
        for (k in 0 until 5) {
            val a = k * TAU / 5
            drawCircle(petal.copy(alpha = 0.55f), s * 0.6f, Offset(p.x + cos(a) * s * 0.6f, p.y + sin(a) * s * 0.6f))
        }
        drawCircle(Color.White.copy(alpha = 0.6f), s * 0.3f, p)
    }
    // pétalas caindo em zigue-zague, girando
    val rnd = Random(23)
    repeat(34) { i ->
        val x0 = rnd.nextFloat() * w
        val speed = 1 + rnd.nextInt(3)
        val y = frac(t * speed + rnd.nextFloat()) * (h + 40f) - 20f
        val sway = sin((t * (4 + i % 3) + rnd.nextFloat()) * TAU) * 22.dp.toPx()
        val sz = (4 + rnd.nextInt(5)).dp.toPx()
        val spin = frac(t * (6 + i % 4) + i * 0.1f) * 360f
        val color = if (i % 3 == 0) petal2 else petal
        rotate(spin, Offset(x0 + sway, y)) {
            drawOval(color.copy(alpha = 0.75f), Offset(x0 + sway - sz, y - sz * 0.5f), Size(sz * 2f, sz))
        }
    }
}

// ───────────────────────── Vulcão ─────────────────────────
internal fun DrawScope.drawVolcano(colors: List<Color>, t: Float) {
    val lava = colors.getOrElse(0) { Color(0xFFFF5A1F) }
    val hot = colors.getOrElse(1) { Color(0xFFFFD23F) }
    val rock = colors.getOrElse(2) { Color(0xFF1A0E0B) }
    val w = size.width; val h = size.height
    val top = Offset(w * 0.5f, h * 0.72f)
    // brilho no céu
    drawCircle(Brush.radialGradient(listOf(lava.copy(alpha = 0.35f), Color.Transparent), top, w * 0.7f), w * 0.7f, top)
    // fumaça subindo
    val smoke = Random(4)
    repeat(10) { i ->
        val p = frac(t * 3 + i / 10f)
        val x = top.x + sin((p * 2 + i) * TAU / 2) * w * 0.08f + (smoke.nextFloat() - 0.5f) * w * 0.1f
        val y = top.y - p * h * 0.75f
        drawCircle(Color.Gray.copy(alpha = 0.12f * (1f - p)), (20 + p * 60).dp.toPx(), Offset(x, y))
    }
    // jatos de lava: partículas em parábola
    val rnd = Random(61)
    repeat(40) { i ->
        val life = frac(t * (5 + i % 4) + rnd.nextFloat())
        val vx = (rnd.nextFloat() - 0.5f) * w * 0.9f
        val vy = (0.55f + rnd.nextFloat() * 0.5f) * h
        val x = top.x + vx * life
        val y = top.y - vy * life + 1.1f * h * life * life
        if (y < h) {
            val c = if (life < 0.4f) hot else lava
            drawCircle(c.copy(alpha = 0.9f * (1f - life)), (2.5f + 2f * (1f - life)).dp.toPx(), Offset(x, y))
        }
    }
    // a montanha e o rio de lava descendo
    val mountain = Path().apply {
        moveTo(-10f, h)
        lineTo(w * 0.38f, top.y)
        lineTo(w * 0.62f, top.y)
        lineTo(w + 10f, h)
        close()
    }
    drawPath(mountain, rock)
    val flow = Path().apply {
        moveTo(w * 0.47f, top.y)
        cubicTo(w * 0.44f, h * 0.8f, w * 0.56f, h * 0.88f, w * 0.5f, h)
    }
    drawPath(flow, lava.copy(alpha = 0.85f), style = Stroke(6.dp.toPx(), cap = StrokeCap.Round))
    val pulse = 0.5f + 0.5f * sin(t * 20 * TAU)
    drawPath(flow, hot.copy(alpha = 0.25f + 0.35f * pulse), style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
    drawLine(hot.copy(alpha = 0.8f), Offset(w * 0.38f, top.y), Offset(w * 0.62f, top.y), 3.dp.toPx(), cap = StrokeCap.Round)
}

// ───────────────────────── DNA ─────────────────────────
internal fun DrawScope.drawDna(colors: List<Color>, t: Float) {
    val a = colors.getOrElse(0) { Color(0xFF4DD8FF) }
    val b = colors.getOrElse(1) { Color(0xFFFF5FD2) }
    val rung = colors.getOrElse(2) { Color(0xFFB8FF5C) }
    val w = size.width; val h = size.height
    // duas hélices: uma grande à direita, uma pequena à esquerda
    listOf(Triple(w * 0.8f, 46.dp.toPx(), 1f), Triple(w * 0.14f, 26.dp.toPx(), 0.55f)).forEachIndexed { idx, (cx, amp, alpha) ->
        val step = 14.dp.toPx()
        val phase = t * (8 + idx * 4) * TAU
        var y = -step
        var i = 0
        while (y < h + step) {
            val ang = y / (90.dp.toPx()) * TAU / 2 + phase
            val x1 = cx + sin(ang) * amp
            val x2 = cx - sin(ang) * amp
            val depth = cos(ang) // frente/trás
            if (i % 2 == 0) drawLine(rung.copy(alpha = 0.22f * alpha), Offset(x1, y), Offset(x2, y), 2.dp.toPx())
            drawCircle(a.copy(alpha = (0.35f + 0.45f * (depth + 1) / 2) * alpha), (3f + 1.5f * depth).dp.toPx(), Offset(x1, y))
            drawCircle(b.copy(alpha = (0.35f + 0.45f * (1 - depth) / 2) * alpha), (3f - 1.5f * depth).dp.toPx(), Offset(x2, y))
            y += step; i++
        }
    }
    // células flutuando
    val rnd = Random(8)
    repeat(12) {
        val x = rnd.nextFloat() * w
        val y = frac(rnd.nextFloat() - t * (1 + it % 2)) * h
        drawCircle(rung.copy(alpha = 0.10f), (6 + rnd.nextInt(10)).dp.toPx(), Offset(x, y), style = Stroke(1.5f.dp.toPx()))
    }
}

// ───────────────────────── Vinil ─────────────────────────
internal fun DrawScope.drawVinyl(colors: List<Color>, t: Float) {
    val label = colors.getOrElse(0) { Color(0xFFFF4D4D) }
    val shine = colors.getOrElse(1) { Color(0xFFFFFFFF) }
    val bars = colors.getOrElse(2) { Color(0xFFFFC23D) }
    val w = size.width; val h = size.height
    val r = w * 0.62f
    val c = Offset(w * 0.98f, h * 0.2f)
    drawCircle(Color(0xFF0B0B0B), r, c)
    var gr = r * 0.38f
    while (gr < r * 0.98f) {
        drawCircle(Color.White.copy(alpha = 0.05f), gr, c, style = Stroke(1f))
        gr += 3.dp.toPx()
    }
    // reflexo girando (33 rpm seria rápido demais; 6 voltas por minuto)
    rotate(frac(t * 6) * 360f, c) {
        drawArc(shine.copy(alpha = 0.10f), -20f, 40f, true, Offset(c.x - r, c.y - r), Size(r * 2, r * 2))
        drawArc(shine.copy(alpha = 0.10f), 160f, 40f, true, Offset(c.x - r, c.y - r), Size(r * 2, r * 2))
    }
    drawCircle(label, r * 0.34f, c)
    drawCircle(Color.Black.copy(alpha = 0.6f), r * 0.04f, c)
    // equalizador no rodapé
    val n = 18
    val bw = w / n
    for (i in 0 until n) {
        val v = 0.25f + 0.75f * abs(sin((t * (20 + i % 5 * 4) + i * 0.37f) * TAU))
        val bh = v * h * 0.14f
        drawRect(bars.copy(alpha = 0.22f), Offset(i * bw + bw * 0.15f, h - bh), Size(bw * 0.7f, bh))
        drawRect(bars.copy(alpha = 0.6f), Offset(i * bw + bw * 0.15f, h - bh), Size(bw * 0.7f, 3.dp.toPx()))
    }
}

// ───────────────────────── Masmorra ─────────────────────────
internal fun DrawScope.drawDungeon(colors: List<Color>, t: Float) {
    val fire = colors.getOrElse(0) { Color(0xFFFF8A1F) }
    val core = colors.getOrElse(1) { Color(0xFFFFE07A) }
    val brick = colors.getOrElse(2) { Color(0xFF3A3047) }
    val w = size.width; val h = size.height
    // tijolos
    val bh = 22.dp.toPx(); val bw = 48.dp.toPx()
    var row = 0
    var y = 0f
    while (y < h) {
        var x = if (row % 2 == 0) 0f else -bw / 2
        while (x < w) {
            drawRect(brick.copy(alpha = 0.35f), Offset(x + 1.5f, y + 1.5f), Size(bw - 3f, bh - 3f), style = Stroke(1.5f))
            x += bw
        }
        y += bh; row++
    }
    // duas tochas com chama tremendo e luz pulsando na parede
    listOf(Offset(w * 0.1f, h * 0.32f), Offset(w * 0.9f, h * 0.32f)).forEachIndexed { i, p ->
        val flicker = 0.75f + 0.25f * sin((t * (37 + i * 5)) * TAU) * sin((t * 23) * TAU + i)
        drawCircle(Brush.radialGradient(listOf(fire.copy(alpha = 0.32f * flicker), Color.Transparent), p, w * 0.45f), w * 0.45f, p)
        drawRect(Color(0xFF2A1A10), Offset(p.x - 4.dp.toPx(), p.y), Size(8.dp.toPx(), 30.dp.toPx()))
        val fh = 22.dp.toPx() * flicker
        val flame = Path().apply {
            moveTo(p.x - 8.dp.toPx(), p.y)
            quadraticTo(p.x - 9.dp.toPx(), p.y - fh * 0.6f, p.x + sin(t * 41 * TAU) * 3.dp.toPx(), p.y - fh)
            quadraticTo(p.x + 9.dp.toPx(), p.y - fh * 0.6f, p.x + 8.dp.toPx(), p.y)
            close()
        }
        drawPath(flame, fire.copy(alpha = 0.9f))
        drawCircle(core.copy(alpha = 0.85f), 4.dp.toPx() * flicker, Offset(p.x, p.y - 5.dp.toPx()))
        // faíscas
        val rnd = Random(30 + i)
        repeat(8) {
            val life = frac(t * (9 + it % 4) + rnd.nextFloat())
            val sx = p.x + (rnd.nextFloat() - 0.5f) * 30.dp.toPx() * life
            drawCircle(core.copy(alpha = 0.8f * (1 - life)), 1.5f.dp.toPx(), Offset(sx, p.y - fh - life * 70.dp.toPx()))
        }
    }
    // escuridão nas bordas de baixo
    drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f)), startY = h * 0.6f))
}
