package com.vitoroliveira.gymtracker.ui.exercise

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.material3.Button
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.runtime.collectAsState
import org.koin.compose.koinInject
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vitoroliveira.gymtracker.data.catalog.CatalogExercise
import com.vitoroliveira.gymtracker.data.catalog.CatalogMuscle
import com.vitoroliveira.gymtracker.data.catalog.Equipment
import com.vitoroliveira.gymtracker.data.catalog.ExerciseCatalog
import com.vitoroliveira.gymtracker.ui.theme.ThemedCard

/** Cor de cada grupo muscular (a mesma em todo o app, pra virar um "idioma" visual). */
fun CatalogMuscle.tint(): Color = when (this) {
    CatalogMuscle.PEITO -> Color(0xFFEF5350)
    CatalogMuscle.COSTAS -> Color(0xFF42A5F5)
    CatalogMuscle.OMBROS -> Color(0xFFFFA726)
    CatalogMuscle.BICEPS -> Color(0xFF66BB6A)
    CatalogMuscle.TRICEPS -> Color(0xFFAB47BC)
    CatalogMuscle.QUADRICEPS -> Color(0xFF26C6DA)
    CatalogMuscle.POSTERIOR -> Color(0xFF8D6E63)
    CatalogMuscle.GLUTEOS -> Color(0xFFEC407A)
    CatalogMuscle.PANTURRILHA -> Color(0xFF9CCC65)
    CatalogMuscle.ABDOMEN -> Color(0xFFFFCA28)
}

/** Escolha de exercício no catálogo: busca, filtro por músculo e cards com ícone do equipamento. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseCatalogSheet(
    existingNames: List<String>,
    /** 1 exercício: abre o formulário pra ajustar. 2 ou mais: adiciona todos direto. */
    onPickOne: (CatalogExercise) -> Unit,
    onPickMany: (List<CatalogExercise>) -> Unit,
    onCustom: () -> Unit,
    onDismiss: () -> Unit
) {
    val prefs: CatalogPrefs = koinInject()
    val favorites by prefs.favorites.collectAsState()
    val recent by prefs.recent.collectAsState()
    var query by remember { mutableStateOf("") }
    var muscle by remember { mutableStateOf<CatalogMuscle?>(null) }
    var onlyFavs by remember { mutableStateOf(false) }
    var onlyRecent by remember { mutableStateOf(false) }
    val results = remember(query, muscle, onlyFavs, onlyRecent, favorites, recent) {
        val found = ExerciseCatalog.search(query, muscle)
        when {
            onlyRecent -> ExerciseCatalog.recentOnly(found, recent)
            onlyFavs -> found.filter { it.name in favorites }
            else -> ExerciseCatalog.favoritesFirst(found, favorites)
        }
    }
    val selected = remember { mutableStateListOf<String>() }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding()) {
            Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Escolher exercício", style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onCustom) { Text("Digitar outro") }
            }
            OutlinedTextField(
                value = query, onValueChange = { query = it },
                placeholder = { Text("Buscar (ex.: supino, glúteo, polia)") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            )
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(muscle == null && !onlyFavs && !onlyRecent, { muscle = null; onlyFavs = false; onlyRecent = false }, { Text("Todos") })
                }
                item {
                    FilterChip(
                        onlyFavs, { onlyFavs = !onlyFavs; onlyRecent = false }, { Text("Favoritos") },
                        leadingIcon = { Icon(Icons.Default.Star, null, Modifier.size(16.dp)) }
                    )
                }
                item { FilterChip(onlyRecent, { onlyRecent = !onlyRecent; onlyFavs = false }, { Text("Recentes") }) }
                items(CatalogMuscle.entries) { m ->
                    FilterChip(muscle == m, { muscle = if (muscle == m) null else m }, { Text(m.label) })
                }
            }
            if (results.isEmpty()) {
                Text(
                    when {
                        onlyFavs -> "Nenhum favorito ainda. Toque na estrela de um exercício."
                        onlyRecent -> "Os exercícios que você adicionar aparecem aqui."
                        else -> "Nada encontrado. Use \"Digitar outro\" pra criar o seu."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(24.dp)
                )
            }
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxWidth().height(440.dp),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(results, key = { it.name }) { ex ->
                    val added = existingNames.any { ExerciseCatalog.isSameName(it, ex.name) }
                    CatalogCard(
                        ex, added = added, selected = ex.name in selected, favorite = ex.name in favorites,
                        onFavorite = { prefs.toggleFavorite(ex.name) }
                    ) {
                        if (!added) { if (ex.name in selected) selected.remove(ex.name) else selected.add(ex.name) }
                    }
                }
            }
            val chosen = ExerciseCatalog.all.filter { it.name in selected }
            Button(
                onClick = {
                    prefs.addRecent(chosen.map { it.name })
                    if (chosen.size == 1) onPickOne(chosen[0]) else onPickMany(chosen)
                },
                enabled = chosen.isNotEmpty(),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).height(52.dp)
            ) {
                Text(
                    when (chosen.size) {
                        0 -> "Toque nos exercícios para escolher"
                        1 -> "Continuar com 1 exercício"
                        else -> "Adicionar ${chosen.size} exercícios"
                    },
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun CatalogCard(
    ex: CatalogExercise, added: Boolean, selected: Boolean,
    favorite: Boolean, onFavorite: () -> Unit, onClick: () -> Unit
) {
    val tint = ex.muscle.tint()
    ThemedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().alpha(if (added) 0.55f else 1f)
            .border(if (selected) 3.dp else 0.dp, MaterialTheme.colorScheme.primary, MaterialTheme.shapes.medium),
        shape = MaterialTheme.shapes.medium
    ) {
        Column {
            Box(
                Modifier.fillMaxWidth().aspectRatio(1.9f).background(tint.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                EquipmentIcon(ex.equipment, tint, Modifier.size(54.dp))
                Box(
                    Modifier.align(Alignment.TopStart).size(40.dp).clip(CircleShape).clickable(onClick = onFavorite),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (favorite) Icons.Default.Star else Icons.Default.StarBorder,
                        if (favorite) "Remover dos favoritos" else "Favoritar", Modifier.size(22.dp),
                        tint = if (favorite) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (added || selected) {
                    Box(
                        Modifier.align(Alignment.TopEnd).padding(6.dp).size(22.dp).clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) { Icon(Icons.Default.Check, if (added) "Já adicionado" else "Selecionado", Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onPrimary) }
                }
            }
            Column(Modifier.padding(10.dp)) {
                Text(
                    ex.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold,
                    maxLines = 2, overflow = TextOverflow.Ellipsis, minLines = 2
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "${ex.muscle.label} · ${ex.equipment.label}",
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/** Ícone desenhado do equipamento (sem arquivos de imagem). */
@Composable
fun EquipmentIcon(equipment: Equipment, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        when (equipment) {
            Equipment.BARRA -> barbell(color)
            Equipment.HALTERES -> dumbbell(color)
            Equipment.MAQUINA -> machine(color)
            Equipment.POLIA -> cable(color)
            Equipment.PESO_CORPO -> person(color)
            Equipment.SMITH -> smith(color)
        }
    }
}

private fun DrawScope.barbell(c: Color) {
    val w = size.width; val h = size.height; val cy = h / 2
    drawRoundRect(c, Offset(w * 0.04f, cy - h * 0.035f), Size(w * 0.92f, h * 0.07f), CornerRadius(h * 0.03f))
    listOf(0.16f to 0.62f, 0.27f to 0.46f).forEach { (x, f) ->
        val ph = h * f; val pw = w * 0.085f
        drawRoundRect(c, Offset(w * x - pw / 2, cy - ph / 2), Size(pw, ph), CornerRadius(pw / 3))
        drawRoundRect(c, Offset(w * (1 - x) - pw / 2, cy - ph / 2), Size(pw, ph), CornerRadius(pw / 3))
    }
}

private fun DrawScope.dumbbell(c: Color) {
    val w = size.width; val h = size.height; val cy = h / 2
    drawRoundRect(c, Offset(w * 0.25f, cy - h * 0.05f), Size(w * 0.5f, h * 0.10f), CornerRadius(h * 0.04f))
    listOf(0.12f, 0.20f).forEachIndexed { i, x ->
        val ph = h * (if (i == 0) 0.52f else 0.40f); val pw = w * 0.10f
        drawRoundRect(c, Offset(w * x - pw / 2, cy - ph / 2), Size(pw, ph), CornerRadius(pw / 3))
        drawRoundRect(c, Offset(w * (1 - x) - pw / 2, cy - ph / 2), Size(pw, ph), CornerRadius(pw / 3))
    }
}

private fun DrawScope.machine(c: Color) {
    val w = size.width; val h = size.height
    val s = Stroke(w * 0.07f, cap = StrokeCap.Round)
    // coluna de pesos
    for (i in 0 until 5) drawRoundRect(c, Offset(w * 0.12f, h * (0.14f + i * 0.13f)), Size(w * 0.22f, h * 0.09f), CornerRadius(3f))
    // assento + encosto + alavanca
    drawLine(c, Offset(w * 0.5f, h * 0.88f), Offset(w * 0.9f, h * 0.88f), s.width, StrokeCap.Round)
    drawLine(c, Offset(w * 0.5f, h * 0.88f), Offset(w * 0.5f, h * 0.38f), s.width, StrokeCap.Round)
    drawLine(c, Offset(w * 0.5f, h * 0.38f), Offset(w * 0.9f, h * 0.2f), s.width, StrokeCap.Round)
}

private fun DrawScope.cable(c: Color) {
    val w = size.width; val h = size.height
    val sw = w * 0.06f
    drawLine(c, Offset(w * 0.15f, h * 0.06f), Offset(w * 0.15f, h * 0.94f), sw, StrokeCap.Round)
    drawCircle(c, w * 0.11f, Offset(w * 0.32f, h * 0.2f), style = Stroke(sw))
    drawLine(c, Offset(w * 0.32f, h * 0.31f), Offset(w * 0.32f, h * 0.7f), sw * 0.6f, StrokeCap.Round)
    drawLine(c, Offset(w * 0.22f, h * 0.7f), Offset(w * 0.42f, h * 0.7f), sw, StrokeCap.Round)
    drawLine(c, Offset(w * 0.15f, h * 0.94f), Offset(w * 0.9f, h * 0.94f), sw, StrokeCap.Round)
    drawRoundRect(c, Offset(w * 0.62f, h * 0.35f), Size(w * 0.2f, h * 0.45f), CornerRadius(4f))
}

private fun DrawScope.person(c: Color) {
    val w = size.width; val h = size.height; val sw = w * 0.07f
    drawCircle(c, w * 0.1f, Offset(w * 0.5f, h * 0.15f))
    drawLine(c, Offset(w * 0.5f, h * 0.28f), Offset(w * 0.5f, h * 0.62f), sw, StrokeCap.Round)
    drawLine(c, Offset(w * 0.22f, h * 0.42f), Offset(w * 0.78f, h * 0.36f), sw, StrokeCap.Round)
    drawLine(c, Offset(w * 0.5f, h * 0.62f), Offset(w * 0.3f, h * 0.9f), sw, StrokeCap.Round)
    drawLine(c, Offset(w * 0.5f, h * 0.62f), Offset(w * 0.7f, h * 0.9f), sw, StrokeCap.Round)
}

private fun DrawScope.smith(c: Color) {
    val w = size.width; val h = size.height; val sw = w * 0.05f
    drawLine(c, Offset(w * 0.18f, h * 0.06f), Offset(w * 0.18f, h * 0.94f), sw, StrokeCap.Round)
    drawLine(c, Offset(w * 0.82f, h * 0.06f), Offset(w * 0.82f, h * 0.94f), sw, StrokeCap.Round)
    drawRoundRect(c, Offset(w * 0.05f, h * 0.44f), Size(w * 0.9f, h * 0.07f), CornerRadius(4f))
    drawRoundRect(c, Offset(w * 0.1f, h * 0.34f), Size(w * 0.07f, h * 0.27f), CornerRadius(3f))
    drawRoundRect(c, Offset(w * 0.83f, h * 0.34f), Size(w * 0.07f, h * 0.27f), CornerRadius(3f))
}
