package com.vitoroliveira.gymtracker.ui.theme

import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import com.vitoroliveira.gymtracker.R

/**
 * Fontes embutidas (Google Fonts, licença OFL). As "variáveis" têm um arquivo só e o peso
 * é escolhido por eixo (Android 8+; o app já exige isso).
 */
@OptIn(ExperimentalTextApi::class)
object ThemeFonts {

    private val weights = listOf(
        FontWeight.Light, FontWeight.Normal, FontWeight.Medium, FontWeight.SemiBold,
        FontWeight.Bold, FontWeight.ExtraBold, FontWeight.Black
    )

    private fun variable(res: Int) = FontFamily(
        weights.map { w ->
            Font(res, w, variationSettings = FontVariation.Settings(FontVariation.weight(w.weight)))
        }
    )

    /** Neutra e muito legível: a base dos temas profissionais. */
    val Inter = variable(R.font.inter)

    /** Geométrica com personalidade, boa pra números grandes. */
    val SpaceGrotesk = variable(R.font.space_grotesk)

    /** Serifada de contraste alto (editorial / luxo). */
    val Playfair = variable(R.font.playfair_display)

    /** Monoespaçada moderna (técnico / terminal). */
    val Mono = variable(R.font.jetbrains_mono)

    /** Condensada pesada: placar, competição. */
    val BarlowCondensed = FontFamily(
        Font(R.font.barlow_condensed_medium, FontWeight.Medium),
        Font(R.font.barlow_condensed_medium, FontWeight.Normal),
        Font(R.font.barlow_condensed_bold, FontWeight.SemiBold),
        Font(R.font.barlow_condensed_bold, FontWeight.Bold),
        Font(R.font.barlow_condensed_extrabold, FontWeight.ExtraBold),
        Font(R.font.barlow_condensed_black, FontWeight.Black)
    )
}
