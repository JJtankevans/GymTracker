package com.vitoroliveira.gymtracker.ui.theme

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Agrupamento usado na tela de Aparência. */
enum class ThemeCategory(val label: String, val blurb: String) {
    PRO("Profissionais", "Limpos e funcionais, pra treinar sem distração"),
    BOLD("Ousados", "Personalidade forte, sem medo de aparecer"),
    ABSTRACT("Abstratos", "Experimentais, pra quem quer impacto")
}

/** Estrutura da Home. Cada tema escolhe a sua: não é só cor, é composição. */
enum class WorkoutLayout { LIST, FOCUS }

enum class HomeLayout { CLASSIC, BENTO, POSTER, TERMINAL, ORBIT, EDITORIAL }

/** Decoração desenhada atrás de cada tela. */
enum class Decor {
    NONE,
    PAPER,      // grão de papel
    NEON_GRID,  // grade em perspectiva rolando
    SCANLINES,  // monitor CRT
    AURORA,     // manchas de luz se movendo
    SYNTH_SUN,  // sol retrô + grade
    CONFETTI,   // formas geométricas espalhadas
    EMBERS,     // brasas subindo
    BLUEPRINT,  // papel milimetrado técnico
    STRIPES,    // listras diagonais de corrida no canto
    DOTS,       // grade de pontinhos discreta
    HALO        // luz suave vinda do topo
}

/**
 * Tudo que o ColorScheme/Typography/Shapes do Material não cobre:
 * fundo, bordas, sombras duras, brilho e cor de sucesso.
 */
@Immutable
data class ThemeExtras(
    val decor: Decor = Decor.NONE,
    /** Gradiente vertical do fundo (topo → base). Vazio = cor lisa do `background`. */
    val backgroundGradient: List<Color> = emptyList(),
    /** Cores usadas pela decoração (blobs, neon, confete...). */
    val decorColors: List<Color> = emptyList(),
    val cardBorderWidth: Dp = 0.dp,
    val cardBorderColor: Color = Color.Transparent,
    /** Sombra "dura" estilo neo-brutalista (deslocamento sem blur). */
    val hardShadowOffset: Dp = 0.dp,
    val hardShadowColor: Color = Color.Transparent,
    /** Brilho neon ao redor dos cards. */
    val glowColor: Color? = null,
    /** Verde de "concluído". Cada tema pode ter o seu. */
    val success: Color = Color(0xFF4CAF50),
    /** Barras do sistema com ícones escuros? Calculado a partir do tema. */
    val animated: Boolean = false,
    val homeLayout: HomeLayout = HomeLayout.CLASSIC,
    val workoutLayout: WorkoutLayout = WorkoutLayout.LIST
)

@Immutable
data class AppThemeSpec(
    val id: String,
    val name: String,
    val tagline: String,
    val category: ThemeCategory,
    val isDark: Boolean,
    val colors: ColorScheme,
    val typography: Typography,
    val shapes: Shapes,
    val extras: ThemeExtras = ThemeExtras(),
    /** true = usa Material You (Android 12+) quando disponível. */
    val dynamic: Boolean = false,
    /**
     * Interruptor do dev: false esconde o tema do seletor sem apagar o código.
     * É assim que você escolhe quais temas ficam no app.
     */
    val enabled: Boolean = true
)

val LocalThemeExtras = compositionLocalOf { ThemeExtras() }

/** Relógio compartilhado (0..1 a cada 60s) pra a decoração não "reiniciar" ao trocar de tela. */
val LocalThemeClock = compositionLocalOf<() -> Float> { { 0f } }

