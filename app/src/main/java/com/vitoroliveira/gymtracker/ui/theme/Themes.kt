package com.vitoroliveira.gymtracker.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Catálogo de temas do GymTracker.
 *
 * Pra tirar um tema do app sem apagar nada: `enabled = false`.
 * Pra criar um novo: copie um bloco, mude as cores/formas/decoração e
 * adicione em [AppThemes.all].
 */
object AppThemes {

    const val DEFAULT_ID = "carbon"

    // ───────────────────────── PROFISSIONAIS ─────────────────────────

    /** Material You: usa as cores do papel de parede (Android 12+). */
    private val studioColors = makeScheme(
        dark = false,
        primary = hex(0x4F46E5), secondary = hex(0x0EA5E9), tertiary = hex(0xF59E0B),
        background = hex(0xF5F6FA), surface = hex(0xFFFFFF), onSurface = hex(0x14161F),
        cardColor = hex(0xFFFFFF)
    )

    val System = AppThemeSpec(
        id = "system", name = "Sistema", tagline = "Cores do seu celular (Material You)",
        category = ThemeCategory.PRO, isDark = false,
        colors = studioColors,
        typography = makeTypography(FontFamily.SansSerif, FontFamily.SansSerif),
        shapes = roundedShapes(8.dp, 12.dp, 16.dp),
        dynamic = true
    )

    val Studio = AppThemeSpec(
        id = "studio", name = "Studio", tagline = "Claro, limpo, índigo",
        category = ThemeCategory.PRO, isDark = false,
        colors = studioColors,
        typography = makeTypography(FontFamily.SansSerif, FontFamily.SansSerif, FontWeight.SemiBold),
        shapes = roundedShapes(8.dp, 12.dp, 18.dp),
        extras = ThemeExtras(
            cardBorderWidth = 1.dp, cardBorderColor = hex(0xE3E5EC),
            success = hex(0x16A34A)
        )
    )

    val Carbon = AppThemeSpec(
        id = "carbon", name = "Carbon", tagline = "Grafite com verde-limão. Cara de academia",
        category = ThemeCategory.PRO, isDark = true,
        colors = makeScheme(
            dark = true,
            primary = hex(0xC6F432), secondary = hex(0x7DD3FC), tertiary = hex(0xFF7A3D),
            background = hex(0x0C0D0F), surface = hex(0x15171A), onSurface = hex(0xECEEF1),
            cardColor = hex(0x1C1F23)
        ),
        typography = makeTypography(FontFamily.SansSerif, FontFamily.SansSerif, FontWeight.ExtraBold, (-0.3).sp),
        shapes = roundedShapes(8.dp, 12.dp, 14.dp),
        extras = ThemeExtras(
            homeLayout = HomeLayout.BENTO,
            cardBorderWidth = 1.dp, cardBorderColor = hex(0x2A2E34),
            success = hex(0xC6F432)
        )
    )

    val Oceano = AppThemeSpec(
        id = "oceano", name = "Oceano", tagline = "Azul-marinho profundo, calmo e focado",
        category = ThemeCategory.PRO, isDark = true,
        colors = makeScheme(
            dark = true,
            primary = hex(0x38BDF8), secondary = hex(0x818CF8), tertiary = hex(0x2DD4BF),
            background = hex(0x08111F), surface = hex(0x0F1B2E), onSurface = hex(0xE6EEF9),
            cardColor = hex(0x14243B)
        ),
        typography = makeTypography(FontFamily.SansSerif, FontFamily.SansSerif, FontWeight.SemiBold),
        shapes = roundedShapes(10.dp, 14.dp, 20.dp),
        extras = ThemeExtras(
            homeLayout = HomeLayout.ORBIT,
            workoutLayout = WorkoutLayout.FOCUS,
            backgroundGradient = listOf(hex(0x0B1730), hex(0x07101D)),
            cardBorderWidth = 1.dp, cardBorderColor = hex(0x1F3556),
            success = hex(0x2DD4BF)
        )
    )

    val Tinta = AppThemeSpec(
        id = "tinta", name = "Tinta", tagline = "Preto no branco. Zero distração",
        category = ThemeCategory.PRO, isDark = false,
        colors = makeScheme(
            dark = false,
            primary = hex(0x111111), secondary = hex(0x555555), tertiary = hex(0x8A8A8A),
            background = hex(0xFFFFFF), surface = hex(0xFFFFFF), onSurface = hex(0x111111),
            outline = hex(0x9A9A9A), cardColor = hex(0xFFFFFF)
        ),
        typography = makeTypography(FontFamily.SansSerif, FontFamily.SansSerif, FontWeight.Medium, (-0.4).sp),
        shapes = roundedShapes(2.dp, 4.dp, 6.dp),
        extras = ThemeExtras(
            homeLayout = HomeLayout.POSTER,
            workoutLayout = WorkoutLayout.FOCUS,
            cardBorderWidth = 1.dp, cardBorderColor = hex(0xD2D2D2),
            success = hex(0x1B8A4B)
        )
    )

    // ───────────────────────── OUSADOS ─────────────────────────

    val Papel = AppThemeSpec(
        id = "papel", name = "Papel", tagline = "Editorial, serifado, tinta vermelha",
        category = ThemeCategory.BOLD, isDark = false,
        colors = makeScheme(
            dark = false,
            primary = hex(0xB3261E), secondary = hex(0x1F4E79), tertiary = hex(0x8A6D1D),
            background = hex(0xF2ECDF), surface = hex(0xF8F4EA), onSurface = hex(0x1B1A17),
            outline = hex(0x6B665A), cardColor = hex(0xFBF8F0)
        ),
        typography = makeTypography(FontFamily.Serif, FontFamily.SansSerif, FontWeight.Bold, (-0.5).sp),
        shapes = roundedShapes(1.dp, 2.dp, 3.dp),
        extras = ThemeExtras(
            homeLayout = HomeLayout.EDITORIAL,
            decor = Decor.PAPER,
            cardBorderWidth = 1.dp, cardBorderColor = hex(0x1B1A17).copy(alpha = 0.75f),
            success = hex(0x2E6B3A)
        )
    )

    val Blueprint = AppThemeSpec(
        id = "blueprint", name = "Blueprint", tagline = "Planta técnica, traço branco, amarelo de obra",
        category = ThemeCategory.BOLD, isDark = true,
        colors = makeScheme(
            dark = true,
            primary = hex(0xFFD400), secondary = hex(0x9CC9FF), tertiary = hex(0xFF8A65),
            background = hex(0x0A2A5E), surface = hex(0x0C3270), onSurface = hex(0xEAF3FF),
            outline = hex(0x7FA6E0), cardColor = Color(0x2E0A2A5E).copy(alpha = 0.55f)
        ),
        typography = makeTypography(FontFamily.Monospace, FontFamily.SansSerif, FontWeight.Bold),
        shapes = roundedShapes(2.dp, 2.dp, 3.dp),
        extras = ThemeExtras(
            homeLayout = HomeLayout.BENTO,
            decor = Decor.BLUEPRINT,
            cardBorderWidth = 1.dp, cardBorderColor = Color.White.copy(alpha = 0.55f),
            success = hex(0x7CFFB2)
        )
    )

    val Brutal = AppThemeSpec(
        id = "brutal", name = "Brutal", tagline = "Neo-brutalista: borda grossa, sombra dura, cor gritando",
        category = ThemeCategory.BOLD, isDark = false,
        colors = makeScheme(
            dark = false,
            primary = hex(0xFF4D8D), secondary = hex(0x2EC4B6), tertiary = hex(0xFFD23F),
            background = hex(0xFEF6E4), surface = hex(0xFFFFFF), onSurface = hex(0x000000),
            outline = hex(0x000000), cardColor = hex(0xFFFFFF), onPrimary = hex(0x000000)
        ),
        typography = makeTypography(FontFamily.SansSerif, FontFamily.SansSerif, FontWeight.Black, (-0.6).sp, labelWeight = FontWeight.Bold),
        shapes = roundedShapes(4.dp, 6.dp, 8.dp),
        extras = ThemeExtras(
            homeLayout = HomeLayout.POSTER,
            workoutLayout = WorkoutLayout.FOCUS,
            cardBorderWidth = 3.dp, cardBorderColor = hex(0x000000),
            hardShadowOffset = 6.dp, hardShadowColor = hex(0x000000),
            success = hex(0x00B87A)
        )
    )

    val Neon = AppThemeSpec(
        id = "neon", name = "Neon", tagline = "Cyberpunk: cantos cortados, grade em fuga, brilho",
        category = ThemeCategory.BOLD, isDark = true,
        colors = makeScheme(
            dark = true,
            primary = hex(0xFF2BD6), secondary = hex(0x00F0FF), tertiary = hex(0xFCEE0A),
            background = hex(0x07040F), surface = hex(0x100820), onSurface = hex(0xF4EFFF),
            outline = hex(0x00F0FF), cardColor = hex(0x170B30), onPrimary = hex(0x12001A)
        ),
        typography = makeTypography(FontFamily.Monospace, FontFamily.SansSerif, FontWeight.Bold, 0.5.sp),
        shapes = cutShapes(6.dp, 10.dp, 16.dp),
        extras = ThemeExtras(
            homeLayout = HomeLayout.ORBIT,
            workoutLayout = WorkoutLayout.FOCUS,
            decor = Decor.NEON_GRID,
            decorColors = listOf(hex(0xFF2BD6), hex(0x00F0FF)),
            cardBorderWidth = 1.dp, cardBorderColor = hex(0x00F0FF).copy(alpha = 0.65f),
            glowColor = hex(0x00F0FF),
            success = hex(0x00FF9C),
            animated = true
        )
    )

    val Terminal = AppThemeSpec(
        id = "terminal", name = "Terminal", tagline = "Fósforo verde num monitor CRT",
        category = ThemeCategory.BOLD, isDark = true,
        colors = makeScheme(
            dark = true,
            primary = hex(0x33FF66), secondary = hex(0x00D4A0), tertiary = hex(0xFFB000),
            background = hex(0x000000), surface = hex(0x040A05), onSurface = hex(0x33FF66),
            outline = hex(0x1F9D3A), cardColor = hex(0x07120A), error = hex(0xFF4D4D)
        ),
        typography = makeTypography(FontFamily.Monospace, FontFamily.Monospace, FontWeight.Bold, labelWeight = FontWeight.Bold),
        shapes = Shapes(
            extraSmall = RoundedCornerShape(0.dp), small = RoundedCornerShape(0.dp),
            medium = RoundedCornerShape(0.dp), large = RoundedCornerShape(0.dp),
            extraLarge = RoundedCornerShape(0.dp)
        ),
        extras = ThemeExtras(
            homeLayout = HomeLayout.TERMINAL,
            decor = Decor.SCANLINES,
            decorColors = listOf(hex(0x33FF66)),
            cardBorderWidth = 1.dp, cardBorderColor = hex(0x1F9D3A),
            success = hex(0x33FF66),
            animated = true
        )
    )

    // ───────────────────────── ABSTRATOS ─────────────────────────

    val Aurora = AppThemeSpec(
        id = "aurora", name = "Aurora", tagline = "Luz líquida em movimento, cartões de vidro",
        category = ThemeCategory.ABSTRACT, isDark = true,
        colors = makeScheme(
            dark = true,
            primary = hex(0x7CF7D4), secondary = hex(0xB28DFF), tertiary = hex(0xFF8AD8),
            background = hex(0x070B1A), surface = hex(0x0E1430), onSurface = hex(0xF2F5FF),
            cardColor = Color.White.copy(alpha = 0.10f), onPrimary = hex(0x00201A),
            topBar = Color.Transparent
        ),
        typography = makeTypography(FontFamily.SansSerif, FontFamily.SansSerif, FontWeight.SemiBold, (-0.2).sp),
        shapes = roundedShapes(14.dp, 20.dp, 28.dp),
        extras = ThemeExtras(
            homeLayout = HomeLayout.ORBIT,
            workoutLayout = WorkoutLayout.FOCUS,
            decor = Decor.AURORA,
            decorColors = listOf(hex(0x00E5C3), hex(0x7B4DFF), hex(0xFF4DB8), hex(0x2D9CFF)),
            cardBorderWidth = 1.dp, cardBorderColor = Color.White.copy(alpha = 0.16f),
            success = hex(0x7CF7D4),
            animated = true
        )
    )

    val Sunset = AppThemeSpec(
        id = "sunset", name = "Synthwave", tagline = "Sol retrô, grade no horizonte, anos 80",
        category = ThemeCategory.ABSTRACT, isDark = true,
        colors = makeScheme(
            dark = true,
            primary = hex(0xFF3D9A), secondary = hex(0x22D3EE), tertiary = hex(0xFFD166),
            background = hex(0x1A0B3B), surface = hex(0x24104A), onSurface = hex(0xFFF3FB),
            cardColor = hex(0x140A33).copy(alpha = 0.62f), topBar = Color.Transparent
        ),
        typography = makeTypography(FontFamily.SansSerif, FontFamily.SansSerif, FontWeight.ExtraBold, 0.2.sp),
        shapes = roundedShapes(12.dp, 18.dp, 22.dp),
        extras = ThemeExtras(
            homeLayout = HomeLayout.POSTER,
            workoutLayout = WorkoutLayout.FOCUS,
            decor = Decor.SYNTH_SUN,
            backgroundGradient = listOf(hex(0x140A33), hex(0x3B0F6B), hex(0xB3175F), hex(0xFF7A3D)),
            decorColors = listOf(hex(0xFFD166), hex(0xFF3D9A), hex(0x22D3EE)),
            cardBorderWidth = 1.dp, cardBorderColor = hex(0xFF3D9A).copy(alpha = 0.55f),
            glowColor = hex(0xFF3D9A).copy(alpha = 0.6f),
            success = hex(0x5CFFB8),
            animated = true
        )
    )

    val Confete = AppThemeSpec(
        id = "confete", name = "Confete", tagline = "Memphis: formas, cores e bagunça organizada",
        category = ThemeCategory.ABSTRACT, isDark = false,
        colors = makeScheme(
            dark = false,
            primary = hex(0x6C4DFF), secondary = hex(0xFF6B6B), tertiary = hex(0x00C2A8),
            background = hex(0xFFF8EC), surface = hex(0xFFFFFF), onSurface = hex(0x1F1F3D),
            outline = hex(0x1F1F3D), cardColor = hex(0xFFFFFF)
        ),
        typography = makeTypography(FontFamily.SansSerif, FontFamily.SansSerif, FontWeight.ExtraBold, (-0.3).sp),
        shapes = roundedShapes(12.dp, 18.dp, 22.dp),
        extras = ThemeExtras(
            homeLayout = HomeLayout.BENTO,
            decor = Decor.CONFETTI,
            decorColors = listOf(hex(0x6C4DFF), hex(0xFF6B6B), hex(0x00C2A8), hex(0xFFC93C), hex(0xFF8FD0)),
            cardBorderWidth = 2.5.dp, cardBorderColor = hex(0x1F1F3D),
            hardShadowOffset = 5.dp, hardShadowColor = hex(0xFFC93C),
            success = hex(0x00A88F),
            animated = true
        )
    )

    val Magma = AppThemeSpec(
        id = "magma", name = "Magma", tagline = "Brasa viva subindo do chão",
        category = ThemeCategory.ABSTRACT, isDark = true,
        colors = makeScheme(
            dark = true,
            primary = hex(0xFF6A00), secondary = hex(0xFFB000), tertiary = hex(0xFF3B30),
            background = hex(0x0E0403), surface = hex(0x1A0705), onSurface = hex(0xFFEBDD),
            cardColor = hex(0x241008), onPrimary = hex(0x1A0800)
        ),
        typography = makeTypography(FontFamily.SansSerif, FontFamily.SansSerif, FontWeight.ExtraBold, (-0.2).sp),
        shapes = roundedShapes(10.dp, 14.dp, 18.dp),
        extras = ThemeExtras(
            homeLayout = HomeLayout.ORBIT,
            workoutLayout = WorkoutLayout.FOCUS,
            decor = Decor.EMBERS,
            backgroundGradient = listOf(hex(0x0E0403), hex(0x1A0604), hex(0x3A0F06)),
            decorColors = listOf(hex(0xFF6A00), hex(0xFFB000), hex(0xFF3B30)),
            cardBorderWidth = 1.dp, cardBorderColor = hex(0xFF6A00).copy(alpha = 0.35f),
            glowColor = hex(0xFF6A00).copy(alpha = 0.45f),
            success = hex(0xFFD23F),
            animated = true
        )
    )

    /** Ordem = ordem de exibição dentro de cada categoria. */
    val all: List<AppThemeSpec> = listOf(
        System, Studio, Carbon, Oceano, Tinta,
        Papel, Blueprint, Brutal, Neon, Terminal,
        Aurora, Sunset, Confete, Magma
    )

    val available: List<AppThemeSpec> get() = all.filter { it.enabled }

    fun byId(id: String?): AppThemeSpec =
        available.firstOrNull { it.id == id } ?: available.firstOrNull { it.id == DEFAULT_ID } ?: Carbon
}
