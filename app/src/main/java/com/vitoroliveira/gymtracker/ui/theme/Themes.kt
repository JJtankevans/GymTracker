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
        typography = makeTypography(ThemeFonts.Inter, ThemeFonts.Inter, FontWeight.SemiBold),
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
        typography = makeTypography(ThemeFonts.SpaceGrotesk, ThemeFonts.Inter, FontWeight.ExtraBold, (-0.3).sp),
        shapes = roundedShapes(8.dp, 12.dp, 14.dp),
        extras = ThemeExtras(
            homeLayout = HomeLayout.BENTO,
            cardBorderWidth = 1.dp, cardBorderColor = hex(0x2A2E34),
            success = hex(0xC6F432)
        )
    )

    val Oceano = AppThemeSpec(
        id = "oceano", name = "Oceano", tagline = "Azul-marinho profundo, ondas e bolhas subindo",
        category = ThemeCategory.PRO, isDark = true,
        colors = makeScheme(
            dark = true,
            primary = hex(0x38BDF8), secondary = hex(0x818CF8), tertiary = hex(0x2DD4BF),
            background = hex(0x08111F), surface = hex(0x0F1B2E), onSurface = hex(0xE6EEF9),
            cardColor = hex(0x14243B)
        ),
        typography = makeTypography(ThemeFonts.Inter, ThemeFonts.Inter, FontWeight.SemiBold),
        shapes = roundedShapes(10.dp, 14.dp, 20.dp),
        extras = ThemeExtras(
            homeLayout = HomeLayout.ORBIT,
            workoutLayout = WorkoutLayout.FOCUS,
            decor = Decor.WAVES,
            decorColors = listOf(hex(0x38BDF8), hex(0x2DD4BF), hex(0x818CF8)),
            backgroundGradient = listOf(hex(0x0B1730), hex(0x07101D), hex(0x041526)),
            cardBorderWidth = 1.dp, cardBorderColor = hex(0x1F3556),
            success = hex(0x2DD4BF),
            animated = true
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
        typography = makeTypography(ThemeFonts.Inter, ThemeFonts.Inter, FontWeight.Medium, (-0.4).sp),
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
        typography = makeTypography(ThemeFonts.Playfair, ThemeFonts.Inter, FontWeight.Bold, (-0.5).sp, headingFeatures = "lnum"),
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
        typography = makeTypography(ThemeFonts.Mono, ThemeFonts.Inter, FontWeight.Bold),
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
        typography = makeTypography(ThemeFonts.SpaceGrotesk, ThemeFonts.Inter, FontWeight.Black, (-0.6).sp, labelWeight = FontWeight.Bold),
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
        typography = makeTypography(ThemeFonts.Mono, ThemeFonts.Inter, FontWeight.Bold, 0.5.sp),
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
        typography = makeTypography(ThemeFonts.Mono, ThemeFonts.Mono, FontWeight.Bold, labelWeight = FontWeight.Bold),
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
            cardColor = Color.White.copy(alpha = 0.18f), onPrimary = hex(0x00201A),
            topBar = Color.Transparent
        ),
        typography = makeTypography(ThemeFonts.SpaceGrotesk, ThemeFonts.Inter, FontWeight.SemiBold, (-0.2).sp),
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
            cardColor = hex(0x140A33).copy(alpha = 0.88f), topBar = Color.Transparent
        ),
        typography = makeTypography(ThemeFonts.SpaceGrotesk, ThemeFonts.Inter, FontWeight.ExtraBold, 0.2.sp),
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
        typography = makeTypography(ThemeFonts.SpaceGrotesk, ThemeFonts.Inter, FontWeight.ExtraBold, (-0.3).sp),
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
        typography = makeTypography(ThemeFonts.BarlowCondensed, ThemeFonts.Inter, FontWeight.ExtraBold, (-0.2).sp),
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

    // ───────────────────── NOVOS: contidos ─────────────────────


    val Nordico = AppThemeSpec(
        id = "nordico", name = "Nórdico", tagline = "Cinza quente, verde-sálvia, muito respiro",
        category = ThemeCategory.PRO, isDark = false,
        colors = makeScheme(
            dark = false,
            primary = hex(0x2F5D50), secondary = hex(0x6B8F82), tertiary = hex(0xC8873A),
            background = hex(0xF3F4F0), surface = hex(0xFFFFFF), onSurface = hex(0x1D2623),
            outline = hex(0xB9C1BB), cardColor = hex(0xFFFFFF)
        ),
        typography = makeTypography(ThemeFonts.Inter, ThemeFonts.Inter, FontWeight.Light, (-0.2).sp, labelWeight = FontWeight.Medium),
        shapes = roundedShapes(12.dp, 18.dp, 26.dp),
        extras = ThemeExtras(
            cardBorderWidth = 1.dp, cardBorderColor = hex(0xE1E5DF),
            success = hex(0x2F8F5B)
        )
    )

    val Obsidiana = AppThemeSpec(
        id = "obsidiana", name = "Obsidiana", tagline = "Preto total, branco puro, linhas finas",
        category = ThemeCategory.PRO, isDark = true,
        colors = makeScheme(
            dark = true,
            primary = hex(0xF5F5F5), secondary = hex(0xA1A1AA), tertiary = hex(0x22D3EE),
            background = hex(0x000000), surface = hex(0x0A0A0A), onSurface = hex(0xF2F2F2),
            outline = hex(0x3A3A3A), cardColor = hex(0x111111), onPrimary = hex(0x000000)
        ),
        typography = makeTypography(ThemeFonts.SpaceGrotesk, ThemeFonts.Inter, FontWeight.Medium, (-0.4).sp),
        shapes = roundedShapes(6.dp, 10.dp, 14.dp),
        extras = ThemeExtras(
            homeLayout = HomeLayout.BENTO,
            workoutLayout = WorkoutLayout.FOCUS,
            decor = Decor.DOTS,
            decorColors = listOf(Color.White),
            cardBorderWidth = 1.dp, cardBorderColor = hex(0x262626),
            success = hex(0x22D3EE)
        )
    )

    val Cobalto = AppThemeSpec(
        id = "cobalto", name = "Cobalto", tagline = "Azul-aço com luz suave no topo",
        category = ThemeCategory.PRO, isDark = true,
        colors = makeScheme(
            dark = true,
            primary = hex(0x6EA0FF), secondary = hex(0x9DB8E8), tertiary = hex(0xF2B84B),
            background = hex(0x0E1420), surface = hex(0x141C2B), onSurface = hex(0xE8EEF9),
            cardColor = hex(0x1A2438), onPrimary = hex(0x06142E)
        ),
        typography = makeTypography(ThemeFonts.Inter, ThemeFonts.Inter, FontWeight.SemiBold, (-0.2).sp),
        shapes = roundedShapes(10.dp, 16.dp, 22.dp),
        extras = ThemeExtras(
            homeLayout = HomeLayout.ORBIT,
            decor = Decor.HALO,
            decorColors = listOf(hex(0x4D86FF)),
            cardBorderWidth = 1.dp, cardBorderColor = hex(0x26344F),
            success = hex(0x4ADE9B)
        )
    )

    val Floresta = AppThemeSpec(
        id = "floresta", name = "Floresta", tagline = "Mata à noite: verde-musgo e vaga-lumes",
        category = ThemeCategory.PRO, isDark = true,
        colors = makeScheme(
            dark = true,
            primary = hex(0x7BE0A4), secondary = hex(0x4FBF95), tertiary = hex(0xE9C46A),
            background = hex(0x08120E), surface = hex(0x0F1D17), onSurface = hex(0xE6F2EB),
            cardColor = hex(0x15281F), onPrimary = hex(0x04200F)
        ),
        typography = makeTypography(ThemeFonts.Inter, ThemeFonts.Inter, FontWeight.SemiBold, (-0.1).sp),
        shapes = roundedShapes(10.dp, 16.dp, 24.dp),
        extras = ThemeExtras(
            homeLayout = HomeLayout.ORBIT,
            decor = Decor.FIREFLIES,
            decorColors = listOf(hex(0xD9F99D), hex(0x2FBF7A)),
            backgroundGradient = listOf(hex(0x0B1A14), hex(0x08120E), hex(0x050C09)),
            cardBorderWidth = 1.dp, cardBorderColor = hex(0x1F3A2D),
            success = hex(0x7BE0A4),
            animated = true
        )
    )

    // ───────────────────── NOVOS: impacto ─────────────────────

    val Arena = AppThemeSpec(
        id = "arena", name = "Arena", tagline = "Preto, vermelho e branco. Dia de competição",
        category = ThemeCategory.BOLD, isDark = true,
        colors = makeScheme(
            dark = true,
            primary = hex(0xFF2D2D), secondary = hex(0xF2F2F2), tertiary = hex(0xFFC53D),
            background = hex(0x0B0B0B), surface = hex(0x141414), onSurface = hex(0xFFFFFF),
            outline = hex(0x5A1414), cardColor = hex(0x1A1A1A), onPrimary = hex(0xFFFFFF)
        ),
        typography = makeTypography(ThemeFonts.BarlowCondensed, ThemeFonts.BarlowCondensed, FontWeight.Black, 0.3.sp, labelWeight = FontWeight.Bold),
        shapes = roundedShapes(2.dp, 4.dp, 6.dp),
        extras = ThemeExtras(
            homeLayout = HomeLayout.POSTER,
            workoutLayout = WorkoutLayout.FOCUS,
            decor = Decor.STRIPES,
            decorColors = listOf(hex(0xFF2D2D)),
            cardBorderWidth = 2.dp, cardBorderColor = hex(0xFF2D2D).copy(alpha = 0.8f),
            success = hex(0x35E08A)
        )
    )

    val Ouro = AppThemeSpec(
        id = "ouro", name = "Ouro", tagline = "Art déco: preto fosco, leque dourado e serifa",
        category = ThemeCategory.BOLD, isDark = true,
        colors = makeScheme(
            dark = true,
            primary = hex(0xD8B25A), secondary = hex(0xB89B5E), tertiary = hex(0xF0E2B6),
            background = hex(0x0A0A0B), surface = hex(0x131315), onSurface = hex(0xF3EEE2),
            outline = hex(0x4A3F27), cardColor = hex(0x1A1A1D), onPrimary = hex(0x1C1403)
        ),
        typography = makeTypography(ThemeFonts.Playfair, ThemeFonts.Inter, FontWeight.SemiBold, (-0.2).sp, headingFeatures = "lnum"),
        shapes = roundedShapes(4.dp, 8.dp, 12.dp),
        extras = ThemeExtras(
            homeLayout = HomeLayout.EDITORIAL,
            decor = Decor.DECO,
            decorColors = listOf(hex(0xD8B25A)),
            cardBorderWidth = 1.dp, cardBorderColor = hex(0xD8B25A).copy(alpha = 0.35f),
            success = hex(0xD8B25A)
        )
    )

    val Artico = AppThemeSpec(
        id = "artico", name = "Ártico", tagline = "Gelo, luz azul-turquesa e vidro fosco",
        category = ThemeCategory.ABSTRACT, isDark = true,
        colors = makeScheme(
            dark = true,
            primary = hex(0x9BE7FF), secondary = hex(0xA5B4FC), tertiary = hex(0xE0F7FF),
            background = hex(0x050E1A), surface = hex(0x0B1A2C), onSurface = hex(0xEAF6FF),
            cardColor = Color.White.copy(alpha = 0.16f), onPrimary = hex(0x02202B),
            topBar = Color.Transparent
        ),
        typography = makeTypography(ThemeFonts.SpaceGrotesk, ThemeFonts.Inter, FontWeight.SemiBold, (-0.3).sp),
        shapes = roundedShapes(14.dp, 22.dp, 30.dp),
        extras = ThemeExtras(
            homeLayout = HomeLayout.ORBIT,
            workoutLayout = WorkoutLayout.FOCUS,
            decor = Decor.AURORA,
            decorColors = listOf(hex(0x1FB6FF), hex(0x5EEAD4), hex(0x818CF8)),
            cardBorderWidth = 1.dp, cardBorderColor = Color.White.copy(alpha = 0.22f),
            success = hex(0x5EEAD4),
            animated = true
        )
    )

    // ───────────────────────── LOUCOS ─────────────────────────

    val Matrix = AppThemeSpec(
        id = "matrix", name = "Matrix", tagline = "Chuva de código verde. Não existe colher",
        category = ThemeCategory.CRAZY, isDark = true,
        colors = makeScheme(
            dark = true,
            primary = hex(0x00FF41), secondary = hex(0x9BFFB0), tertiary = hex(0xE8FFE8),
            background = hex(0x000000), surface = hex(0x020A04), onSurface = hex(0xC8FFD4),
            outline = hex(0x0F7A2A), cardColor = hex(0x03140A).copy(alpha = 0.86f), onPrimary = hex(0x001A06)
        ),
        typography = makeTypography(ThemeFonts.Mono, ThemeFonts.Mono, FontWeight.Bold, 0.4.sp, labelWeight = FontWeight.Bold),
        shapes = roundedShapes(0.dp, 2.dp, 2.dp, 4.dp),
        extras = ThemeExtras(
            homeLayout = HomeLayout.TERMINAL,
            workoutLayout = WorkoutLayout.FOCUS,
            decor = Decor.MATRIX,
            decorColors = listOf(hex(0xD5FFE0), hex(0x00FF41)),
            cardBorderWidth = 1.dp, cardBorderColor = hex(0x00FF41).copy(alpha = 0.45f),
            glowColor = hex(0x00FF41).copy(alpha = 0.5f),
            success = hex(0x00FF41),
            animated = true
        )
    )

    val Hipnose = AppThemeSpec(
        id = "hipnose", name = "Hipnose", tagline = "Olhe fixo na espiral... você vai bater a meta",
        category = ThemeCategory.CRAZY, isDark = true,
        colors = makeScheme(
            dark = true,
            primary = hex(0xFF4FD8), secondary = hex(0x6DF2FF), tertiary = hex(0xFFF35C),
            background = hex(0x12001F), surface = hex(0x1C0530), onSurface = hex(0xFFF0FD),
            cardColor = hex(0x1C0530).copy(alpha = 0.82f), onPrimary = hex(0x2A0022),
            topBar = Color.Transparent
        ),
        typography = makeTypography(ThemeFonts.SpaceGrotesk, ThemeFonts.Inter, FontWeight.Black, (-0.4).sp),
        shapes = roundedShapes(18.dp, 26.dp, 34.dp),
        extras = ThemeExtras(
            homeLayout = HomeLayout.ORBIT,
            workoutLayout = WorkoutLayout.FOCUS,
            decor = Decor.SPIRAL,
            decorColors = listOf(hex(0xFF4FD8), hex(0x6DF2FF)),
            cardBorderWidth = 1.5.dp, cardBorderColor = hex(0xFF4FD8).copy(alpha = 0.6f),
            glowColor = hex(0xFF4FD8).copy(alpha = 0.5f),
            success = hex(0x6DF2FF),
            animated = true
        )
    )

    val Hiperespaco = AppThemeSpec(
        id = "hiperespaco", name = "Hiperespaço", tagline = "Estrelas em dobra. Pulando pra próxima série",
        category = ThemeCategory.CRAZY, isDark = true,
        colors = makeScheme(
            dark = true,
            primary = hex(0x9BE7FF), secondary = hex(0xFFE27A), tertiary = hex(0xC4B5FD),
            background = hex(0x02030A), surface = hex(0x080B1A), onSurface = hex(0xEEF4FF),
            cardColor = hex(0x0D1226).copy(alpha = 0.8f), onPrimary = hex(0x001826),
            topBar = Color.Transparent
        ),
        typography = makeTypography(ThemeFonts.SpaceGrotesk, ThemeFonts.Inter, FontWeight.Bold, 1.2.sp),
        shapes = cutShapes(4.dp, 8.dp, 14.dp),
        extras = ThemeExtras(
            homeLayout = HomeLayout.ORBIT,
            workoutLayout = WorkoutLayout.FOCUS,
            decor = Decor.WARP,
            decorColors = listOf(hex(0x9BE7FF)),
            cardBorderWidth = 1.dp, cardBorderColor = hex(0x9BE7FF).copy(alpha = 0.35f),
            success = hex(0xFFE27A),
            animated = true
        )
    )

    val Glitch = AppThemeSpec(
        id = "glitch", name = "Glitch", tagline = "Sinal ruim, treino bom. Tela com defeito de propósito",
        category = ThemeCategory.CRAZY, isDark = true,
        colors = makeScheme(
            dark = true,
            primary = hex(0x00F0FF), secondary = hex(0xFF2BD6), tertiary = hex(0xF4FF3B),
            background = hex(0x0A0A0C), surface = hex(0x121216), onSurface = hex(0xF5F5F5),
            outline = hex(0x5A5A66), cardColor = hex(0x16161C), onPrimary = hex(0x001A1C)
        ),
        typography = makeTypography(ThemeFonts.Mono, ThemeFonts.Inter, FontWeight.ExtraBold, (-0.2).sp, labelWeight = FontWeight.Bold),
        shapes = roundedShapes(0.dp, 0.dp, 0.dp, 0.dp),
        extras = ThemeExtras(
            homeLayout = HomeLayout.BENTO,
            workoutLayout = WorkoutLayout.FOCUS,
            decor = Decor.GLITCH,
            decorColors = listOf(hex(0x00F0FF), hex(0xFF2BD6)),
            cardBorderWidth = 1.dp, cardBorderColor = hex(0xF5F5F5).copy(alpha = 0.5f),
            hardShadowOffset = 3.dp, hardShadowColor = hex(0xFF2BD6).copy(alpha = 0.75f),
            success = hex(0xF4FF3B),
            animated = true
        )
    )

    val Lava = AppThemeSpec(
        id = "lava", name = "Lâmpada de Lava", tagline = "Bolhas quentes subindo devagar. Anos 70",
        category = ThemeCategory.CRAZY, isDark = true,
        colors = makeScheme(
            dark = true,
            primary = hex(0xFF7A3D), secondary = hex(0xFF3D9A), tertiary = hex(0xFFD166),
            background = hex(0x1A0626), surface = hex(0x240A33), onSurface = hex(0xFFF1E8),
            cardColor = hex(0x2A0C3A).copy(alpha = 0.8f), onPrimary = hex(0x2A0A00),
            topBar = Color.Transparent
        ),
        typography = makeTypography(ThemeFonts.Playfair, ThemeFonts.Inter, FontWeight.Black, (-0.3).sp, headingFeatures = "lnum"),
        shapes = roundedShapes(20.dp, 28.dp, 36.dp),
        extras = ThemeExtras(
            homeLayout = HomeLayout.POSTER,
            workoutLayout = WorkoutLayout.FOCUS,
            decor = Decor.LAVA,
            backgroundGradient = listOf(hex(0x1A0626), hex(0x2B0838), hex(0x3D0A2E)),
            decorColors = listOf(hex(0xFF7A3D), hex(0xFF3D9A), hex(0xFFB13D)),
            cardBorderWidth = 1.dp, cardBorderColor = hex(0xFF7A3D).copy(alpha = 0.4f),
            success = hex(0xFFD166),
            animated = true
        )
    )

    val Arcade = AppThemeSpec(
        id = "arcade", name = "Arcade", tagline = "8 bits: invasores marchando, insira uma ficha",
        category = ThemeCategory.CRAZY, isDark = true,
        colors = makeScheme(
            dark = true,
            primary = hex(0xFFD300), secondary = hex(0x00E0FF), tertiary = hex(0xFF3EA5),
            background = hex(0x07071A), surface = hex(0x0E0E2A), onSurface = hex(0xFFFFFF),
            outline = hex(0xFFFFFF), cardColor = hex(0x12123A), onPrimary = hex(0x000000)
        ),
        typography = makeTypography(ThemeFonts.Mono, ThemeFonts.Mono, FontWeight.Black, 1.sp, labelWeight = FontWeight.Bold),
        shapes = roundedShapes(0.dp, 0.dp, 0.dp, 0.dp),
        extras = ThemeExtras(
            homeLayout = HomeLayout.POSTER,
            workoutLayout = WorkoutLayout.FOCUS,
            decor = Decor.ARCADE,
            decorColors = listOf(hex(0x39FF14), hex(0xFF3EA5), hex(0x00E0FF)),
            cardBorderWidth = 3.dp, cardBorderColor = hex(0xFFFFFF),
            hardShadowOffset = 4.dp, hardShadowColor = hex(0xFF3EA5),
            success = hex(0x39FF14),
            animated = true
        )
    )

    val Carnaval = AppThemeSpec(
        id = "carnaval", name = "Carnaval", tagline = "Confete, serpentina e lantejoula. Bloco do shape",
        category = ThemeCategory.CRAZY, isDark = true,
        colors = makeScheme(
            dark = true,
            primary = hex(0xFFC400), secondary = hex(0x00D26A), tertiary = hex(0xFF2E93),
            background = hex(0x1E0638), surface = hex(0x2A0A4A), onSurface = hex(0xFFF7E6),
            cardColor = hex(0x2A0A4A).copy(alpha = 0.88f), onPrimary = hex(0x2A1A00)
        ),
        typography = makeTypography(ThemeFonts.BarlowCondensed, ThemeFonts.Inter, FontWeight.Black, 0.4.sp, labelWeight = FontWeight.Bold),
        shapes = roundedShapes(10.dp, 16.dp, 22.dp),
        extras = ThemeExtras(
            homeLayout = HomeLayout.POSTER,
            workoutLayout = WorkoutLayout.FOCUS,
            decor = Decor.CARNIVAL,
            backgroundGradient = listOf(hex(0x1E0638), hex(0x34095C), hex(0x4A0A4F)),
            decorColors = listOf(hex(0xFFC400), hex(0x00D26A), hex(0xFF2E93), hex(0x2E9BFF)),
            cardBorderWidth = 2.dp, cardBorderColor = hex(0xFFC400).copy(alpha = 0.7f),
            glowColor = hex(0xFF2E93).copy(alpha = 0.45f),
            success = hex(0x00D26A),
            animated = true
        )
    )

    // ───────────── LOUCOS, segunda leva (28/10/2026) ─────────────

    val Tempestade = AppThemeSpec(
        id = "tempestade", name = "Tempestade", tagline = "Raio caindo a cada série. Treina na chuva",
        category = ThemeCategory.CRAZY, isDark = true,
        colors = makeScheme(
            dark = true,
            primary = hex(0x7CC4FF), secondary = hex(0xFFE45C), tertiary = hex(0xB9C6D9),
            background = hex(0x0A0F1A), surface = hex(0x111827), onSurface = hex(0xE8F0FF),
            cardColor = hex(0x131C2C).copy(alpha = 0.86f), onPrimary = hex(0x001A33),
            topBar = Color.Transparent
        ),
        typography = makeTypography(ThemeFonts.BarlowCondensed, ThemeFonts.Inter, FontWeight.Black, 0.6.sp, labelWeight = FontWeight.Bold),
        shapes = cutShapes(4.dp, 10.dp, 16.dp),
        extras = ThemeExtras(
            homeLayout = HomeLayout.BENTO,
            workoutLayout = WorkoutLayout.FOCUS,
            decor = Decor.STORM,
            backgroundGradient = listOf(hex(0x0A0F1A), hex(0x111A2B), hex(0x1A2436)),
            decorColors = listOf(hex(0xBFE3FF)),
            cardBorderWidth = 1.dp, cardBorderColor = hex(0x7CC4FF).copy(alpha = 0.35f),
            glowColor = hex(0xFFE45C).copy(alpha = 0.25f),
            success = hex(0xFFE45C),
            animated = true
        )
    )

    val Discoteca = AppThemeSpec(
        id = "discoteca", name = "Discoteca", tagline = "Bola espelhada girando. Supino com groove",
        category = ThemeCategory.CRAZY, isDark = true,
        colors = makeScheme(
            dark = true,
            primary = hex(0xFF3DCB), secondary = hex(0x3DDCFF), tertiary = hex(0xFFE03D),
            background = hex(0x14021F), surface = hex(0x1F0630), onSurface = hex(0xFFF0FB),
            cardColor = hex(0x22073A).copy(alpha = 0.85f), onPrimary = hex(0x2A0020),
            topBar = Color.Transparent
        ),
        typography = makeTypography(ThemeFonts.Playfair, ThemeFonts.Inter, FontWeight.Black, (-0.2).sp, headingFeatures = "lnum"),
        shapes = roundedShapes(16.dp, 24.dp, 32.dp),
        extras = ThemeExtras(
            homeLayout = HomeLayout.POSTER,
            workoutLayout = WorkoutLayout.FOCUS,
            decor = Decor.DISCO,
            decorColors = listOf(hex(0xFF3DCB), hex(0x3DDCFF), hex(0xFFE03D), hex(0x7CFF3D)),
            cardBorderWidth = 1.5.dp, cardBorderColor = hex(0xFFE03D).copy(alpha = 0.55f),
            glowColor = hex(0xFF3DCB).copy(alpha = 0.45f),
            success = hex(0x7CFF3D),
            animated = true
        )
    )

    val Caleidoscopio = AppThemeSpec(
        id = "caleidoscopio", name = "Caleidoscópio", tagline = "Arco-íris girando sem parar. Tonteira controlada",
        category = ThemeCategory.CRAZY, isDark = true,
        colors = makeScheme(
            dark = true,
            primary = hex(0xFFB84D), secondary = hex(0x4DB8FF), tertiary = hex(0xB84DFF),
            background = hex(0x0B0712), surface = hex(0x140D1F), onSurface = hex(0xFFF8EE),
            cardColor = hex(0x140D1F).copy(alpha = 0.84f), onPrimary = hex(0x2A1500),
            topBar = Color.Transparent
        ),
        typography = makeTypography(ThemeFonts.SpaceGrotesk, ThemeFonts.Inter, FontWeight.Black, (-0.5).sp),
        shapes = roundedShapes(12.dp, 20.dp, 28.dp),
        extras = ThemeExtras(
            homeLayout = HomeLayout.ORBIT,
            workoutLayout = WorkoutLayout.FOCUS,
            decor = Decor.KALEIDO,
            decorColors = listOf(hex(0xFF4D4D), hex(0xFFB84D), hex(0xFFF04D), hex(0x4DFF88), hex(0x4DB8FF), hex(0xB84DFF)),
            cardBorderWidth = 1.5.dp, cardBorderColor = Color.White.copy(alpha = 0.35f),
            success = hex(0x4DFF88),
            animated = true
        )
    )

    val Abissal = AppThemeSpec(
        id = "abissal", name = "Abissal", tagline = "Fundo do mar, águas-vivas brilhando no escuro",
        category = ThemeCategory.CRAZY, isDark = true,
        colors = makeScheme(
            dark = true,
            primary = hex(0x6CF6FF), secondary = hex(0xFF6CF0), tertiary = hex(0xA5B4FC),
            background = hex(0x00050F), surface = hex(0x030C1C), onSurface = hex(0xE6FBFF),
            cardColor = hex(0x061428).copy(alpha = 0.8f), onPrimary = hex(0x00242A),
            topBar = Color.Transparent
        ),
        typography = makeTypography(ThemeFonts.Inter, ThemeFonts.Inter, FontWeight.Light, 1.5.sp),
        shapes = roundedShapes(20.dp, 28.dp, 36.dp),
        extras = ThemeExtras(
            homeLayout = HomeLayout.ORBIT,
            workoutLayout = WorkoutLayout.FOCUS,
            decor = Decor.ABYSS,
            backgroundGradient = listOf(hex(0x02142A), hex(0x010A18), hex(0x00030A)),
            decorColors = listOf(hex(0x6CF6FF), hex(0xFF6CF0)),
            cardBorderWidth = 1.dp, cardBorderColor = hex(0x6CF6FF).copy(alpha = 0.35f),
            glowColor = hex(0x6CF6FF).copy(alpha = 0.35f),
            success = hex(0x6CF6FF),
            animated = true
        )
    )

    val Galaxia = AppThemeSpec(
        id = "galaxia", name = "Galáxia", tagline = "Braços de estrelas girando. Shape interestelar",
        category = ThemeCategory.CRAZY, isDark = true,
        colors = makeScheme(
            dark = true,
            primary = hex(0x9D8CFF), secondary = hex(0xFFD6A5), tertiary = hex(0xFF7EB6),
            background = hex(0x05030F), surface = hex(0x0C0820), onSurface = hex(0xF3EEFF),
            cardColor = hex(0x110C2A).copy(alpha = 0.8f), onPrimary = hex(0x120A33),
            topBar = Color.Transparent
        ),
        typography = makeTypography(ThemeFonts.SpaceGrotesk, ThemeFonts.Inter, FontWeight.Bold, 2.sp),
        shapes = roundedShapes(14.dp, 22.dp, 30.dp),
        extras = ThemeExtras(
            homeLayout = HomeLayout.ORBIT,
            workoutLayout = WorkoutLayout.FOCUS,
            decor = Decor.GALAXY,
            decorColors = listOf(hex(0xFFD6A5), hex(0x9D8CFF), hex(0xFF7EB6)),
            cardBorderWidth = 1.dp, cardBorderColor = hex(0x9D8CFF).copy(alpha = 0.4f),
            glowColor = hex(0x9D8CFF).copy(alpha = 0.35f),
            success = hex(0xFFD6A5),
            animated = true
        )
    )

    val ForaDoAr = AppThemeSpec(
        id = "fora_do_ar", name = "Fora do Ar", tagline = "Chuvisco de TV antiga. Sintonize o treino",
        category = ThemeCategory.CRAZY, isDark = true,
        colors = makeScheme(
            dark = true,
            primary = hex(0xF2F2F2), secondary = hex(0xFFE600), tertiary = hex(0x00E5FF),
            background = hex(0x0B0B0B), surface = hex(0x141414), onSurface = hex(0xF5F5F5),
            outline = hex(0x8A8A8A), cardColor = hex(0x161616).copy(alpha = 0.9f), onPrimary = hex(0x000000)
        ),
        typography = makeTypography(ThemeFonts.Mono, ThemeFonts.Mono, FontWeight.ExtraBold, 0.8.sp, labelWeight = FontWeight.Bold),
        shapes = roundedShapes(2.dp, 4.dp, 6.dp, 8.dp),
        extras = ThemeExtras(
            homeLayout = HomeLayout.TERMINAL,
            workoutLayout = WorkoutLayout.FOCUS,
            decor = Decor.STATIC,
            decorColors = listOf(hex(0xF2F2F2), hex(0xFFE600), hex(0x00E5FF), hex(0x00E676), hex(0xFF00E6), hex(0xFF1744), hex(0x2962FF)),
            cardBorderWidth = 2.dp, cardBorderColor = hex(0xF2F2F2).copy(alpha = 0.6f),
            hardShadowOffset = 3.dp, hardShadowColor = Color.Black,
            success = hex(0xFFE600),
            animated = true
        )
    )

    val Holografico = AppThemeSpec(
        id = "holografico", name = "Holográfico", tagline = "Brilho perolado de figurinha rara. Claro e cintilante",
        category = ThemeCategory.CRAZY, isDark = false,
        colors = makeScheme(
            dark = false,
            primary = hex(0x6D3BFF), secondary = hex(0x00A6B8), tertiary = hex(0xE6008A),
            background = hex(0xF6F2FF), surface = hex(0xFFFFFF), onSurface = hex(0x1B1530),
            cardColor = Color.White.copy(alpha = 0.72f), topBar = Color.Transparent
        ),
        typography = makeTypography(ThemeFonts.SpaceGrotesk, ThemeFonts.Inter, FontWeight.Bold, (-0.3).sp),
        shapes = roundedShapes(18.dp, 26.dp, 34.dp),
        extras = ThemeExtras(
            homeLayout = HomeLayout.EDITORIAL,
            workoutLayout = WorkoutLayout.FOCUS,
            decor = Decor.HOLO,
            decorColors = listOf(hex(0xFFB3E6), hex(0xB3E5FF), hex(0xC8FFD9), hex(0xFFF3B3), hex(0xD9B3FF)),
            cardBorderWidth = 1.5.dp, cardBorderColor = Color.White,
            success = hex(0x00A86B),
            animated = true
        )
    )

    val Assombrado = AppThemeSpec(
        id = "assombrado", name = "Assombrado", tagline = "Lua cheia, morcegos e neblina. Treino de arrepiar",
        category = ThemeCategory.CRAZY, isDark = true,
        colors = makeScheme(
            dark = true,
            primary = hex(0xFF8A1F), secondary = hex(0xA78BFA), tertiary = hex(0x9DFF6C),
            background = hex(0x0B0716), surface = hex(0x140D24), onSurface = hex(0xF4EEFF),
            cardColor = hex(0x160E28).copy(alpha = 0.86f), onPrimary = hex(0x2A1200),
            topBar = Color.Transparent
        ),
        typography = makeTypography(ThemeFonts.Playfair, ThemeFonts.Inter, FontWeight.Black, 0.3.sp, headingFeatures = "lnum"),
        shapes = cutShapes(6.dp, 12.dp, 18.dp),
        extras = ThemeExtras(
            homeLayout = HomeLayout.POSTER,
            workoutLayout = WorkoutLayout.FOCUS,
            decor = Decor.HAUNTED,
            backgroundGradient = listOf(hex(0x1A1033), hex(0x0F0920), hex(0x07040F)),
            decorColors = listOf(hex(0xFFF1C2), hex(0x8A7CC2)),
            cardBorderWidth = 1.dp, cardBorderColor = hex(0xFF8A1F).copy(alpha = 0.45f),
            glowColor = hex(0xFF8A1F).copy(alpha = 0.3f),
            success = hex(0x9DFF6C),
            animated = true
        )
    )

    val Reveillon = AppThemeSpec(
        id = "reveillon", name = "Réveillon", tagline = "Fogos estourando o tempo todo. Cada série é ano novo",
        category = ThemeCategory.CRAZY, isDark = true,
        colors = makeScheme(
            dark = true,
            primary = hex(0xFFD447), secondary = hex(0xFF4D8D), tertiary = hex(0x4DD2FF),
            background = hex(0x050816), surface = hex(0x0B1026), onSurface = hex(0xFFF9E8),
            cardColor = hex(0x0E1430).copy(alpha = 0.84f), onPrimary = hex(0x2A2000),
            topBar = Color.Transparent
        ),
        typography = makeTypography(ThemeFonts.BarlowCondensed, ThemeFonts.Inter, FontWeight.Black, 1.sp, labelWeight = FontWeight.Bold),
        shapes = roundedShapes(10.dp, 18.dp, 26.dp),
        extras = ThemeExtras(
            homeLayout = HomeLayout.POSTER,
            workoutLayout = WorkoutLayout.FOCUS,
            decor = Decor.FIREWORKS,
            backgroundGradient = listOf(hex(0x050816), hex(0x0A1030), hex(0x1A0F33)),
            decorColors = listOf(hex(0xFFD447), hex(0xFF4D8D), hex(0x4DD2FF), hex(0x9DFF4D)),
            cardBorderWidth = 1.5.dp, cardBorderColor = hex(0xFFD447).copy(alpha = 0.6f),
            glowColor = hex(0xFFD447).copy(alpha = 0.3f),
            success = hex(0x9DFF4D),
            animated = true
        )
    )

    val Blocos = AppThemeSpec(
        id = "blocos", name = "Blocos", tagline = "Peças caindo e encaixando. Fecha a linha, fecha a série",
        category = ThemeCategory.CRAZY, isDark = true,
        colors = makeScheme(
            dark = true,
            primary = hex(0x00E5FF), secondary = hex(0xFFE600), tertiary = hex(0xB000FF),
            background = hex(0x0A0A1F), surface = hex(0x12122E), onSurface = hex(0xFFFFFF),
            outline = hex(0x5A5A9A), cardColor = hex(0x14143A).copy(alpha = 0.9f), onPrimary = hex(0x001A1F)
        ),
        typography = makeTypography(ThemeFonts.Mono, ThemeFonts.Inter, FontWeight.Black, 0.5.sp, labelWeight = FontWeight.Bold),
        shapes = roundedShapes(0.dp, 0.dp, 0.dp, 0.dp),
        extras = ThemeExtras(
            homeLayout = HomeLayout.BENTO,
            workoutLayout = WorkoutLayout.FOCUS,
            decor = Decor.BLOCKS,
            decorColors = listOf(hex(0x00E5FF), hex(0xFFE600), hex(0xB000FF), hex(0xFF8A00), hex(0x0057FF), hex(0x00E676), hex(0xFF1744)),
            cardBorderWidth = 2.dp, cardBorderColor = hex(0x00E5FF).copy(alpha = 0.7f),
            hardShadowOffset = 4.dp, hardShadowColor = hex(0xB000FF).copy(alpha = 0.8f),
            success = hex(0x00E676),
            animated = true
        )
    )

    val Cardio = AppThemeSpec(
        id = "cardio", name = "Cardio", tagline = "Monitor cardíaco batendo. Treino na UTI do shape",
        category = ThemeCategory.CRAZY, isDark = true,
        colors = makeScheme(
            dark = true,
            primary = hex(0x39FF88), secondary = hex(0xFF3B5C), tertiary = hex(0x7DD3FC),
            background = hex(0x020A06), surface = hex(0x06140C), onSurface = hex(0xDFFFEA),
            outline = hex(0x1F7A45), cardColor = hex(0x071A10).copy(alpha = 0.88f), onPrimary = hex(0x00220F)
        ),
        typography = makeTypography(ThemeFonts.BarlowCondensed, ThemeFonts.Mono, FontWeight.Black, 0.8.sp, labelWeight = FontWeight.Bold),
        shapes = roundedShapes(4.dp, 8.dp, 12.dp),
        extras = ThemeExtras(
            homeLayout = HomeLayout.TERMINAL,
            workoutLayout = WorkoutLayout.FOCUS,
            decor = Decor.ECG,
            decorColors = listOf(hex(0x39FF88)),
            cardBorderWidth = 1.dp, cardBorderColor = hex(0x39FF88).copy(alpha = 0.45f),
            glowColor = hex(0x39FF88).copy(alpha = 0.35f),
            success = hex(0x39FF88),
            animated = true
        )
    )

    val Rave = AppThemeSpec(
        id = "rave", name = "Rave", tagline = "Lasers cortando a fumaça. O grave bate no peito",
        category = ThemeCategory.CRAZY, isDark = true,
        colors = makeScheme(
            dark = true,
            primary = hex(0x39FF14), secondary = hex(0xFF00E6), tertiary = hex(0x00F0FF),
            background = hex(0x050505), surface = hex(0x0D0D0D), onSurface = hex(0xF7F7F7),
            cardColor = hex(0x101010).copy(alpha = 0.85f), onPrimary = hex(0x041A00),
            topBar = Color.Transparent
        ),
        typography = makeTypography(ThemeFonts.SpaceGrotesk, ThemeFonts.Inter, FontWeight.Black, (-0.6).sp),
        shapes = cutShapes(6.dp, 12.dp, 20.dp),
        extras = ThemeExtras(
            homeLayout = HomeLayout.POSTER,
            workoutLayout = WorkoutLayout.FOCUS,
            decor = Decor.LASERS,
            decorColors = listOf(hex(0x39FF14), hex(0xFF00E6), hex(0x00F0FF)),
            cardBorderWidth = 1.5.dp, cardBorderColor = hex(0x39FF14).copy(alpha = 0.55f),
            glowColor = hex(0xFF00E6).copy(alpha = 0.4f),
            success = hex(0x39FF14),
            animated = true
        )
    )

    // ───────────── LOUCOS, terceira leva (28/10/2026, madrugada) ─────────────

    val PlacaMae = AppThemeSpec(
        id = "placa_mae", name = "Placa-Mãe", tagline = "Energia correndo pelas trilhas. Overclock no shape",
        category = ThemeCategory.CRAZY, isDark = true,
        colors = makeScheme(
            dark = true,
            primary = hex(0x00E08A), secondary = hex(0xB6FF3B), tertiary = hex(0x3BC9FF),
            background = hex(0x03140C), surface = hex(0x062014), onSurface = hex(0xE3FFF0),
            outline = hex(0x1C6B45), cardColor = hex(0x072618).copy(alpha = 0.9f), onPrimary = hex(0x00240F)
        ),
        typography = makeTypography(ThemeFonts.Mono, ThemeFonts.Inter, FontWeight.Bold, 0.5.sp, labelWeight = FontWeight.Bold),
        shapes = cutShapes(3.dp, 6.dp, 10.dp),
        extras = ThemeExtras(
            homeLayout = HomeLayout.BENTO,
            workoutLayout = WorkoutLayout.FOCUS,
            decor = Decor.CIRCUIT,
            decorColors = listOf(hex(0x00E08A), hex(0xB6FF3B)),
            cardBorderWidth = 1.dp, cardBorderColor = hex(0x00E08A).copy(alpha = 0.5f),
            glowColor = hex(0x00E08A).copy(alpha = 0.3f),
            success = hex(0xB6FF3B),
            animated = true
        )
    )

    val Nevasca = AppThemeSpec(
        id = "nevasca", name = "Nevasca", tagline = "Floco por floco, série por série. Treino no gelo",
        category = ThemeCategory.CRAZY, isDark = true,
        colors = makeScheme(
            dark = true,
            primary = hex(0x9AD8FF), secondary = hex(0xE0F4FF), tertiary = hex(0xFFB3C7),
            background = hex(0x0A1626), surface = hex(0x102036), onSurface = hex(0xF2F9FF),
            cardColor = hex(0x13263F).copy(alpha = 0.82f), onPrimary = hex(0x002238),
            topBar = Color.Transparent
        ),
        typography = makeTypography(ThemeFonts.SpaceGrotesk, ThemeFonts.Inter, FontWeight.Bold, 0.8.sp),
        shapes = roundedShapes(16.dp, 24.dp, 32.dp),
        extras = ThemeExtras(
            homeLayout = HomeLayout.ORBIT,
            workoutLayout = WorkoutLayout.FOCUS,
            decor = Decor.SNOW,
            backgroundGradient = listOf(hex(0x15304F), hex(0x0E1F36), hex(0x08121F)),
            decorColors = listOf(hex(0xBDE7FF)),
            cardBorderWidth = 1.dp, cardBorderColor = Color.White.copy(alpha = 0.3f),
            success = hex(0x9AD8FF),
            animated = true
        )
    )

    val Bolhas = AppThemeSpec(
        id = "bolhas", name = "Bolhas", tagline = "Bolhas de sabão furta-cor subindo. Leveza no pós-treino",
        category = ThemeCategory.CRAZY, isDark = true,
        colors = makeScheme(
            dark = true,
            primary = hex(0x9AE6FF), secondary = hex(0xFF9AE6), tertiary = hex(0xFFF09A),
            background = hex(0x10101E), surface = hex(0x181830), onSurface = hex(0xF6F4FF),
            cardColor = hex(0x1B1B36).copy(alpha = 0.8f), onPrimary = hex(0x00222E),
            topBar = Color.Transparent
        ),
        typography = makeTypography(ThemeFonts.Inter, ThemeFonts.Inter, FontWeight.SemiBold, 0.2.sp),
        shapes = roundedShapes(24.dp, 32.dp, 40.dp),
        extras = ThemeExtras(
            homeLayout = HomeLayout.ORBIT,
            workoutLayout = WorkoutLayout.FOCUS,
            decor = Decor.BUBBLES,
            decorColors = listOf(hex(0xFF9AE6), hex(0x9AE6FF), hex(0xFFF09A)),
            cardBorderWidth = 1.dp, cardBorderColor = hex(0x9AE6FF).copy(alpha = 0.4f),
            success = hex(0x9AFFC8),
            animated = true
        )
    )

    val Miragem = AppThemeSpec(
        id = "miragem", name = "Miragem", tagline = "Sol a pino e dunas tremendo de calor. Cardio no deserto",
        category = ThemeCategory.CRAZY, isDark = true,
        colors = makeScheme(
            dark = true,
            primary = hex(0xFFB347), secondary = hex(0xFF6B4A), tertiary = hex(0xFFE08A),
            background = hex(0x2A1408), surface = hex(0x341A0C), onSurface = hex(0xFFF3E6),
            cardColor = hex(0x3A1D0D).copy(alpha = 0.82f), onPrimary = hex(0x2A1300),
            topBar = Color.Transparent
        ),
        typography = makeTypography(ThemeFonts.Playfair, ThemeFonts.Inter, FontWeight.Black, 0.2.sp, headingFeatures = "lnum"),
        shapes = roundedShapes(12.dp, 20.dp, 28.dp),
        extras = ThemeExtras(
            homeLayout = HomeLayout.POSTER,
            workoutLayout = WorkoutLayout.FOCUS,
            decor = Decor.DESERT,
            backgroundGradient = listOf(hex(0x5A2410), hex(0x3A170A), hex(0x1E0C05)),
            decorColors = listOf(hex(0xFFB347), hex(0xD9824A)),
            cardBorderWidth = 1.dp, cardBorderColor = hex(0xFFB347).copy(alpha = 0.45f),
            success = hex(0xFFE08A),
            animated = true
        )
    )

    val Origami = AppThemeSpec(
        id = "origami", name = "Origami", tagline = "Pássaros de papel voando sobre papel creme. Claro e leve",
        category = ThemeCategory.CRAZY, isDark = false,
        colors = makeScheme(
            dark = false,
            primary = hex(0xC8432F), secondary = hex(0x2F8F83), tertiary = hex(0x3B5BA9),
            background = hex(0xF6EFE3), surface = hex(0xFFFAF2), onSurface = hex(0x2A2118),
            cardColor = hex(0xFFFAF2).copy(alpha = 0.9f)
        ),
        typography = makeTypography(ThemeFonts.Playfair, ThemeFonts.Inter, FontWeight.Bold, (-0.2).sp, headingFeatures = "lnum"),
        shapes = cutShapes(2.dp, 4.dp, 8.dp),
        extras = ThemeExtras(
            homeLayout = HomeLayout.EDITORIAL,
            workoutLayout = WorkoutLayout.FOCUS,
            decor = Decor.ORIGAMI,
            decorColors = listOf(hex(0xE8604C), hex(0x2F8F83), hex(0xF2B33D), hex(0x3B5BA9)),
            cardBorderWidth = 1.dp, cardBorderColor = hex(0x2A2118).copy(alpha = 0.2f),
            hardShadowOffset = 3.dp, hardShadowColor = hex(0x2A2118).copy(alpha = 0.15f),
            success = hex(0x2F8F83),
            animated = true
        )
    )

    val CidadeNeon = AppThemeSpec(
        id = "cidade_neon", name = "Cidade Neon", tagline = "Prédios acesos, letreiros e carros voadores. 2077 na academia",
        category = ThemeCategory.CRAZY, isDark = true,
        colors = makeScheme(
            dark = true,
            primary = hex(0xFF2E88), secondary = hex(0x2EE6FF), tertiary = hex(0xFFD12E),
            background = hex(0x0B0518), surface = hex(0x140A26), onSurface = hex(0xFFF0F8),
            cardColor = hex(0x170B2E).copy(alpha = 0.86f), onPrimary = hex(0x2A0016),
            topBar = Color.Transparent
        ),
        typography = makeTypography(ThemeFonts.BarlowCondensed, ThemeFonts.Inter, FontWeight.Black, 1.2.sp, labelWeight = FontWeight.Bold),
        shapes = cutShapes(4.dp, 10.dp, 16.dp),
        extras = ThemeExtras(
            homeLayout = HomeLayout.POSTER,
            workoutLayout = WorkoutLayout.FOCUS,
            decor = Decor.NEON_CITY,
            backgroundGradient = listOf(hex(0x1A0838), hex(0x10061F), hex(0x07030E)),
            decorColors = listOf(hex(0xFF2E88), hex(0x2EE6FF), hex(0xFFD12E)),
            cardBorderWidth = 1.5.dp, cardBorderColor = hex(0x2EE6FF).copy(alpha = 0.55f),
            glowColor = hex(0xFF2E88).copy(alpha = 0.4f),
            success = hex(0x2EE6FF),
            animated = true
        )
    )

    /** Ordem = ordem de exibição dentro de cada categoria. */
    val all: List<AppThemeSpec> = listOf(
        System, Studio, Carbon, Oceano, Tinta, Nordico, Obsidiana, Cobalto, Floresta,
        Papel, Blueprint, Brutal, Neon, Terminal, Arena, Ouro,
        Aurora, Sunset, Confete, Magma, Artico,
        Matrix, Hipnose, Hiperespaco, Glitch, Lava, Arcade, Carnaval,
        Tempestade, Discoteca, Caleidoscopio, Abissal, Galaxia, ForaDoAr, Holografico,
        Assombrado, Reveillon, Blocos, Cardio, Rave,
        PlacaMae, Nevasca, Bolhas, Miragem, Origami, CidadeNeon
    )

    val available: List<AppThemeSpec> get() = all.filter { it.enabled }

    fun byId(id: String?): AppThemeSpec =
        available.firstOrNull { it.id == id } ?: available.firstOrNull { it.id == DEFAULT_ID } ?: Carbon
}
