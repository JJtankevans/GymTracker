package com.vitoroliveira.gymtracker.ui.theme

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.koin.compose.koinInject

/** Tela "Aparência": lista os temas com preview ao vivo e salva a escolha. */
@Composable
fun ThemePickerScreen(onBack: () -> Unit) {
    val prefs: ThemePreferences = koinInject()
    val selectedId by prefs.themeId.collectAsState()

    // Um único relógio pros previews animados (evita N animações independentes).
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
                },
                actions = {
                    // 40 temas: deixa a sorte escolher (nunca repete o atual)
                    IconButton(onClick = {
                        AppThemes.available.filter { it.id != selectedId }.randomOrNull()?.let { prefs.select(it.id) }
                    }) {
                        Icon(Icons.Default.Shuffle, contentDescription = "Tema aleatório")
                    }
                }
            )
        }
    ) { padding ->
        CompositionLocalProvider(LocalThemeClock provides clock) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item(key = "intensity") { IntensityCard(prefs) }
                ThemeCategory.entries.forEach { category ->
                    val themes = AppThemes.available.filter { it.category == category }
                    if (themes.isEmpty()) return@forEach
                    item(key = "header-${category.name}") {
                        Column(Modifier.padding(top = 12.dp)) {
                            Text(
                                category.label,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                category.blurb,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    items(themes, key = { it.id }) { spec ->
                        ThemeOption(
                            spec = spec,
                            selected = spec.id == selectedId,
                            onClick = { prefs.select(spec.id) }
                        )
                    }
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
private fun ThemeOption(spec: AppThemeSpec, selected: Boolean, onClick: () -> Unit) {
    val ring = if (selected) BorderStroke(3.dp, MaterialTheme.colorScheme.primary)
    else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    val clock = LocalThemeClock.current

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .clickable(onClick = onClick)
            .border(ring, RoundedCornerShape(22.dp))
    ) {
        // ── preview no estilo do próprio tema ──
        ThemeScope(spec) {
            val bg = MaterialTheme.colorScheme.background
            val extras = spec.extras
            Box(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 176.dp)
                    .drawBehind { drawThemeBackground(extras, bg, if (extras.animated) clock() else 0.3f) }
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Treino A",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(Modifier.weight(1f))
                        Box(
                            Modifier.size(22.dp).clip(CircleShape).drawBehind {
                                drawCircle(Color.Transparent)
                            }.border(2.dp, MaterialTheme.colorScheme.tertiary, CircleShape)
                        )
                    }
                    ThemedCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(14.dp)) {
                            Text(
                                "3 de 4 treinos",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { 0.75f },
                                modifier = Modifier.fillMaxWidth().height(8.dp),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(onClick = onClick) { Text("Iniciar treino") }
                        Box(
                            Modifier
                                .height(40.dp)
                                .width(40.dp)
                                .clip(MaterialTheme.shapes.medium)
                                .drawBehind { drawRect(extras.success) }
                        )
                    }
                }
            }
        }

        // ── nome + descrição (no tema atual do app) ──
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(spec.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    spec.tagline,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
                    androidx.compose.material3.FilterChip(current == i, { prefs.setIntensity(i) }, { Text(i.label) })
                }
            }
            Text(current.blurb, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
