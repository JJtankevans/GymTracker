package com.vitoroliveira.gymtracker.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Fundo do tema (cor/gradiente + decoração animada) atrás de qualquer composable. */
@Composable
fun Modifier.themedBackground(): Modifier {
    val extras = LocalThemeExtras.current
    val bg = MaterialTheme.colorScheme.background
    val clock = LocalThemeClock.current
    val intensity = LocalIntensity.current
    val accent = MaterialTheme.colorScheme.primary
    return this.drawBehind {
        val t = clock()
        // Insano: o fundo anda 3x mais rápido (múltiplo inteiro, então o loop continua contínuo)
        drawThemeBackground(extras, bg, if (intensity == Intensity.WILD) (t * 3f) % 1f else t)
        val animatedDecor = extras.decor !in listOf(
            Decor.NONE, Decor.PAPER, Decor.BLUEPRINT, Decor.STRIPES, Decor.DOTS, Decor.HALO, Decor.DECO
        )
        // Calmo: um véu da cor do fundo deixa a decoração discreta
        if (intensity == Intensity.CALM && extras.decor != Decor.NONE) drawRect(bg.copy(alpha = 0.5f))
        if (intensity == Intensity.WILD && animatedDecor) drawWildOverlay(extras.decorColors.firstOrNull() ?: accent, t)
    }
}

/** Sombra "dura" deslocada, sem blur. */
private fun Modifier.hardShadow(shape: Shape, offset: Dp, color: Color): Modifier = drawBehind {
    val o = offset.toPx()
    translate(o, o) {
        drawOutline(shape.createOutline(size, layoutDirection, this@drawBehind), color)
    }
}

/** Brilho neon: contornos concêntricos com alpha decrescente. */
private fun Modifier.neonGlow(shape: Shape, color: Color): Modifier = drawBehind {
    val outline = shape.createOutline(size, layoutDirection, this)
    for (i in 1..4) {
        drawOutline(outline, color.copy(alpha = color.alpha * 0.20f / i), style = Stroke(width = (i * 3).dp.toPx()))
    }
}

/**
 * Substituto do Scaffold: desenha o fundo do tema por tela. Como o relógio
 * da decoração é compartilhado, a animação continua contínua na navegação.
 */
@Composable
fun ThemedScaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    snackbarHost: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    floatingActionButtonPosition: FabPosition = FabPosition.End,
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        modifier = modifier.themedBackground(),
        topBar = topBar,
        bottomBar = bottomBar,
        snackbarHost = snackbarHost,
        floatingActionButton = floatingActionButton,
        floatingActionButtonPosition = floatingActionButtonPosition,
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onBackground,
        content = content
    )
}

/** TopAppBar transparente: deixa o fundo do tema aparecer. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemedTopAppBar(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {}
) {
    TopAppBar(
        title = title,
        modifier = modifier,
        navigationIcon = navigationIcon,
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
            scrolledContainerColor = Color.Transparent
        )
    )
}

/**
 * Card que respeita o estilo do tema: borda, sombra dura, brilho neon.
 * Use no lugar de `Card`.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ThemedCard(
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.large,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    border: BorderStroke? = null,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val x = LocalThemeExtras.current
    val themeBorder = border ?: if (x.cardBorderWidth.value > 0f) BorderStroke(x.cardBorderWidth, x.cardBorderColor) else null

    var m = modifier
    if (x.hardShadowOffset.value > 0f) m = m.hardShadow(shape, x.hardShadowOffset, x.hardShadowColor)
    x.glowColor?.let { m = m.neonGlow(shape, it) }

    if (onClick != null && onLongClick != null) {
        Surface(
            modifier = m.clip(shape).combinedClickable(onClick = onClick, onLongClick = onLongClick),
            shape = shape, color = containerColor, contentColor = contentColor, border = themeBorder
        ) {
            Column(content = content)
        }
    } else if (onClick != null) {
        Surface(onClick = onClick, modifier = m, shape = shape, color = containerColor, contentColor = contentColor, border = themeBorder) {
            Column(content = content)
        }
    } else {
        Surface(modifier = m, shape = shape, color = containerColor, contentColor = contentColor, border = themeBorder) {
            Column(content = content)
        }
    }
}
