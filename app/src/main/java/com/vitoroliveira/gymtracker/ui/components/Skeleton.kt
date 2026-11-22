package com.vitoroliveira.gymtracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Esqueleto de carregamento (Moxen "Esqueleto"/"Demora"; UI UX Pro Max: esqueleto para >300 ms).
 * Parado, sem pulsar (regra anti-genérico do DESIGN.md): só mostra onde o conteúdo vai ficar, nas formas
 * e cores do tema. Cartões de vidro/borda do tema aparecem como o cartão de verdade (via cor de superfície).
 */
@Composable
fun SkeletonBlock(height: Dp, modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f), MaterialTheme.shapes.large)
    )
}

/** Esqueleto da Home: aviso, destaque, meta + números, gráfico e atalhos. */
@Composable
fun HomeSkeleton(modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SkeletonBlock(96.dp)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SkeletonBlock(206.dp, Modifier.weight(1.25f))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SkeletonBlock(97.dp)
                SkeletonBlock(97.dp)
            }
        }
        SkeletonBlock(220.dp)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SkeletonBlock(56.dp, Modifier.weight(1f))
            SkeletonBlock(56.dp, Modifier.weight(1f))
        }
    }
}

/** Esqueleto de lista (Histórico, etc.): n cartões de altura fixa. */
@Composable
fun ListSkeleton(modifier: Modifier = Modifier, rows: Int = 6, rowHeight: Dp = 72.dp) {
    Column(modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        repeat(rows) { SkeletonBlock(rowHeight) }
    }
}
