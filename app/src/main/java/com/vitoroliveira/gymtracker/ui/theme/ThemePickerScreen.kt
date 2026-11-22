package com.vitoroliveira.gymtracker.ui.theme

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.koin.compose.koinInject

/**
 * Tela "Aparência". Grade de 2 colunas com uma miniatura da tela do app em cada tema (cores, cantos,
 * decoração, cor de sucesso), filtro por categoria e a escolha aplicada na hora. Só o tema selecionado
 * anima; os outros ficam parados pra rolagem ficar leve mesmo com dezenas de temas.
 */
@Composable
fun ThemePickerScreen(onBack: () -> Unit) {
    val prefs: ThemePreferences = koinInject()
    val selectedId by prefs.themeId.collectAsState()
    var filter by remember { mutableStateOf<ThemeCategory?>(null) }

    // Um único relógio pro preview animado do tema selecionado.
    val transition = rememberInfiniteTransition(label = "picker-clock")
    val t = transition.animateFloat(
        0f, 1f, infiniteRepeatable(tween(60_000, easing = LinearEasing), RepeatMode.Restart), label = "t"
    )
    val clock: () -> Float = { t.value }

    ThemedScaffold(
        topBar = {
            ThemedTopAppBar(
                title = { Text("Aparência") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { padding ->
        CompositionLocalProvider(LocalThemeClock provides clock) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item(key = "current", span = { GridItemSpan(maxLineSpan) }) {
                    CurrentThemeCard(AppThemes.byId(selectedId))
                }
                item(key = "chips", span = { GridItemSpan(maxLineSpan) }) {
                    CategoryChips(filter) { filter = it }
                }
                ThemeCategory.entries.forEach { category ->
                    if (filter != null && filter != category) return@forEach
                    val themes = AppThemes.available.filter { it.category == category }
                    if (themes.isEmpty()) return@forEach
                    item(key = "header-${category.name}", span = { GridItemSpan(maxLineSpan) }) {
                        Column(Modifier.padding(top = 8.dp)) {
                            Text(category.label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(
                                category.blurb,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    items(themes, key = { it.id }) { spec ->
                        ThemeTile(spec = spec, selected = spec.id == selectedId, onClick = { prefs.select(spec.id) })
                    }
                }
                item(key = "intensity", span = { GridItemSpan(maxLineSpan) }) {
                    Column(Modifier.padding(top = 12.dp)) { IntensityCard(prefs) }
                }
            }
        }
    }
}

/** Resumo do tema em uso, no topo: o que está valendo agora. */
@Composable
private fun CurrentThemeCard(spec: AppThemeSpec) {
    ThemedCard(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text("Tema atual", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(spec.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(spec.tagline, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun CategoryChips(selected: ThemeCategory?, onSelect: (ThemeCategory?) -> Unit) {
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(selected == null, { onSelect(null) }, { Text("Todos") })
        ThemeCategory.entries.forEach { c ->
            FilterChip(selected == c, { onSelect(if (selected == c) null else c) }, { Text(c.label) })
        }
    }
}

/** Miniatura de um tema: fundo, título, cartão com progresso, botão e a paleta. */
@Composable
private fun ThemeTile(spec: AppThemeSpec, selected: Boolean, onClick: () -> Unit) {
    val ring = if (selected) BorderStroke(3.dp, MaterialTheme.colorScheme.primary)
    else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    val clock = LocalThemeClock.current
    val shape = RoundedCornerShape(18.dp)

    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .clickable(onClick = onClick)
            .border(ring, shape)
    ) {
        ThemeScope(spec) {
            val c = MaterialTheme.colorScheme
            val extras = spec.extras
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(172.dp)
                    .drawBehind { drawThemeBackground(extras, c.background, if (selected && extras.animated) clock() else 0.3f) }
                    .padding(10.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Treino A", style = MaterialTheme.typography.titleSmall, color = c.onBackground, fontWeight = FontWeight.Bold, maxLines = 1)
                    ThemedCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(8.dp)) {
                            Text("3 de 4 treinos", style = MaterialTheme.typography.labelMedium, color = c.onSurface, maxLines = 1)
                            Spacer(Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { 0.75f },
                                modifier = Modifier.fillMaxWidth().height(5.dp),
                                color = c.primary,
                                trackColor = c.primary.copy(alpha = 0.25f)
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.weight(1f).height(28.dp).clip(RoundedCornerShape(50)).background(c.primary),
                            contentAlignment = Alignment.Center
                        ) { Text("Iniciar", style = MaterialTheme.typography.labelMedium, color = c.onPrimary, maxLines = 1) }
                        Box(Modifier.size(28.dp).clip(MaterialTheme.shapes.small).background(extras.success))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        listOf(c.primary, c.secondary, c.tertiary, extras.success).forEach {
                            Box(Modifier.size(10.dp).clip(CircleShape).background(it))
                        }
                    }
                }
            }
        }

        // nome + descrição curta (no tema atual do app); altura fixa pra todas as linhas alinharem
        Row(
            Modifier.fillMaxWidth().height(58.dp).padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(spec.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    spec.tagline,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (selected) {
                Icon(Icons.Default.Check, contentDescription = "Selecionado", tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun IntensityCard(prefs: ThemePreferences) {
    val current by prefs.intensity.collectAsState()
    ThemedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Intensidade", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Intensity.entries.forEach { i ->
                    FilterChip(current == i, { prefs.setIntensity(i) }, { Text(i.label) })
                }
            }
            Text(current.blurb, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
