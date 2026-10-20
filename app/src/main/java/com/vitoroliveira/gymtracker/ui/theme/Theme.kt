package com.vitoroliveira.gymtracker.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.lerp
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/** Aplica cores/tipografia/formas/extras do tema, sem desenhar fundo. */
@Composable
fun ThemeScope(
    spec: AppThemeSpec,
    colors: ColorScheme = spec.colors,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalThemeExtras provides spec.extras) {
        MaterialTheme(
            colorScheme = colors,
            typography = spec.typography,
            shapes = spec.shapes,
            content = content
        )
    }
}

private fun Context.findActivity(): Activity? {
    var c: Context? = this
    while (c is ContextWrapper) {
        if (c is Activity) return c
        c = c.baseContext
    }
    return null
}

private fun animationsEnabled(context: Context): Boolean = try {
    Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f
} catch (_: Exception) {
    true
}

/**
 * Tema raiz do app. Sem argumento usa o tema padrão (útil nos @Preview);
 * a MainActivity passa o tema escolhido pelo usuário.
 */
@Composable
fun GymTrackerTheme(
    spec: AppThemeSpec = AppThemes.byId(AppThemes.DEFAULT_ID),
    accent: Color? = null,
    intensity: Intensity = Intensity.NORMAL,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val systemDark = isSystemInDarkTheme()

    val baseColors: ColorScheme = when {
        spec.dynamic && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (systemDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        spec.dynamic && systemDark -> AppThemes.Carbon.colors
        else -> spec.colors
    }
    val dark = if (spec.dynamic) systemDark else spec.isDark

    // Cor da capa do álbum (música tocando): transição suave, só quando há uma
    val animatedAccent by animateColorAsState(accent ?: baseColors.primary, tween(900), label = "accent")
    val colors = if (accent != null) baseColors.withAccent(animatedAccent, dark) else baseColors
    val themed = if (accent != null) spec.withAccentExtras(animatedAccent) else spec

    val animate = remember(context) { animationsEnabled(context) } && spec.extras.animated
    val clock: () -> Float = if (animate) {
        val transition = rememberInfiniteTransition(label = "theme-clock")
        val t = transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(60_000, easing = LinearEasing), RepeatMode.Restart),
            label = "t"
        )
        ({ t.value })
    } else {
        { 0.3f }
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = view.context.findActivity()?.window ?: return@SideEffect
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !dark
            controller.isAppearanceLightNavigationBars = !dark
        }
    }

    CompositionLocalProvider(LocalThemeClock provides clock, LocalIntensity provides intensity) {
        ThemeScope(themed, colors, content)
    }
}

private fun Color.onColor(): Color = if (luminance() > 0.5f) Color(0xFF101010) else Color.White

/** Troca a cor primária pela da capa do álbum, mantendo contraste. */
private fun ColorScheme.withAccent(a: Color, dark: Boolean): ColorScheme {
    val container = lerp(surface, a, if (dark) 0.32f else 0.22f)
    return copy(
        primary = a, onPrimary = a.onColor(),
        primaryContainer = container, onPrimaryContainer = container.onColor(),
        surfaceTint = a, inversePrimary = a
    )
}

private fun shiftHue(c: Color, deg: Float): Color {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(android.graphics.Color.argb(255, (c.red * 255).toInt(), (c.green * 255).toInt(), (c.blue * 255).toInt()), hsv)
    hsv[0] = (hsv[0] + deg + 360f) % 360f
    return Color(android.graphics.Color.HSVToColor(hsv))
}

/** Decorações animadas também pegam a paleta da música. */
private fun AppThemeSpec.withAccentExtras(a: Color): AppThemeSpec {
    val reactive = when (extras.decor) {
        Decor.AURORA, Decor.SYNTH_SUN, Decor.NEON_GRID, Decor.EMBERS, Decor.CONFETTI -> true
        else -> false
    }
    if (!reactive) return this
    val palette = listOf(a, shiftHue(a, 40f), shiftHue(a, -45f), shiftHue(a, 90f))
    val n = extras.decorColors.size.coerceAtLeast(1)
    return copy(
        extras = extras.copy(
            decorColors = List(n) { palette[it % palette.size] },
            glowColor = extras.glowColor?.let { a.copy(alpha = it.alpha) }
        )
    )
}
