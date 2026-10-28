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
    ABSTRACT("Abstratos", "Experimentais, pra quem quer impacto"),
    CRAZY("Loucos", "Sem freio nenhum. Treino no modo caos")
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
    HALO,       // luz suave vinda do topo
    MATRIX,     // chuva de letras verdes
    SPIRAL,     // espiral hipnótica girando
    WARP,       // estrelas em velocidade de dobra
    GLITCH,     // falhas de imagem RGB
    LAVA,       // lâmpada de lava
    ARCADE,     // invasores de pixel marchando
    CARNIVAL,   // confete e serpentina caindo
    WAVES,      // ondas e bolhas (Oceano)
    FIREFLIES,  // vaga-lumes no mato (Floresta)
    DECO,       // leque e moldura art déco (Ouro)
    // segunda leva dos Loucos (ThemeDecorCrazy2.kt)
    STORM,      // chuva, nuvens e relâmpagos
    DISCO,      // bola espelhada e fachos de luz
    KALEIDO,    // raios de arco-íris e polígonos girando
    ABYSS,      // águas-vivas e plâncton brilhando
    GALAXY,     // braços espirais de estrelas
    STATIC,     // chuvisco de TV fora do ar
    HOLO,       // reflexo perolado holográfico
    HAUNTED,    // lua, morcegos, neblina e cerca torta
    FIREWORKS,  // fogos de artifício
    BLOCKS,     // peças de blocos caindo
    ECG,        // monitor cardíaco
    LASERS,     // lasers de rave varrendo
    // terceira leva (ThemeDecorCrazy3.kt)
    CIRCUIT,    // trilhas de placa-mãe com pulsos
    SNOW,       // nevasca com floquinhos
    BUBBLES,    // bolhas de sabão iridescentes
    DESERT,     // sol, dunas e calor tremendo
    ORIGAMI,    // pássaros de papel planando
    NEON_CITY,  // prédios com janelas, letreiros e carros voadores
    // quarta leva (ThemeDecorCrazy4.kt)
    RADAR,      // varredura de radar com alvos piscando
    SAKURA,     // galho de cerejeira e pétalas caindo
    VOLCANO,    // vulcão em erupção com lava e fumaça
    DNA,        // hélices de DNA girando
    VINYL,      // disco de vinil girando e equalizador
    DUNGEON     // parede de pedra com tochas
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

