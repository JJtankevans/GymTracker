package com.vitoroliveira.gymtracker.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal fun hex(v: Long) = Color(v or 0xFF000000)

private fun onColorFor(c: Color) = if (c.luminanceCompat() > 0.5f) Color(0xFF111111) else Color(0xFFFFFFFF)
private fun Color.luminanceCompat(): Float = (0.299f * red + 0.587f * green + 0.114f * blue)

/**
 * Monta um ColorScheme completo a partir de poucas cores-chave.
 * Os containers e superfícies são derivados misturando as cores,
 * então todo tema fica coerente sem 30 linhas de cor cada.
 */
internal fun makeScheme(
    dark: Boolean,
    primary: Color,
    secondary: Color,
    tertiary: Color,
    background: Color,
    surface: Color,
    onSurface: Color,
    outline: Color = lerp(surface, onSurface, 0.35f),
    error: Color = if (dark) hex(0xFFFF6B6B) else hex(0xFFB3261E),
    /** Sobrescreve a cor de card (surfaceContainerHighest) — usado em temas "vidro". */
    cardColor: Color? = null,
    onPrimary: Color = onColorFor(primary),
    topBar: Color? = null
): ColorScheme {
    val container = { c: Color -> lerp(surface, c, if (dark) 0.28f else 0.16f) }
    val base = if (dark) darkColorScheme() else lightColorScheme()
    val mix = { f: Float -> lerp(surface, onSurface, f) }
    return base.copy(
        primary = primary,
        onPrimary = onPrimary,
        primaryContainer = container(primary),
        onPrimaryContainer = if (dark) lerp(primary, Color.White, 0.75f) else lerp(primary, Color.Black, 0.65f),
        secondary = secondary,
        onSecondary = onColorFor(secondary),
        secondaryContainer = container(secondary),
        onSecondaryContainer = if (dark) lerp(secondary, Color.White, 0.75f) else lerp(secondary, Color.Black, 0.65f),
        tertiary = tertiary,
        onTertiary = onColorFor(tertiary),
        tertiaryContainer = container(tertiary),
        onTertiaryContainer = if (dark) lerp(tertiary, Color.White, 0.75f) else lerp(tertiary, Color.Black, 0.65f),
        background = background,
        onBackground = onSurface,
        surface = topBar ?: surface,
        onSurface = onSurface,
        surfaceVariant = mix(0.10f),
        onSurfaceVariant = lerp(surface, onSurface, 0.72f),
        surfaceTint = primary,
        inverseSurface = onSurface,
        inverseOnSurface = surface,
        outline = outline,
        outlineVariant = lerp(surface, outline, 0.5f),
        error = error,
        onError = onColorFor(error),
        errorContainer = container(error),
        onErrorContainer = if (dark) lerp(error, Color.White, 0.75f) else lerp(error, Color.Black, 0.65f),
        surfaceDim = if (dark) surface else mix(0.06f),
        surfaceBright = if (dark) mix(0.12f) else surface,
        surfaceContainerLowest = if (dark) lerp(surface, Color.Black, 0.4f) else lerp(surface, Color.White, 0.8f),
        surfaceContainerLow = mix(0.03f),
        surfaceContainer = mix(0.06f),
        surfaceContainerHigh = mix(0.09f),
        surfaceContainerHighest = cardColor ?: mix(0.12f)
    )
}

/**
 * Família do sistema Android por nome (ex.: "sans-serif-condensed", "sans-serif-light").
 * Dá variedade de tipografia sem embutir arquivos de fonte.
 */
@OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)
internal fun deviceFamily(name: String): FontFamily = FontFamily(
    listOf(
        FontWeight.Light, FontWeight.Normal, FontWeight.Medium, FontWeight.SemiBold,
        FontWeight.Bold, FontWeight.ExtraBold, FontWeight.Black
    ).map { androidx.compose.ui.text.font.Font(androidx.compose.ui.text.font.DeviceFontFamilyName(name), it) }
)

/** Escala de tipografia completa trocando só famílias/pesos/tracking. */
internal fun makeTypography(
    heading: FontFamily,
    body: FontFamily,
    headingWeight: FontWeight = FontWeight.Bold,
    headingTracking: TextUnit? = null,
    bodyWeight: FontWeight? = null,
    labelWeight: FontWeight = FontWeight.Medium,
    /** Ex.: "lnum" força números alinhados em fontes serifadas (senão o 0 parece um "o"). */
    headingFeatures: String? = null
): Typography {
    val b = Typography()
    fun TextStyle.h() = copy(
        fontFamily = heading,
        fontWeight = headingWeight,
        letterSpacing = headingTracking ?: letterSpacing,
        fontFeatureSettings = headingFeatures ?: fontFeatureSettings
    )
    fun TextStyle.t() = copy(
        fontFamily = body,
        fontWeight = bodyWeight ?: fontWeight
    )
    fun TextStyle.l() = copy(fontFamily = body, fontWeight = labelWeight)
    return Typography(
        displayLarge = b.displayLarge.h(), displayMedium = b.displayMedium.h(), displaySmall = b.displaySmall.h(),
        headlineLarge = b.headlineLarge.h(), headlineMedium = b.headlineMedium.h(), headlineSmall = b.headlineSmall.h(),
        titleLarge = b.titleLarge.h(), titleMedium = b.titleMedium.h().copy(fontSize = 16.sp), titleSmall = b.titleSmall.h(),
        bodyLarge = b.bodyLarge.t(), bodyMedium = b.bodyMedium.t(), bodySmall = b.bodySmall.t(),
        labelLarge = b.labelLarge.l(), labelMedium = b.labelMedium.l(), labelSmall = b.labelSmall.l()
    )
}

internal fun roundedShapes(small: Dp, medium: Dp, large: Dp, extraLarge: Dp = large + 8.dp) = Shapes(
    extraSmall = RoundedCornerShape((small.value / 2).dp),
    small = RoundedCornerShape(small),
    medium = RoundedCornerShape(medium),
    large = RoundedCornerShape(large),
    extraLarge = RoundedCornerShape(extraLarge)
)

internal fun cutShapes(small: Dp, medium: Dp, large: Dp) = Shapes(
    extraSmall = CutCornerShape((small.value / 2).dp),
    small = CutCornerShape(small),
    medium = CutCornerShape(medium),
    large = CutCornerShape(topStart = large, topEnd = 0.dp, bottomEnd = large, bottomStart = 0.dp),
    extraLarge = CutCornerShape(topStart = large + 6.dp, topEnd = 0.dp, bottomEnd = large + 6.dp, bottomStart = 0.dp)
)

