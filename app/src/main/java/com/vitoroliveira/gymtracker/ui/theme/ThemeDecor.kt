package com.vitoroliveira.gymtracker.ui.theme

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private const val TAU = (2 * PI).toFloat()

/**
 * Desenha fundo base (cor lisa ou gradiente) + decoração do tema.
 * [t] é o relógio global em 0..1 (volta completa a cada 60s). Todas as
 * animações usam múltiplos inteiros de [t], então o loop é contínuo.
 */
fun DrawScope.drawThemeBackground(extras: ThemeExtras, base: Color, t: Float, level: Int = 1) {
    // tamanho zero (antes do layout): gradiente radial com raio 0 lança exceção no Android
    if (size.width < 2f || size.height < 2f) return
    if (extras.backgroundGradient.size >= 2) {
        drawRect(Brush.verticalGradient(extras.backgroundGradient))
    } else {
        drawRect(base)
    }
    when (extras.decor) {
        Decor.NONE -> Unit
        Decor.PAPER -> drawPaper()
        Decor.NEON_GRID -> drawNeonGrid(extras.decorColors, t)
        Decor.SCANLINES -> drawScanlines(extras.decorColors.firstOrNull() ?: Color.Green, t)
        Decor.AURORA -> drawAurora(extras.decorColors, t)
        Decor.SYNTH_SUN -> drawSynthSun(extras.decorColors, t)
        Decor.CONFETTI -> drawConfetti(extras.decorColors, t)
        Decor.EMBERS -> drawEmbers(extras.decorColors, t)
        Decor.BLUEPRINT -> drawBlueprint()
        Decor.STRIPES -> drawStripes(extras.decorColors.firstOrNull() ?: Color.Red)
        Decor.DOTS -> drawDots(extras.decorColors.firstOrNull() ?: Color.White)
        Decor.HALO -> drawHalo(extras.decorColors.firstOrNull() ?: Color.White)
        Decor.MATRIX -> drawMatrixRain(extras.decorColors, t)
        Decor.SPIRAL -> drawSpiral(extras.decorColors, t)
        Decor.WARP -> drawWarp(extras.decorColors, t)
        Decor.GLITCH -> drawGlitch(extras.decorColors, t)
        Decor.LAVA -> drawLava(extras.decorColors, t)
        Decor.ARCADE -> drawArcade(extras.decorColors, t)
        Decor.CARNIVAL -> drawCarnival(extras.decorColors, t)
        Decor.WAVES -> drawWaves(extras.decorColors, t)
        Decor.FIREFLIES -> drawFireflies(extras.decorColors, t)
        Decor.DECO -> drawDeco(extras.decorColors)
        Decor.STORM -> drawStorm(extras.decorColors, t)
        Decor.DISCO -> drawDisco(extras.decorColors, t)
        Decor.KALEIDO -> drawKaleido(extras.decorColors, t)
        Decor.ABYSS -> drawAbyss(extras.decorColors, t)
        Decor.GALAXY -> drawGalaxy(extras.decorColors, t)
        Decor.STATIC -> drawStatic(extras.decorColors, t)
        Decor.HOLO -> drawHolo(extras.decorColors, t)
        Decor.HAUNTED -> drawHaunted(extras.decorColors, t)
        Decor.FIREWORKS -> drawFireworks(extras.decorColors, t)
        Decor.BLOCKS -> drawBlocks(extras.decorColors, t)
        Decor.ECG -> drawEcg(extras.decorColors, t)
        Decor.LASERS -> drawLasers(extras.decorColors, t)
        Decor.CIRCUIT -> drawCircuit(extras.decorColors, t)
        Decor.SNOW -> drawSnow(extras.decorColors, t)
        Decor.BUBBLES -> drawBubbles(extras.decorColors, t)
        Decor.DESERT -> drawDesert(extras.decorColors, t)
        Decor.ORIGAMI -> drawOrigami(extras.decorColors, t)
        Decor.NEON_CITY -> drawNeonCity(extras.decorColors, t)
        Decor.RADAR -> drawRadar(extras.decorColors, t)
        Decor.SAKURA -> drawSakura(extras.decorColors, t)
        Decor.VOLCANO -> drawVolcano(extras.decorColors, t)
        Decor.DNA -> drawDna(extras.decorColors, t)
        Decor.VINYL -> drawVinyl(extras.decorColors, t)
        Decor.DUNGEON -> drawDungeon(extras.decorColors, t)
        Decor.MATERIAL -> drawMaterial(extras.decorColors, t, level)
        Decor.SPOTLIGHT -> drawSpotlight(extras.decorColors, t, level)
        Decor.CARBON -> drawCarbon(extras.decorColors, t, level)
        Decor.TIDE -> drawTide(extras.decorColors, t, level)
        Decor.INK -> drawInk(extras.decorColors, t, level)
        Decor.NOTEBOOK -> drawNotebook(extras.decorColors, t, level)
        Decor.DRAFT -> drawDraft(extras.decorColors, t, level)
        Decor.BRUTAL -> drawBrutal(extras.decorColors, t, level)
        Decor.NEON_SIGN -> drawNeonSign(extras.decorColors, t, level)
        Decor.TERMINAL -> drawTerminal(extras.decorColors, t, level)
        Decor.TRACK -> drawTrack(extras.decorColors, t, level)
        Decor.GOLD -> drawGold(extras.decorColors, t, level)
        Decor.CURTAINS -> drawCurtains(extras.decorColors, t, level)
        Decor.OUTRUN -> drawOutrun(extras.decorColors, t, level)
        Decor.PARTY -> drawParty(extras.decorColors, t, level)
        Decor.CRACKS -> drawCracks(extras.decorColors, t, level)
        Decor.FJORD -> drawFjord(extras.decorColors, t, level)
        Decor.OBSIDIAN -> drawObsidian(extras.decorColors, t, level)
        Decor.HEXNET -> drawHexNet(extras.decorColors, t, level)
        Decor.FOREST -> drawForest(extras.decorColors, t, level)
        Decor.GLACIER -> drawGlacier(extras.decorColors, t, level)
    }
}

// ───────────────────────── Listras de corrida ─────────────────────────
private fun DrawScope.drawStripes(color: Color) {
    val w = size.width; val h = size.height
    // três faixas a 45° cortando o canto inferior direito (longe da barra de cima e do texto)
    listOf(0.26f to 14.dp, 0.33f to 5.dp, 0.38f to 2.dp).forEach { (f, thick) ->
        val d = w * f
        drawLine(color.copy(alpha = 0.55f), Offset(w, h - d), Offset(w - d, h), thick.toPx())
    }
}

// ───────────────────────── Pontinhos ─────────────────────────
private fun DrawScope.drawDots(color: Color) {
    val gap = 22.dp.toPx()
    val r = 1.1.dp.toPx()
    var y = gap / 2
    while (y < size.height) {
        var x = gap / 2
        while (x < size.width) {
            drawCircle(color.copy(alpha = 0.10f), r, Offset(x, y))
            x += gap
        }
        y += gap
    }
}

// ───────────────────────── Halo ─────────────────────────
private fun DrawScope.drawHalo(color: Color) {
    val r = size.width * 1.1f
    drawRect(
        Brush.radialGradient(
            listOf(color.copy(alpha = 0.22f), color.copy(alpha = 0.06f), Color.Transparent),
            center = Offset(size.width / 2f, -size.height * 0.05f), radius = r
        )
    )
}

// ───────────────────────── Papel ─────────────────────────
private fun DrawScope.drawPaper() {
    val rnd = Random(7)
    val n = ((size.width * size.height) / 2600f).toInt().coerceIn(150, 900)
    repeat(n) {
        val x = rnd.nextFloat() * size.width
        val y = rnd.nextFloat() * size.height
        val a = 0.025f + rnd.nextFloat() * 0.05f
        drawCircle(Color(0xFF3A2E1C).copy(alpha = a), radius = 0.6f + rnd.nextFloat() * 1.2f, center = Offset(x, y))
    }
    // vinheta quente nas bordas
    drawRect(
        Brush.radialGradient(
            0.6f to Color.Transparent,
            1f to Color(0xFF7A5C2E).copy(alpha = 0.10f),
            center = center, radius = size.maxDimension * 0.75f
        )
    )
}

// ───────────────────────── Blueprint ─────────────────────────
private fun DrawScope.drawBlueprint() {
    val minor = 24.dp.toPx()
    val line = Color.White
    var x = 0f
    var i = 0
    while (x <= size.width) {
        val major = i % 5 == 0
        drawLine(line.copy(alpha = if (major) 0.16f else 0.06f), Offset(x, 0f), Offset(x, size.height), if (major) 1.2f else 0.6f)
        x += minor; i++
    }
    var y = 0f
    i = 0
    while (y <= size.height) {
        val major = i % 5 == 0
        drawLine(line.copy(alpha = if (major) 0.16f else 0.06f), Offset(0f, y), Offset(size.width, y), if (major) 1.2f else 0.6f)
        y += minor; i++
    }
    // marcas de registro nos cantos
    val m = 14.dp.toPx(); val s = 10.dp.toPx()
    listOf(Offset(m, m), Offset(size.width - m, m), Offset(m, size.height - m), Offset(size.width - m, size.height - m)).forEach {
        drawLine(line.copy(alpha = 0.5f), it - Offset(s, 0f), it + Offset(s, 0f), 1.4f)
        drawLine(line.copy(alpha = 0.5f), it - Offset(0f, s), it + Offset(0f, s), 1.4f)
    }
}

// ───────────────────────── Neon grid ─────────────────────────
private fun DrawScope.drawNeonGrid(colors: List<Color>, t: Float) {
    val pink = colors.getOrElse(0) { Color.Magenta }
    val cyan = colors.getOrElse(1) { Color.Cyan }
    val horizon = size.height * 0.58f
    // brilho no horizonte
    drawRect(
        Brush.verticalGradient(
            listOf(Color.Transparent, pink.copy(alpha = 0.22f), Color.Transparent),
            startY = horizon - size.height * 0.18f, endY = horizon + size.height * 0.10f
        )
    )
    val vx = size.width / 2f
    // linhas verticais em fuga
    val cols = 14
    for (i in -cols..cols) {
        val xb = vx + i * (size.width / cols) * 1.5f
        drawLine(cyan.copy(alpha = 0.28f), Offset(vx + i * 6f, horizon), Offset(xb, size.height), 1.2f)
    }
    // linhas horizontais rolando em direção ao usuário
    val lines = 12
    val scroll = (t * 60f) % 1f   // 60 ciclos/min = 1 por segundo
    for (k in 0 until lines) {
        val p = ((k + scroll) / lines)
        val eased = p * p
        val y = horizon + (size.height - horizon) * eased
        drawLine(pink.copy(alpha = 0.10f + 0.32f * eased), Offset(0f, y), Offset(size.width, y), 1f + 1.6f * eased)
    }
    drawLine(pink.copy(alpha = 0.7f), Offset(0f, horizon), Offset(size.width, horizon), 2f)
}

// ───────────────────────── Scanlines CRT ─────────────────────────
private fun DrawScope.drawScanlines(color: Color, t: Float) {
    val gap = 3.dp.toPx()
    var y = 0f
    while (y < size.height) {
        drawLine(color.copy(alpha = 0.045f), Offset(0f, y), Offset(size.width, y), 1f)
        y += gap
    }
    // faixa de refresh varrendo a tela (1 passada a cada 6s)
    val sweep = ((t * 10f) % 1f) * (size.height + 160f) - 80f
    drawRect(
        Brush.verticalGradient(
            listOf(Color.Transparent, color.copy(alpha = 0.07f), Color.Transparent),
            startY = sweep - 80f, endY = sweep + 80f
        ),
        topLeft = Offset(0f, sweep - 80f), size = Size(size.width, 160f)
    )
    // vinheta de tubo
    drawRect(
        Brush.radialGradient(
            0.55f to Color.Transparent,
            1f to Color.Black.copy(alpha = 0.55f),
            center = center, radius = size.maxDimension * 0.7f
        )
    )
}

// ───────────────────────── Aurora ─────────────────────────
private fun DrawScope.drawAurora(colors: List<Color>, t: Float) {
    val palette = colors.ifEmpty { listOf(Color.Cyan, Color.Magenta) }
    val w = size.width; val h = size.height
    palette.forEachIndexed { i, c ->
        val phase = i * 0.37f
        val cx = w * (0.5f + 0.38f * sin(TAU * (t * 1f + phase)))
        val cy = h * (0.5f + 0.36f * cos(TAU * (t * 2f + phase * 1.7f)))
        val r = size.maxDimension * (0.42f + 0.06f * sin(TAU * (t * 3f + phase)))
        drawCircle(
            Brush.radialGradient(
                listOf(c.copy(alpha = 0.50f), c.copy(alpha = 0.18f), Color.Transparent),
                center = Offset(cx, cy), radius = r
            ),
            radius = r, center = Offset(cx, cy)
        )
    }
    // véu escuro leve pra manter o contraste do texto
    drawRect(Color.Black.copy(alpha = 0.34f))
}

// ───────────────────────── Synthwave ─────────────────────────
private fun DrawScope.drawSynthSun(colors: List<Color>, t: Float) {
    val sunYellow = colors.getOrElse(0) { Color.Yellow }
    val pink = colors.getOrElse(1) { Color.Magenta }
    val cyan = colors.getOrElse(2) { Color.Cyan }
    val w = size.width; val h = size.height
    val horizon = h * 0.74f
    // estrelas
    val rnd = Random(21)
    repeat(70) {
        val x = rnd.nextFloat() * w
        val y = rnd.nextFloat() * horizon * 0.8f
        val tw = 0.35f + 0.65f * (0.5f + 0.5f * sin(TAU * (t * (2 + it % 5) + it * 0.13f)))
        drawCircle(Color.White.copy(alpha = 0.55f * tw), radius = 0.8f + rnd.nextFloat() * 1.3f, center = Offset(x, y))
    }
    // sol
    val r = w * 0.27f
    val c = Offset(w / 2f, horizon - r * 0.55f)
    drawCircle(Brush.verticalGradient(listOf(sunYellow.copy(alpha = 0.30f), pink.copy(alpha = 0.30f)), startY = c.y - r, endY = c.y + r), radius = r, center = c)
    // listras cortando a metade de baixo do sol (cor do horizonte)
    val horizonColor = Color(0xFF3B0F6B)
    var y = c.y + r * 0.05f
    var th = r * 0.035f
    while (y < c.y + r) {
        drawRect(horizonColor.copy(alpha = 0.45f), topLeft = Offset(c.x - r, y), size = Size(r * 2, th))
        y += th * 2.6f
        th *= 1.22f
    }
    // chão escuro + grade
    drawRect(Color(0xFF14082E), topLeft = Offset(0f, horizon), size = Size(w, h - horizon))
    val vx = w / 2f
    for (i in -10..10) {
        drawLine(cyan.copy(alpha = 0.40f), Offset(vx + i * 5f, horizon), Offset(vx + i * (w / 10f) * 1.4f, h), 1.2f)
    }
    val scroll = (t * 30f) % 1f
    val lines = 9
    for (k in 0 until lines) {
        val p = (k + scroll) / lines
        val yy = horizon + (h - horizon) * p * p
        drawLine(cyan.copy(alpha = 0.15f + 0.45f * p), Offset(0f, yy), Offset(w, yy), 1f + 1.4f * p)
    }
    drawLine(pink.copy(alpha = 0.9f), Offset(0f, horizon), Offset(w, horizon), 2.5f)
}

// ───────────────────────── Confete / Memphis ─────────────────────────
private fun DrawScope.drawConfetti(colors: List<Color>, t: Float) {
    val palette = colors.ifEmpty { listOf(Color.Red, Color.Blue) }
    val rnd = Random(99)
    val count = 18
    val stroke = 3.dp.toPx()
    repeat(count) { i ->
        val x = rnd.nextFloat() * size.width + 6f * sin(TAU * (t * 2 + i * 0.1f))
        val y = rnd.nextFloat() * size.height + 6f * cos(TAU * (t * 2 + i * 0.1f))
        val s = 14.dp.toPx() + rnd.nextFloat() * 22.dp.toPx()
        val col = palette[i % palette.size].copy(alpha = 0.28f)
        val rot = rnd.nextFloat() * 360f + t * 360f * if (i % 2 == 0) 1 else -1
        when (i % 5) {
            0 -> drawCircle(col, radius = s / 2, center = Offset(x, y))
            1 -> drawCircle(col, radius = s / 2, center = Offset(x, y), style = Stroke(stroke))
            2 -> rotate(rot, Offset(x, y)) {
                val p = Path().apply {
                    moveTo(x, y - s / 2); lineTo(x + s / 2, y + s / 2); lineTo(x - s / 2, y + s / 2); close()
                }
                drawPath(p, col)
            }
            3 -> rotate(rot, Offset(x, y)) {
                drawLine(col, Offset(x - s / 2, y), Offset(x + s / 2, y), stroke * 1.6f)
                drawLine(col, Offset(x, y - s / 2), Offset(x, y + s / 2), stroke * 1.6f)
            }
            else -> rotate(rot, Offset(x, y)) {
                val p = Path().apply {
                    moveTo(x - s, y)
                    quadraticTo(x - s / 2, y - s / 2, x, y)
                    quadraticTo(x + s / 2, y + s / 2, x + s, y)
                }
                drawPath(p, col, style = Stroke(stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round))
            }
        }
    }
}

// ───────────────────────── Brasas ─────────────────────────
private fun DrawScope.drawEmbers(colors: List<Color>, t: Float) {
    val palette = colors.ifEmpty { listOf(Color(0xFFFF6A00)) }
    val w = size.width; val h = size.height
    // calor no chão
    drawRect(
        Brush.verticalGradient(
            listOf(Color.Transparent, palette[0].copy(alpha = 0.22f)),
            startY = h * 0.55f, endY = h
        )
    )
    val rnd = Random(5)
    repeat(46) { i ->
        val speed = 1 + (i % 3)                      // ciclos por minuto (inteiro = loop perfeito)
        val phase = rnd.nextFloat()
        val p = (t * speed * 6f + phase) % 1f        // 0 (embaixo) → 1 (topo)
        val x = rnd.nextFloat() * w + 18f * sin(TAU * (p * 2f + phase))
        val y = h * (1f - p)
        val fade = (1f - p) * (if (p < 0.08f) p / 0.08f else 1f)
        val col = palette[i % palette.size]
        val rad = 1.2f + rnd.nextFloat() * 2.8f
        drawCircle(col.copy(alpha = 0.18f * fade), radius = rad * 4f, center = Offset(x, y))
        drawCircle(col.copy(alpha = 0.9f * fade), radius = rad, center = Offset(x, y))
    }
}
