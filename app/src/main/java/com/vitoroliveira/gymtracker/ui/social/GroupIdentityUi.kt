package com.vitoroliveira.gymtracker.ui.social

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.vitoroliveira.gymtracker.data.model.SocialGroup
import com.vitoroliveira.gymtracker.presentation.social.GroupEditForm
import com.vitoroliveira.gymtracker.presentation.social.GroupWeekProgress
import com.vitoroliveira.gymtracker.presentation.social.Identity
import com.vitoroliveira.gymtracker.presentation.social.Look
import com.vitoroliveira.gymtracker.ui.theme.AppThemes
import com.vitoroliveira.gymtracker.ui.theme.LocalThemeClock
import com.vitoroliveira.gymtracker.ui.theme.drawThemeBackground

/** Ícone escolhido para o grupo ou desafio (ids de [Identity.icons]); sem escolha, o halter. */
fun identityIcon(id: String?): ImageVector = when (id) {
    "bolt" -> Icons.Default.Bolt
    "fire" -> Icons.Default.LocalFireDepartment
    "trophy" -> Icons.Default.EmojiEvents
    "shield" -> Icons.Default.Shield
    "rocket" -> Icons.Default.RocketLaunch
    "star" -> Icons.Default.Star
    "heart" -> Icons.Default.Favorite
    "diamond" -> Icons.Default.Diamond
    "run" -> Icons.Default.DirectionsRun
    "mountain" -> Icons.Default.Terrain
    "paw" -> Icons.Default.Pets
    else -> Icons.Default.FitnessCenter
}

/** "#RRGGBB" para cor; null se não der. */
fun parseAccent(hex: String?): Color? = hex?.let { runCatching { Color(android.graphics.Color.parseColor(it)) }.getOrNull() }

/**
 * Faixa de capa de um grupo ou desafio: o fundo animado do tema escolhido (Radar, Sakura, Vulcão...) com a cor de
 * destaque por cima. [animated] = false desenha um quadro parado e mais simples (listas com várias capas); a folha de
 * detalhe anima. Sem capa escolhida, só a cor de destaque.
 */
@Composable
fun CoverBox(
    look: Look,
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.medium,
    animated: Boolean = false,
    content: @Composable BoxScope.() -> Unit
) {
    val spec = remember(look.cover) { look.cover?.let { id -> AppThemes.all.firstOrNull { it.id == id } } }
    val accent = parseAccent(look.accentHex) ?: MaterialTheme.colorScheme.primary
    val clock = LocalThemeClock.current
    val base = if (spec != null) Modifier.drawBehind {
        drawThemeBackground(spec.extras, spec.colors.background, if (animated) clock() else 0.3f, if (animated) 1 else 0)
    } else Modifier.background(Brush.linearGradient(listOf(accent.copy(alpha = 0.35f), accent.copy(alpha = 0.10f))))
    Box(modifier.clip(shape).then(base)) {
        // véu da cor de destaque e escurecimento embaixo, para o texto sempre ler bem
        Box(Modifier.matchParentSize().background(Brush.verticalGradient(listOf(accent.copy(alpha = if (spec != null) 0.16f else 0f), Color.Black.copy(alpha = 0.50f)))))
        content()
    }
}

/** Círculo com o ícone, na cor de destaque. */
@Composable
fun IdentityBadge(look: Look, size: Dp = 40.dp) {
    val accent = parseAccent(look.accentHex) ?: MaterialTheme.colorScheme.primary
    Box(
        Modifier.size(size).clip(CircleShape).background(Color.Black.copy(alpha = 0.55f)).border(2.dp, accent, CircleShape),
        Alignment.Center
    ) { Icon(identityIcon(look.icon), null, Modifier.size(size * 0.55f), tint = accent) }
}

/** Escolha de capa, cor e ícone (usado ao criar/editar grupo e ao criar desafio). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun IdentityPicker(look: Look, onChange: (Look) -> Unit) {
    val cs = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Capa", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val none = look.cover == null
            Box(
                Modifier.size(72.dp, 48.dp).clip(MaterialTheme.shapes.small).background(cs.surfaceContainerHighest)
                    .border(if (none) 2.dp else 1.dp, if (none) cs.primary else cs.outlineVariant, MaterialTheme.shapes.small)
                    .clickable { onChange(look.copy(cover = null)) },
                Alignment.Center
            ) { Text("Sem capa", style = MaterialTheme.typography.labelSmall) }
            Identity.covers.forEach { id ->
                val selected = look.cover == id
                CoverBox(
                    Look(cover = id, accentHex = look.accentHex),
                    Modifier.size(72.dp, 48.dp).clickable { onChange(look.copy(cover = id)) }
                        .border(if (selected) 2.dp else 0.dp, if (selected) cs.primary else Color.Transparent, MaterialTheme.shapes.small),
                    MaterialTheme.shapes.small
                ) {
                    Text(
                        AppThemes.all.firstOrNull { it.id == id }?.name ?: id, Modifier.align(Alignment.BottomStart).padding(4.dp),
                        style = MaterialTheme.typography.labelSmall, color = Color.White, maxLines = 1
                    )
                }
            }
        }
        Text("Cor", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Identity.accents.forEach { hex ->
                val c = parseAccent(hex) ?: return@forEach
                val selected = look.accentHex?.uppercase() == hex
                Box(
                    Modifier.size(34.dp).clip(CircleShape).background(c)
                        .border(if (selected) 3.dp else 1.dp, if (selected) cs.onSurface else cs.outlineVariant, CircleShape)
                        .clickable { onChange(look.copy(accentHex = if (selected) null else hex)) },
                    Alignment.Center
                ) { if (selected) Icon(Icons.Default.Check, null, Modifier.size(18.dp), tint = Color.Black) }
            }
        }
        Text("Ícone", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Identity.icons.forEach { id ->
                val selected = look.icon == id
                Box(
                    Modifier.size(44.dp).clip(MaterialTheme.shapes.small)
                        .background(if (selected) cs.primary.copy(alpha = 0.20f) else cs.surfaceContainerHighest)
                        .border(if (selected) 2.dp else 1.dp, if (selected) cs.primary else cs.outlineVariant, MaterialTheme.shapes.small)
                        .clickable { onChange(look.copy(icon = if (selected) null else id)) },
                    Alignment.Center
                ) { Icon(identityIcon(id), null, tint = if (selected) cs.primary else cs.onSurface) }
            }
        }
    }
}

/** Detalhe do grupo: capa animada, aviso fixado, meta da semana, código de convite e ações. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupSheet(
    group: SocialGroup,
    mine: Boolean,
    isOwner: Boolean,
    progress: GroupWeekProgress?,
    onLoad: () -> Unit,
    onEdit: () -> Unit,
    onJoin: () -> Unit,
    onLeave: () -> Unit,
    onDismiss: () -> Unit
) {
    LaunchedEffect(group.id) { if (mine) onLoad() }
    val cs = MaterialTheme.colorScheme
    val accent = parseAccent(group.accentHex) ?: cs.primary
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            CoverBox(group.look, Modifier.fillMaxWidth().height(150.dp), animated = true) {
                Row(Modifier.align(Alignment.BottomStart).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    IdentityBadge(group.look, 52.dp)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(group.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = Color.White)
                        Text(
                            (if (group.isPublic) "Grupo público" else "Por convite") + (group.maxMembers?.let { " · até $it membros" } ?: ""),
                            style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                }
            }
            group.description?.takeIf { it.isNotBlank() }?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
            group.pinnedMessage?.takeIf { it.isNotBlank() }?.let { msg ->
                Row(
                    Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).background(accent.copy(alpha = 0.12f))
                        .border(1.dp, accent.copy(alpha = 0.5f), MaterialTheme.shapes.medium).padding(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(Icons.Default.PushPin, null, tint = accent)
                    Spacer(Modifier.width(8.dp))
                    Text(msg, style = MaterialTheme.typography.bodyMedium)
                }
            }
            if (mine && (group.weeklyGoal != null || progress != null)) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Meta da semana", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    val p = progress ?: GroupWeekProgress()
                    val f = p.fraction(group.weeklyGoal)
                    if (f != null) {
                        LinearProgressIndicator(progress = { f.coerceIn(0f, 1f) }, Modifier.fillMaxWidth().height(10.dp).clip(CircleShape), color = accent)
                    }
                    Text(
                        p.summary(group.weeklyGoal) + if (f != null && f >= 1f) " · meta batida!" else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (f != null && f >= 1f) accent else cs.onSurfaceVariant,
                        fontWeight = if (f != null && f >= 1f) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
            if (isOwner && group.inviteCode != null) Text(
                "Código de convite: ${group.inviteCode}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = accent
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                when {
                    isOwner -> Button(onEdit, Modifier.weight(1f)) { Text("Personalizar grupo") }
                    mine -> OutlinedButton(onLeave, Modifier.weight(1f)) { Text("Sair do grupo") }
                    else -> Button(onJoin, Modifier.weight(1f)) { Text("Entrar no grupo") }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

/** O dono personaliza o grupo: capa, cor, ícone, descrição, aviso fixado e meta semanal. */
@Composable
fun GroupEditDialog(group: SocialGroup, onSave: (GroupEditForm) -> Unit, onDismiss: () -> Unit) {
    var look by remember { mutableStateOf(group.look) }
    var description by remember { mutableStateOf(group.description.orEmpty()) }
    var pinned by remember { mutableStateOf(group.pinnedMessage.orEmpty()) }
    var goal by remember { mutableStateOf(group.weeklyGoal?.toString().orEmpty()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Personalizar grupo") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                IdentityPicker(look, onChange = { look = it })
                OutlinedTextField(description, { description = it.take(500) }, label = { Text("Descrição") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                OutlinedTextField(pinned, { pinned = it.take(300) }, label = { Text("Aviso fixado (todos veem no topo)") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                OutlinedTextField(
                    goal, { goal = it.filter(Char::isDigit).take(3) }, label = { Text("Meta de treinos do grupo por semana") },
                    singleLine = true, modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                )
                Text(
                    "A meta soma os treinos de todos os membros de segunda a domingo. Deixe vazio para não ter meta.",
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = { Button({ onSave(GroupEditForm(description, look, pinned, goal.toIntOrNull())) }) { Text("Salvar") } },
        dismissButton = { TextButton(onDismiss) { Text("Cancelar") } }
    )
}
