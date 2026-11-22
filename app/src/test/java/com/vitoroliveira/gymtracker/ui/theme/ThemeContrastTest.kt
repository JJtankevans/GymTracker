package com.vitoroliveira.gymtracker.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Confere o contraste de cada tema (regra do DESIGN.md). Falha listando TODOS os pares ruins de uma vez,
 * para dar para corrigir o tema em lote. Temas dinâmicos (Material You) usam as cores do celular e ficam de fora.
 */
class ThemeContrastTest {
    private fun ratio(a: Color, b: Color): Double {
        val la = a.luminance().toDouble()
        val lb = b.luminance().toDouble()
        return (maxOf(la, lb) + 0.05) / (minOf(la, lb) + 0.05)
    }

    private data class Check(val label: String, val fg: Color, val bg: Color, val min: Double)

    @Test
    fun todosOsTemasTemContrasteLegivel() {
        val problems = mutableListOf<String>()
        AppThemes.available.filter { !it.dynamic }.forEach { spec ->
            val c = spec.colors
            // cartões de vidro são translúcidos: o que o olho vê é o cartão composto sobre o fundo
            val card = c.surfaceContainerHighest.compositeOver(c.background)
            val checks = listOf(
                Check("texto/fundo", c.onBackground, c.background, 4.5),
                Check("texto/cartão", c.onSurface, card, 4.5),
                Check("texto de apoio/cartão", c.onSurfaceVariant, card, 3.0),
                Check("texto do botão/destaque", c.onPrimary, c.primary, 4.5),
                Check("destaque/fundo", c.primary, c.background, 3.0),
                Check("texto/container de destaque", c.onPrimaryContainer, c.primaryContainer, 4.5)
            )
            checks.forEach { k ->
                val r = ratio(k.fg, k.bg)
                if (r < k.min) problems += "${spec.id}: ${k.label} = ${"%.2f".format(r)} (mín ${k.min})"
            }
        }
        assertTrue("Contraste insuficiente em ${problems.size} pares:\n" + problems.joinToString("\n"), problems.isEmpty())
    }
}
