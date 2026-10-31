package com.vitoroliveira.gymtracker.ui.theme

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
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
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/*
 * Segunda parte dos cenários dos temas antigos (31/10/2026): Papel, Blueprint, Brutal, Neon, Terminal, Arena, Ouro,
 * Aurora, Synthwave, Confete, Magma e Ártico. Mesmo contrato de ThemeDecorClassic.kt (t em 0..1 por minuto, só
 * múltiplos inteiros; [level] 0 = Calmo, 1 = Normal, 2 = Insano).
 */

private const val TAU = (2 * PI).toFloat()

private fun frac(x: Float) = x - floor(x)
private fun lv(level: Int, calm: Int, normal: Int, wild: Int) = when (level) { 0 -> calm; 1 -> normal; else -> wild }
private fun Color.a(alpha: Float) = copy(alpha = alpha)
private fun List<Color>.at(i: Int, fallback: Color) = getOrElse(i) { fallback }

/** Desenha só o começo (de 0 a 1) de uma linha quebrada, como se estivesse sendo traçada à mão. */
private fun DrawScope.drawPolyline(pts: List<Offset>, reveal: Float, color: Color, stroke: Float) {
    if (pts.size < 2 || reveal <= 0f) return
    var total = 0f
    for (i in 1 until pts.size) total += (pts[i] - pts[i - 1]).getDistance()
    var left = total * reveal
    for (i in 1 until pts.size) {
        val a = pts[i - 1]; val b = pts[i]
        val d = (b - a).getDistance()
        if (d <= 0f) continue
        if (left >= d) {
            drawLine(color, a, b, stroke, StrokeCap.Round); left -= d
        } else {
            drawLine(color, a, a + (b - a) * (left / d), stroke, StrokeCap.Round); break
        }
    }
}

// ───────────────────────── Papel: caderno com rabiscos que se desenham sozinhos ─────────────────────────
private fun doodle(kind: Int, c: Offset, s: Float): List<Offset> = when (kind) {
    0 -> (0..10).map { k -> // estrela de 5 pontas, fechada
        val a = (-90f + k * 144f) * PI.toFloat() / 180f
        Offset(c.x + cos(a) * s, c.y + sin(a) * s)
    }
    1 -> (0..40).map { k -> // espiral
        val a = k * 0.32f; val r = s * k / 40f
        Offset(c.x + cos(a) * r, c.y + sin(a) * r)
    }
    2 -> listOf( // halter
        Offset(c.x - s, c.y - s * .5f), Offset(c.x - s, c.y + s * .5f), Offset(c.x - s * .7f, c.y + s * .5f),
        Offset(c.x - s * .7f, c.y + s * .15f), Offset(c.x + s * .7f, c.y + s * .15f), Offset(c.x + s * .7f, c.y + s * .5f),
        Offset(c.x + s, c.y + s * .5f), Offset(c.x + s, c.y - s * .5f), Offset(c.x + s * .7f, c.y - s * .5f),
        Offset(c.x + s * .7f, c.y - s * .15f), Offset(c.x - s * .7f, c.y - s * .15f), Offset(c.x - s * .7f, c.y - s * .5f),
        Offset(c.x - s, c.y - s * .5f)
    )
    else -> listOf(Offset(c.x - s, c.y), Offset(c.x - s * .3f, c.y + s * .7f), Offset(c.x + s, c.y - s * .8f)) // visto
}

internal fun DrawScope.drawNotebook(colors: List<Color>, t: Float, level: Int) {
    val w = size.width; val h = size.height
    val rule = colors.at(0, Color(0xFF5B8DEF)); val margin = colors.at(1, Color(0xFFE0524D)); val pencil = colors.at(2, Color(0xFF3A3A3A))
    val gap = 28.dp.toPx()
    var y = gap * 2
    while (y < h) { drawLine(rule.a(0.16f), Offset(0f, y), Offset(w, y), 1.dp.toPx()); y += gap }
    drawLine(margin.a(0.30f), Offset(w * 0.13f, 0f), Offset(w * 0.13f, h), 1.5.dp.toPx())
    for (k in 0 until 3) drawCircle(pencil.a(0.10f), 7.dp.toPx(), Offset(w * 0.045f, h * (0.2f + 0.3f * k)))
    val n = lv(level, 2, 4, 6)
    for (i in 0 until n) {
        val p = frac(t * 6f + i / n.toFloat())
        val reveal = (p / 0.45f).coerceIn(0f, 1f)
        val alpha = if (p < 0.8f) 0.55f else 0.55f * (1f - (p - 0.8f) / 0.2f)
        val c = Offset(w * (0.32f + 0.5f * frac(0.41f * i + 0.2f)), h * (0.14f + 0.72f * frac(0.57f * i + 0.31f)))
        drawPolyline(doodle(i % 4, c, w * 0.11f), reveal, (if (i % 2 == 0) pencil else margin).a(alpha), 2.dp.toPx())
    }
}

// ───────────────────────── Blueprint: prancheta com compasso, engrenagem e cota ─────────────────────────
internal fun DrawScope.drawDraft(colors: List<Color>, t: Float, level: Int) {
    val w = size.width; val h = size.height
    val line = colors.at(0, Color(0xFFBFD9FF)); val mark = colors.at(1, Color(0xFFFFD400))
    val g = 16.dp.toPx()
    var x = 0f; var i = 0
    while (x <= w) { drawLine(line.a(if (i % 5 == 0) 0.16f else 0.06f), Offset(x, 0f), Offset(x, h), 1.dp.toPx()); x += g; i++ }
    var y = 0f; i = 0
    while (y <= h) { drawLine(line.a(if (i % 5 == 0) 0.16f else 0.06f), Offset(0f, y), Offset(w, y), 1.dp.toPx()); y += g; i++ }
    // compasso traçando um círculo
    val cc = Offset(w * 0.70f, h * 0.30f); val cr = w * 0.20f
    val sweep = frac(t * 4f) * 360f
    drawCircle(line.a(0.07f), cr, cc, style = Stroke(1.dp.toPx()))
    drawArc(mark.a(0.7f), -90f, sweep, false, Offset(cc.x - cr, cc.y - cr), Size(cr * 2, cr * 2), style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
    val rad = (sweep - 90f) * PI.toFloat() / 180f
    val tip = Offset(cc.x + cos(rad) * cr, cc.y + sin(rad) * cr)
    drawLine(mark.a(0.5f), cc, tip, 1.5.dp.toPx()); drawCircle(mark.a(0.8f), 4.dp.toPx(), cc); drawCircle(mark.a(0.9f), 3.dp.toPx(), tip)
    // engrenagens
    for (gi in 0 until lv(level, 1, 1, 2)) {
        val gc = if (gi == 0) Offset(w * 0.22f, h * 0.72f) else Offset(w * 0.22f + w * 0.2f, h * 0.72f + h * 0.04f)
        val gr = w * (if (gi == 0) 0.13f else 0.10f); val teeth = if (gi == 0) 10 else 8
        val gear = Path().apply {
            for (k in 0 until teeth * 2) {
                val ang = k * PI.toFloat() / teeth; val r = if (k % 2 == 0) gr * 1.18f else gr
                val px = gc.x + cos(ang) * r; val py = gc.y + sin(ang) * r
                if (k == 0) moveTo(px, py) else lineTo(px, py)
            }
            close()
        }
        rotate(t * 360f * 2f * (if (gi == 0) 1f else -1f), gc) {
            drawPath(gear, line.a(0.30f), style = Stroke(1.5.dp.toPx(), join = StrokeJoin.Round))
            drawCircle(line.a(0.30f), gr * 0.35f, gc, style = Stroke(1.5.dp.toPx()))
        }
    }
    // cota com marcador passeando
    val cy = h * 0.92f; val x0 = w * 0.1f; val x1 = w * 0.9f
    drawLine(mark.a(0.45f), Offset(x0, cy), Offset(x1, cy), 1.dp.toPx())
    for (e in listOf(x0, x1)) drawLine(mark.a(0.6f), Offset(e, cy - 8.dp.toPx()), Offset(e, cy + 8.dp.toPx()), 1.5.dp.toPx())
    val mx = x0 + (x1 - x0) * (0.5f + 0.5f * sin(t * 2 * TAU))
    drawLine(mark.a(0.9f), Offset(mx, cy - 6.dp.toPx()), Offset(mx, cy + 6.dp.toPx()), 2.dp.toPx())
    if (level >= 1) {
        val vx = w * 0.06f
        drawLine(mark.a(0.35f), Offset(vx, h * 0.15f), Offset(vx, h * 0.55f), 1.dp.toPx())
        for (e in listOf(h * 0.15f, h * 0.55f)) drawLine(mark.a(0.5f), Offset(vx - 6.dp.toPx(), e), Offset(vx + 6.dp.toPx(), e), 1.5.dp.toPx())
    }
}

// ───────────────────────── Brutal: blocos duros, sombra seca e fita de perigo ─────────────────────────
internal fun DrawScope.drawBrutal(colors: List<Color>, t: Float, level: Int) {
    val w = size.width; val h = size.height
    val pal = listOf(colors.at(0, Color(0xFFFF4D8D)), colors.at(1, Color(0xFFFFD93D)), colors.at(2, Color(0xFF4D9DFF)))
    val ink = colors.at(3, Color(0xFF111111))
    val rnd = Random(9)
    for (idx in 0 until lv(level, 3, 5, 8)) {
        val col = pal[idx % 3]
        val bx = w * (0.1f + 0.8f * rnd.nextFloat()); val by = h * (0.08f + 0.80f * rnd.nextFloat())
        val sz = w * (0.12f + 0.12f * rnd.nextFloat())
        val c = Offset(bx + sin(t * (1 + idx % 3) * TAU + idx) * w * 0.05f, by + cos(t * (1 + idx % 2) * TAU + idx * 2) * h * 0.02f)
        val spin = if (idx % 3 == 0) t * 360f * (if (idx % 2 == 0) 1f else -1f) else idx * 17f
        val sh = 6.dp.toPx()
        rotate(spin, c) {
            when (idx % 3) {
                0 -> {
                    drawRect(ink.a(0.16f), Offset(c.x - sz / 2 + sh, c.y - sz / 2 + sh), Size(sz, sz))
                    drawRect(col.a(0.38f), Offset(c.x - sz / 2, c.y - sz / 2), Size(sz, sz))
                    drawRect(ink.a(0.55f), Offset(c.x - sz / 2, c.y - sz / 2), Size(sz, sz), style = Stroke(3.dp.toPx()))
                }
                1 -> {
                    drawCircle(ink.a(0.16f), sz / 2, Offset(c.x + sh, c.y + sh))
                    drawCircle(col.a(0.38f), sz / 2, c)
                    drawCircle(ink.a(0.55f), sz / 2, c, style = Stroke(3.dp.toPx()))
                }
                else -> {
                    val tri = { dx: Float, dy: Float ->
                        Path().apply {
                            moveTo(c.x + dx, c.y - sz / 2 + dy); lineTo(c.x + sz / 2 + dx, c.y + sz / 2 + dy); lineTo(c.x - sz / 2 + dx, c.y + sz / 2 + dy); close()
                        }
                    }
                    drawPath(tri(sh, sh), ink.a(0.16f))
                    drawPath(tri(0f, 0f), col.a(0.38f))
                    drawPath(tri(0f, 0f), ink.a(0.55f), style = Stroke(3.dp.toPx(), join = StrokeJoin.Miter))
                }
            }
        }
    }
    // fita de perigo correndo na base
    val bandH = 26.dp.toPx(); val y0 = h * 0.94f
    drawRect(pal[1].a(0.45f), Offset(0f, y0), Size(w, bandH))
    val stripe = 36.dp.toPx()
    var sx = -bandH + frac(t * 20f) * stripe
    while (sx < w) {
        val p = Path().apply {
            moveTo(sx, y0 + bandH); lineTo(sx + 18.dp.toPx(), y0 + bandH); lineTo(sx + 18.dp.toPx() + bandH, y0); lineTo(sx + bandH, y0); close()
        }
        drawPath(p, ink.a(0.50f))
        sx += stripe
    }
}

// ───────────────────────── Neon: tubos de néon piscando ─────────────────────────
internal fun DrawScope.drawNeonSign(colors: List<Color>, t: Float, level: Int) {
    val w = size.width; val h = size.height
    val pink = colors.at(0, Color(0xFFFF2BD6)); val cyan = colors.at(1, Color(0xFF00F0FF))
    fun flick(i: Int): Float {
        val dip = if (sin(t * 13f * TAU + i * 2.7f) > 0.94f) 0.3f else 1f
        return dip * (0.88f + 0.12f * sin(t * 2f * TAU + i))
    }
    fun tube(path: Path, c: Color, f: Float) {
        for ((wd, al) in listOf(14f to 0.06f, 8f to 0.12f, 4f to 0.26f, 1.6f to 0.95f)) {
            drawPath(path, c.a(al * f), style = Stroke(wd.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }
    fun poly(vararg pts: Offset, closed: Boolean = false) = Path().apply {
        pts.forEachIndexed { i, p -> if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y) }
        if (closed) close()
    }
    val bob = sin(t * TAU) * 4.dp.toPx()
    // anel
    tube(Path().apply { addOval(Rect(Offset(w * 0.78f, h * 0.17f + bob), w * 0.10f)) }, pink, flick(0))
    // raio
    val bx = w * 0.20f; val by = h * 0.26f - bob
    tube(poly(Offset(bx + 18.dp.toPx(), by - 40.dp.toPx()), Offset(bx - 8.dp.toPx(), by + 4.dp.toPx()), Offset(bx + 10.dp.toPx(), by + 4.dp.toPx()), Offset(bx - 12.dp.toPx(), by + 44.dp.toPx())), cyan, flick(1))
    // triângulo
    val tx = w * 0.80f; val ty = h * 0.60f + bob
    tube(poly(Offset(tx, ty - 36.dp.toPx()), Offset(tx + 34.dp.toPx(), ty + 26.dp.toPx()), Offset(tx - 34.dp.toPx(), ty + 26.dp.toPx()), closed = true), cyan, flick(2))
    // batimento no rodapé
    val zy = h * 0.88f
    tube(poly(Offset(w * 0.08f, zy), Offset(w * 0.30f, zy), Offset(w * 0.36f, zy - 26.dp.toPx()), Offset(w * 0.42f, zy + 22.dp.toPx()), Offset(w * 0.48f, zy), Offset(w * 0.92f, zy)), pink, flick(3))
    if (level >= 1) tube(Path().apply { addOval(Rect(Offset(w * 0.25f, h * 0.76f - bob), w * 0.07f)) }, cyan, flick(4))
    if (level >= 2) {
        val sx = w * 0.55f; val sy = h * 0.48f
        val star = (0..10).map { k -> val a = (-90f + k * 144f) * PI.toFloat() / 180f; Offset(sx + cos(a) * 30.dp.toPx(), sy + sin(a) * 30.dp.toPx()) }
        tube(poly(*star.toTypedArray()), pink, flick(5))
    }
}

// ───────────────────────── Terminal: log rolando, cursor e tubo de raios catódicos ─────────────────────────
internal fun DrawScope.drawTerminal(colors: List<Color>, t: Float, level: Int) {
    val w = size.width; val h = size.height
    val g = colors.at(0, Color(0xFF33FF66))
    val lineH = 14.dp.toPx()
    val rows = (h / lineH).toInt() + 2
    val tick = floor(t * 60f).toInt()
    val off = frac(t * 60f) * lineH
    for (r in 0 until rows) {
        val seed = (r + tick) % 60
        val rnd = Random(seed * 13 + 5)
        val y = h - (r + 1) * lineH + off
        var x = w * 0.05f + (seed % 3) * 8.dp.toPx()
        val prompt = seed % 9 == 0
        val hot = seed % 7 == 0 || (level >= 2 && seed % 4 == 0)
        if (prompt) { drawRect(g.a(0.55f), Offset(w * 0.05f - 10.dp.toPx(), y + 3.dp.toPx()), Size(5.dp.toPx(), 7.dp.toPx())) }
        val tokens = 2 + rnd.nextInt(4)
        for (k in 0 until tokens) {
            val tw = (2 + rnd.nextInt(8)) * 6.dp.toPx()
            if (x + tw > w * 0.92f) break
            drawRect(g.a(if (hot && k == 0) 0.5f else 0.09f + 0.14f * rnd.nextFloat()), Offset(x, y + 4.dp.toPx()), Size(tw, 4.dp.toPx()))
            x += tw + 6.dp.toPx()
        }
    }
    // cursor piscando (1 Hz)
    if (sin(t * 60f * TAU) > 0f) drawRect(g.a(0.75f), Offset(w * 0.05f, h - 20.dp.toPx()), Size(8.dp.toPx(), 14.dp.toPx()))
    // linhas de varredura e faixa de refresh descendo
    var sy = 0f
    while (sy < h) { drawLine(g.a(0.045f), Offset(0f, sy), Offset(w, sy), 1.dp.toPx()); sy += 4.dp.toPx() }
    val by = frac(t * 3f) * h * 1.4f - h * 0.2f
    drawRect(Brush.verticalGradient(listOf(Color.Transparent, g.a(0.07f), Color.Transparent), by - h * 0.08f, by + h * 0.08f), Offset(0f, by - h * 0.08f), Size(w, h * 0.16f))
    // vinheta de tubo
    drawRect(Brush.radialGradient(listOf(Color.Transparent, Color.Black.a(0.45f)), Offset(w / 2, h / 2), max(w, h) * 0.75f))
}

// ───────────────────────── Arena: pista de atletismo com corredores e refletores ─────────────────────────
internal fun DrawScope.drawTrack(colors: List<Color>, t: Float, level: Int) {
    val w = size.width; val h = size.height
    val red = colors.at(0, Color(0xFFFF2D2D))
    // refletores varrendo dos cantos de cima
    for (side in 0..1) {
        val ox = if (side == 0) w * 0.02f else w * 0.98f
        val ang = (if (side == 0) 0.35f else -0.35f) + sin(t * 2 * TAU + side * 2f) * 0.12f
        val len = h * 1.1f
        val cone = Path().apply {
            moveTo(ox, 0f)
            lineTo(ox + sin(ang - 0.12f) * len, cos(ang - 0.12f) * len)
            lineTo(ox + sin(ang + 0.12f) * len, cos(ang + 0.12f) * len); close()
        }
        drawPath(cone, Brush.linearGradient(listOf(Color.White.a(0.11f), Color.Transparent), Offset(ox, 0f), Offset(ox + sin(ang) * len, cos(ang) * len)))
    }
    val lanes = lv(level, 4, 5, 6)
    val c = Offset(w * 0.5f, h * 0.60f)
    for (k in 0 until lanes) {
        val a = w * (0.46f - 0.045f * k); val b = h * (0.26f - 0.028f * k)
        drawOval(Color.White.a(0.10f), Offset(c.x - a, c.y - b), Size(a * 2, b * 2), style = Stroke(1.5.dp.toPx()))
        val ang = t * (3 + k) * TAU + k * 0.9f
        val tail = lv(level, 5, 8, 12)
        val col = if (k % 2 == 0) red else Color.White
        for (s in 0 until tail) {
            val aa = ang - s * 0.07f
            drawCircle(col.a(0.75f * (1f - s / tail.toFloat())), (4.5f - s * 0.25f).coerceAtLeast(1f).dp.toPx(), Offset(c.x + cos(aa) * a, c.y + sin(aa) * b))
        }
    }
    // linha de largada/chegada na reta de baixo
    val ob = h * 0.26f
    drawLine(red.a(0.55f), Offset(c.x, c.y + ob - h * 0.028f * (lanes - 1)), Offset(c.x, c.y + ob), 3.dp.toPx())
}

// ───────────────────────── Ouro: art déco com raios, ondas douradas e brilhos ─────────────────────────
internal fun DrawScope.drawGold(colors: List<Color>, t: Float, level: Int) {
    val w = size.width; val h = size.height
    val gold = colors.at(0, Color(0xFFD8B25A)); val pale = colors.at(1, Color(0xFFFFE9A8))
    val o = Offset(w * 0.5f, h * 1.04f)
    val rays = 28
    for (k in 0 until rays) {
        val ang = PI.toFloat() + (k + 0.5f) / rays * PI.toFloat()
        val len = h * 0.95f
        val shimmer = 0.55f + 0.45f * sin(t * 2 * TAU - k * 0.4f)
        val p = Path().apply {
            moveTo(o.x, o.y)
            lineTo(o.x + cos(ang - 0.016f) * len, o.y + sin(ang - 0.016f) * len)
            lineTo(o.x + cos(ang + 0.016f) * len, o.y + sin(ang + 0.016f) * len); close()
        }
        drawPath(p, gold.a((if (k % 2 == 0) 0.09f else 0.04f) * shimmer))
    }
    // anéis dourados expandindo do pé da tela
    for (j in 0 until lv(level, 3, 4, 6)) {
        val p = frac(t * 3f + j / lv(level, 3, 4, 6).toFloat())
        drawCircle(gold.a(0.24f * (1f - p)), p * h * 0.95f, o, style = Stroke(1.5.dp.toPx()))
    }
    // moldura déco com cantos cortados
    val m = 10.dp.toPx(); val cut = 18.dp.toPx()
    val frame = Path().apply {
        moveTo(m + cut, m); lineTo(w - m - cut, m); lineTo(w - m, m + cut); lineTo(w - m, h - m - cut); lineTo(w - m - cut, h - m)
        lineTo(m + cut, h - m); lineTo(m, h - m - cut); lineTo(m, m + cut); close()
    }
    drawPath(frame, gold.a(0.28f), style = Stroke(1.dp.toPx()))
    // brilhos
    val rnd = Random(3)
    repeat(lv(level, 8, 14, 26)) {
        val x = w * rnd.nextFloat(); val y = h * rnd.nextFloat(); val ph = rnd.nextFloat()
        val s = max(0f, sin(t * (2 + it % 3) * TAU + ph * TAU))
        if (s > 0.02f) {
            val l = (3f + 6f * s).dp.toPx()
            drawLine(pale.a(0.85f * s), Offset(x - l, y), Offset(x + l, y), 1.dp.toPx())
            drawLine(pale.a(0.85f * s), Offset(x, y - l), Offset(x, y + l), 1.dp.toPx())
            drawCircle(pale.a(s), 1.5.dp.toPx(), Offset(x, y))
        }
    }
}

// ───────────────────────── Aurora: cortinas de luz sobre estrelas e montanhas ─────────────────────────
internal fun DrawScope.drawCurtains(colors: List<Color>, t: Float, level: Int) {
    val w = size.width; val h = size.height
    val rs = Random(15)
    repeat(lv(level, 24, 40, 70)) {
        val x = w * rs.nextFloat(); val y = h * 0.7f * rs.nextFloat(); val ph = rs.nextFloat() * TAU
        drawCircle(Color.White.a(0.25f + 0.5f * (0.5f + 0.5f * sin(t * (2 + it % 4) * TAU + ph))), (0.6f + rs.nextFloat() * 1.2f).dp.toPx(), Offset(x, y))
    }
    val bands = lv(level, 3, 4, 6)
    for (k in 0 until bands) {
        val c = colors.at(k % max(1, colors.size), Color(0xFF00E5C3))
        val baseY = h * (0.10f + 0.09f * k)
        val thick = h * 0.30f
        val steps = 36
        fun topY(i: Int) = baseY + sin(i * 0.35f + t * (1 + k % 2) * TAU + k * 1.7f) * h * 0.05f + sin(i * 0.9f - t * 2 * TAU + k) * h * 0.015f
        val path = Path()
        for (i in 0..steps) { val x = w * i / steps; if (i == 0) path.moveTo(x, topY(i)) else path.lineTo(x, topY(i)) }
        for (i in steps downTo 0) path.lineTo(w * i / steps, topY(i) + thick * (0.8f + 0.2f * sin(i * 0.5f + k)))
        path.close()
        drawPath(path, Brush.verticalGradient(listOf(Color.Transparent, c.a(0.36f), c.a(0.14f), Color.Transparent), baseY - h * 0.05f, baseY + thick + h * 0.05f))
    }
    // serra escura no horizonte
    val rm = Random(2)
    val ridge = Path().apply {
        moveTo(0f, h)
        var x = 0f
        lineTo(0f, h * 0.92f)
        while (x < w) { x += w * (0.07f + 0.06f * rm.nextFloat()); lineTo(x.coerceAtMost(w), h * (0.86f + 0.08f * rm.nextFloat())) }
        lineTo(w, h); close()
    }
    drawPath(ridge, Color.Black.a(0.55f))
}

// ───────────────────────── Synthwave: sol listrado, montanhas de arame e chão em fuga ─────────────────────────
internal fun DrawScope.drawOutrun(colors: List<Color>, t: Float, level: Int) {
    val w = size.width; val h = size.height
    val yellow = colors.at(0, Color(0xFFFFD166)); val pink = colors.at(1, Color(0xFFFF3D9A)); val cyan = colors.at(2, Color(0xFF22D3EE))
    val hy = h * 0.58f
    val sunC = Offset(w * 0.5f, h * 0.42f); val sr = w * 0.24f
    val strip = 2.dp.toPx()
    var yy = sunC.y - sr
    while (yy < sunC.y + sr) {
        val u = (yy - (sunC.y - sr)) / (2 * sr)
        val cut = (u - 0.4f).coerceAtLeast(0f) * 1.3f
        val gap = frac(u * 9f - t * 6f) < cut
        if (!gap) {
            val dy = yy - sunC.y
            val half = sqrt(max(0f, sr * sr - dy * dy))
            val col = Color(
                red = yellow.red + (pink.red - yellow.red) * u, green = yellow.green + (pink.green - yellow.green) * u,
                blue = yellow.blue + (pink.blue - yellow.blue) * u, alpha = 0.85f
            )
            drawRect(col, Offset(sunC.x - half, yy), Size(half * 2, strip))
        }
        yy += strip
    }
    // montanhas de arame
    for (side in 0..1) {
        val bx = if (side == 0) w * 0.22f else w * 0.80f
        val pk = Path().apply { moveTo(bx - w * 0.25f, hy); lineTo(bx - w * 0.05f, hy - h * 0.10f); lineTo(bx + w * 0.02f, hy - h * 0.05f); lineTo(bx + w * 0.12f, hy - h * 0.13f); lineTo(bx + w * 0.28f, hy) }
        drawPath(pk, cyan.a(0.55f), style = Stroke(1.5.dp.toPx(), join = StrokeJoin.Round))
    }
    // chão em fuga
    drawLine(pink.a(0.8f), Offset(0f, hy), Offset(w, hy), 2.dp.toPx())
    val lines = lv(level, 10, 14, 18)
    for (k in 0 until lines) {
        val z = frac(k / lines.toFloat() + t * 4f)
        val y = hy + (h - hy) * z * z
        drawLine(pink.a(0.12f + 0.5f * z), Offset(0f, y), Offset(w, y), 1.dp.toPx())
    }
    for (i in -10..10) drawLine(pink.a(0.35f), Offset(w / 2 + i * w * 0.016f, hy), Offset(w / 2 + i * w * 0.16f, h), 1.dp.toPx())
    // estrelas e uma cadente
    val rs = Random(6)
    repeat(lv(level, 10, 18, 34)) { drawCircle(Color.White.a(0.3f + 0.5f * (0.5f + 0.5f * sin(t * (2 + it % 3) * TAU + it))), 1.dp.toPx(), Offset(w * rs.nextFloat(), hy * 0.8f * rs.nextFloat())) }
    if (level >= 1) {
        val p = frac(t * 6f)
        val sx = w * (1.1f - 1.3f * p); val sy = h * (0.05f + 0.25f * p)
        drawLine(Brush.linearGradient(listOf(Color.White.a(0.9f), Color.Transparent), Offset(sx, sy), Offset(sx + 80.dp.toPx(), sy - 28.dp.toPx())), Offset(sx, sy), Offset(sx + 80.dp.toPx(), sy - 28.dp.toPx()), 2.dp.toPx(), StrokeCap.Round)
    }
}

// ───────────────────────── Confete: balões subindo e estouros de confete ─────────────────────────
internal fun DrawScope.drawParty(colors: List<Color>, t: Float, level: Int) {
    val w = size.width; val h = size.height
    val pal = if (colors.isEmpty()) listOf(Color(0xFF6C4DFF), Color(0xFFFF6B6B), Color(0xFF00C2A8), Color(0xFFFFC93C)) else colors
    val rnd = Random(12)
    repeat(lv(level, 4, 6, 10)) { i ->
        val c = pal[i % pal.size]
        val bx = w * (0.08f + 0.84f * rnd.nextFloat()); val sp = 1 + rnd.nextInt(2); val ph = rnd.nextFloat(); val extra = rnd.nextFloat()
        val x = bx + sin(t * 2 * TAU + ph * 7f) * 10.dp.toPx()
        val y = h * (1.15f - 1.3f * frac(t * sp + ph))
        val r = 15.dp.toPx() + 6.dp.toPx() * extra
        drawLine(c.a(0.40f), Offset(x, y + r * 1.25f), Offset(x + sin(t * 3 * TAU + ph) * 6.dp.toPx(), y + r * 1.25f + 38.dp.toPx()), 1.dp.toPx())
        drawOval(c.a(0.45f), Offset(x - r * 0.82f, y - r), Size(r * 1.64f, r * 2f))
        drawOval(Color.White.a(0.38f), Offset(x - r * 0.45f, y - r * 0.7f), Size(r * 0.3f, r * 0.5f))
        val knot = Path().apply { moveTo(x, y + r); lineTo(x - 3.dp.toPx(), y + r + 5.dp.toPx()); lineTo(x + 3.dp.toPx(), y + r + 5.dp.toPx()); close() }
        drawPath(knot, c.a(0.55f))
    }
    // estouro vindo de um canto de baixo, alternando os lados a cada 6 s
    val cyc = t * 10f
    val p = frac(cyc)
    val side = floor(cyc).toInt() % 2
    val ox = if (side == 0) w * 0.04f else w * 0.96f; val oy = h * 0.98f
    val r2 = Random(100 + side)
    repeat(lv(level, 16, 26, 44)) { i ->
        val ang = (-90f + (if (side == 0) 22f else -22f) + (r2.nextFloat() - 0.5f) * 70f) * PI.toFloat() / 180f
        val v = 0.55f + 0.7f * r2.nextFloat()
        val px = ox + cos(ang) * v * w * 1.1f * p
        val py = oy + sin(ang) * v * h * 1.15f * p + 0.5f * h * 1.1f * p * p
        val fade = if (p < 0.75f) 1f else (1f - p) / 0.25f
        rotate(p * 720f * (if (i % 2 == 0) 1f else -1f) + i * 40f, Offset(px, py)) {
            drawRect(pal[i % pal.size].a(0.8f * fade), Offset(px - 3.dp.toPx(), py - 1.5.dp.toPx()), Size(6.dp.toPx(), 3.dp.toPx()))
        }
    }
}

// ───────────────────────── Magma: crosta rachada com veias de lava e faíscas ─────────────────────────
internal fun DrawScope.drawCracks(colors: List<Color>, t: Float, level: Int) {
    val w = size.width; val h = size.height
    val hot = colors.at(0, Color(0xFFFF6A00)); val gold = colors.at(1, Color(0xFFFFB000)); val red = colors.at(2, Color(0xFFFF3B30))
    drawRect(Brush.verticalGradient(listOf(Color.Transparent, red.a(0.18f)), h * 0.5f, h))
    val rnd = Random(4)
    for (i in 0 until lv(level, 9, 13, 18)) {
        var px = w * rnd.nextFloat(); var py = h * rnd.nextFloat()
        val path = Path().apply { moveTo(px, py) }
        var ang = rnd.nextFloat() * TAU
        repeat(4 + rnd.nextInt(3)) {
            ang += (rnd.nextFloat() - 0.5f) * 1.6f
            val len = w * (0.06f + 0.10f * rnd.nextFloat())
            px += cos(ang) * len; py += sin(ang) * len
            path.lineTo(px, py)
        }
        val a = 0.25f + 0.6f * (0.5f + 0.5f * sin(t * (2 + i % 3) * TAU + i * 1.3f))
        drawPath(path, hot.a(0.10f * a), style = Stroke(10.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawPath(path, hot.a(0.35f * a), style = Stroke(4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawPath(path, gold.a(0.9f * a), style = Stroke(1.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
    val r2 = Random(8)
    repeat(lv(level, 10, 18, 34)) { i ->
        val x = w * r2.nextFloat(); val sp = 2 + r2.nextInt(3); val ph = r2.nextFloat()
        val q = frac(t * sp + ph)
        drawCircle(gold.a(0.8f * (1f - q)), (1.2f + 1.6f * (1f - q)).dp.toPx(), Offset(x + sin(q * 9f + i) * 12.dp.toPx(), h * (1.02f - 0.9f * q)))
    }
}

// ───────────────────────── Ártico: gelo, floco gigante, icebergs à deriva e neve fina ─────────────────────────
internal fun DrawScope.drawGlacier(colors: List<Color>, t: Float, level: Int) {
    val w = size.width; val h = size.height
    val ice = colors.at(0, Color(0xFF1FB6FF)); val mint = colors.at(1, Color(0xFF5EEAD4)); val vio = colors.at(2, Color(0xFF818CF8))
    // aurora fraca no topo
    val top = Path()
    for (i in 0..30) { val x = w * i / 30f; val y = h * 0.10f + sin(i * 0.4f + t * TAU) * h * 0.04f; if (i == 0) top.moveTo(x, y) else top.lineTo(x, y) }
    for (i in 30 downTo 0) top.lineTo(w * i / 30f, h * 0.10f + sin(i * 0.4f + t * TAU) * h * 0.04f + h * 0.16f)
    top.close()
    drawPath(top, Brush.verticalGradient(listOf(Color.Transparent, mint.a(0.18f), vio.a(0.08f), Color.Transparent), h * 0.04f, h * 0.34f))
    // floco gigante girando devagar
    val c = Offset(w * 0.78f, h * 0.24f); val rr = w * 0.30f
    rotate(t * 360f, c) {
        for (arm in 0 until 6) {
            val a = arm * PI.toFloat() / 3f
            val dir = Offset(cos(a), sin(a))
            drawLine(ice.a(0.20f), c, c + dir * rr, 1.5.dp.toPx(), StrokeCap.Round)
            for (b in listOf(0.45f, 0.72f)) {
                val bp = c + dir * (rr * b); val bl = rr * 0.20f * (1.3f - b)
                for (s in listOf(-1f, 1f)) {
                    val ba = a + s * PI.toFloat() / 3f
                    drawLine(ice.a(0.16f), bp, bp + Offset(cos(ba), sin(ba)) * bl, 1.2.dp.toPx(), StrokeCap.Round)
                }
            }
        }
    }
    // icebergs à deriva, com reflexo
    repeat(lv(level, 3, 4, 6)) { i ->
        val rnd = Random(50 + i)
        val y = h * (0.78f + 0.16f * rnd.nextFloat()); val sz = w * (0.12f + 0.12f * rnd.nextFloat())
        val x = w * (frac((if (i % 2 == 0) t else -t) + rnd.nextFloat()) * 1.4f - 0.2f)
        val peak = sz * (0.6f + 0.4f * rnd.nextFloat())
        val p = Path().apply {
            moveTo(x - sz, y); lineTo(x - sz * 0.5f, y - peak * 0.6f); lineTo(x + sz * 0.1f, y - peak); lineTo(x + sz * 0.6f, y - peak * 0.5f); lineTo(x + sz, y); close()
        }
        drawPath(p, ice.a(0.13f))
        drawPath(p, mint.a(0.38f), style = Stroke(1.2.dp.toPx(), join = StrokeJoin.Round))
        drawLine(ice.a(0.10f), Offset(x - sz * 0.8f, y + 4.dp.toPx()), Offset(x + sz * 0.8f, y + 4.dp.toPx()), 2.dp.toPx())
    }
    // neve fina e brilhos
    val rs = Random(31)
    repeat(lv(level, 18, 32, 60)) {
        val x = w * rs.nextFloat(); val sp = 1 + rs.nextInt(2); val ph = rs.nextFloat()
        drawCircle(Color.White.a(0.25f + 0.4f * rs.nextFloat()), (0.8f + rs.nextFloat() * 1.3f).dp.toPx(), Offset(x + sin(t * 2 * TAU + ph * 7f) * 8.dp.toPx(), h * frac(t * sp + ph)))
    }
}
