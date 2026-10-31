package com.vitoroliveira.gymtracker.ui.social

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vitoroliveira.gymtracker.data.model.GroupMemberRow
import com.vitoroliveira.gymtracker.data.model.GroupMessage
import com.vitoroliveira.gymtracker.data.model.SocialGroup
import com.vitoroliveira.gymtracker.presentation.social.ChallengeItem
import com.vitoroliveira.gymtracker.presentation.social.ChallengeStatus
import com.vitoroliveira.gymtracker.presentation.social.GroupRole
import com.vitoroliveira.gymtracker.presentation.social.GroupWeekProgress
import com.vitoroliveira.gymtracker.presentation.social.MAX_MESSAGE_LENGTH
import com.vitoroliveira.gymtracker.presentation.social.canChangeRole
import com.vitoroliveira.gymtracker.presentation.social.canDeleteMessage
import com.vitoroliveira.gymtracker.presentation.social.canRemoveMember
import com.vitoroliveira.gymtracker.presentation.social.messageAge
import com.vitoroliveira.gymtracker.presentation.social.rankMembers
import com.vitoroliveira.gymtracker.presentation.social.toggledRole
import com.vitoroliveira.gymtracker.ui.profile.UserAvatar
import kotlin.time.Clock

/** Tudo que a folha do grupo mostra. */
class GroupSheetData(
    val group: SocialGroup,
    val mine: Boolean,
    val me: String?,
    val progress: GroupWeekProgress?,
    val streak: Int,
    val board: List<GroupMemberRow>,
    val messages: List<GroupMessage>,
    val challenges: List<ChallengeItem>
)

class GroupSheetActions(
    val load: () -> Unit,
    val edit: () -> Unit,
    val join: () -> Unit,
    val leave: () -> Unit,
    val removeMember: (String) -> Unit,
    val setRole: (String, String) -> Unit,
    val post: (String) -> Unit,
    val deleteMessage: (String) -> Unit,
    val newChallenge: () -> Unit,
    val openChallenge: (ChallengeItem) -> Unit
)

/**
 * Detalhe do grupo: capa animada, dados rápidos, e três abas: Resumo (aviso fixado, meta da semana, desafios e convite),
 * Ranking (membros na semana, com gerenciamento para dono e admins) e Mural.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupSheet(d: GroupSheetData, a: GroupSheetActions, onDismiss: () -> Unit) {
    val group = d.group
    LaunchedEffect(group.id) { if (d.mine) a.load() }
    val cs = MaterialTheme.colorScheme
    val accent = parseAccent(group.accentHex) ?: cs.primary
    val myRole = if (group.ownerId == d.me) GroupRole.OWNER else d.board.firstOrNull { it.isMe }?.role
    val canManage = myRole == GroupRole.OWNER || myRole == GroupRole.ADMIN
    var tab by rememberSaveable { mutableIntStateOf(0) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            Modifier.fillMaxWidth().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // capa
            CoverBox(group.look, Modifier.fillMaxWidth().height(168.dp), shape = RoundedCornerShape(22.dp), animated = true) {
                Text(
                    if (group.isPublic) "Público" else "Por convite",
                    Modifier.align(Alignment.TopEnd).padding(12.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.45f)).padding(horizontal = 10.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelMedium, color = Color.White
                )
                Row(Modifier.align(Alignment.BottomStart).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    IdentityBadge(group.look, 56.dp)
                    Spacer(Modifier.width(12.dp))
                    Text(group.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }

            // dados rápidos
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val members = d.progress?.members ?: d.board.size.takeIf { it > 0 }
                members?.let { StatPill(Icons.Default.Groups, if (it == 1) "1 membro" else "$it membros", accent) }
                if (d.streak > 0) StatPill(Icons.Default.LocalFireDepartment, if (d.streak == 1) "1 semana seguida" else "${d.streak} semanas seguidas", accent)
                group.weeklyGoal?.let { StatPill(Icons.Default.Check, "meta $it treinos/semana", accent) }
                group.maxMembers?.let { StatPill(Icons.Default.Groups, "até $it", accent) }
            }

            group.description?.takeIf { it.isNotBlank() }?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
            }

            if (d.mine) PillTabs(listOf("Resumo", "Ranking", "Mural"), tab, accent) { tab = it }

            when {
                !d.mine || tab == 0 -> SummaryTab(d, a, accent, canManage)
                tab == 1 -> RankingTab(d, a, myRole)
                else -> WallTab(d, a, myRole)
            }

            // ação principal
            when {
                myRole == GroupRole.OWNER -> Button(a.edit, Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(16.dp)) { Text("Personalizar grupo") }
                d.mine -> OutlinedButton(a.leave, Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(16.dp)) { Text("Sair do grupo", color = cs.error) }
                else -> Button(a.join, Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(16.dp)) { Text("Entrar no grupo") }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

// ───────────────────────── Peças ─────────────────────────

@Composable
private fun StatPill(icon: ImageVector, text: String, tint: Color) {
    Row(
        Modifier.clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerHighest).padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, Modifier.size(16.dp), tint = tint)
        Spacer(Modifier.width(6.dp))
        Text(text, style = MaterialTheme.typography.labelLarge, maxLines = 1)
    }
}

/** Abas em pílula: uma faixa suave com a aba escolhida destacada na cor do grupo. */
@Composable
private fun PillTabs(labels: List<String>, selected: Int, accent: Color, onSelect: (Int) -> Unit) {
    val cs = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(cs.surfaceContainerHighest).padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        labels.forEachIndexed { i, label ->
            val sel = i == selected
            Box(
                Modifier.weight(1f).clip(RoundedCornerShape(12.dp))
                    .background(if (sel) accent.copy(alpha = 0.24f) else Color.Transparent)
                    .clickable { onSelect(i) }.padding(vertical = 10.dp),
                Alignment.Center
            ) {
                Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = if (sel) FontWeight.Bold else FontWeight.Medium, color = if (sel) cs.onSurface else cs.onSurfaceVariant)
            }
        }
    }
}

/** Aviso fixado: cartão suave com uma barra na cor do grupo e "ver mais" quando o texto é grande. Mostra o texto inteiro. */
@Composable
private fun PinnedNotice(text: String, accent: Color) {
    var expanded by remember(text) { mutableStateOf(false) }
    var overflow by remember(text) { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth().height(IntrinsicSize.Min).clip(RoundedCornerShape(16.dp)).background(accent.copy(alpha = 0.09f))
    ) {
        Box(Modifier.width(4.dp).fillMaxHeight().background(accent))
        Column(Modifier.weight(1f).padding(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Campaign, null, Modifier.size(16.dp), tint = accent)
                Spacer(Modifier.width(6.dp))
                Text("AVISO DO GRUPO", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = accent, letterSpacing = 1.2.sp)
            }
            Text(
                text, style = MaterialTheme.typography.bodyMedium,
                maxLines = if (expanded) Int.MAX_VALUE else 4, overflow = TextOverflow.Ellipsis,
                onTextLayout = { if (!expanded) overflow = it.hasVisualOverflow }
            )
            if (overflow || expanded) TextButton({ expanded = !expanded }, contentPadding = PaddingValues(0.dp)) {
                Text(if (expanded) "Ver menos" else "Ver mais", color = accent, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

// ───────────────────────── Aba Resumo ─────────────────────────

@Composable
private fun SummaryTab(d: GroupSheetData, a: GroupSheetActions, accent: Color, canManage: Boolean) {
    val cs = MaterialTheme.colorScheme
    val group = d.group
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        group.pinnedMessage?.takeIf { it.isNotBlank() }?.let { PinnedNotice(it, accent) }

        if (d.mine && (group.weeklyGoal != null || d.progress != null)) {
            val p = d.progress ?: GroupWeekProgress()
            val f = p.fraction(group.weeklyGoal)
            val reached = f != null && f >= 1f
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(cs.surfaceContainerHigh).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("${p.workouts}", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black, color = accent)
                    Text(
                        if (group.weeklyGoal != null) " / ${group.weeklyGoal} treinos na semana" else if (p.workouts == 1) " treino na semana" else " treinos na semana",
                        Modifier.padding(bottom = 6.dp), style = MaterialTheme.typography.titleSmall, color = cs.onSurfaceVariant
                    )
                    Spacer(Modifier.weight(1f))
                    if (reached) Row(
                        Modifier.clip(CircleShape).background(accent.copy(alpha = 0.18f)).padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Check, null, Modifier.size(14.dp), tint = accent); Spacer(Modifier.width(4.dp))
                        Text("Meta batida", style = MaterialTheme.typography.labelMedium, color = accent, fontWeight = FontWeight.Bold)
                    }
                }
                if (f != null) LinearProgressIndicator(
                    progress = { f.coerceIn(0f, 1f) }, Modifier.fillMaxWidth().height(12.dp).clip(CircleShape), color = accent, trackColor = accent.copy(alpha = 0.16f)
                )
                Text(
                    "${p.activeMembers} de ${p.members} ${if (p.members == 1) "membro treinou" else "membros treinaram"} nesta semana",
                    style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant
                )
                if (d.streak > 0) Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocalFireDepartment, null, Modifier.size(18.dp), tint = accent)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        if (d.streak == 1) "1 semana seguida batendo a meta" else "${d.streak} semanas seguidas batendo a meta",
                        style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        if (d.mine) {
            Text("Desafios do grupo", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            if (d.challenges.isEmpty()) Text("Nenhum desafio ainda. Que tal criar o primeiro?", style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
            d.challenges.take(5).forEach { item ->
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(cs.surfaceContainerHigh).clickable { a.openChallenge(item) }.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IdentityBadge(item.challenge.look, 36.dp)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(item.challenge.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            item.status.label + if (item.status == ChallengeStatus.ACTIVE) " · faltam ${item.daysLeft} ${if (item.daysLeft == 1) "dia" else "dias"}" else "",
                            style = MaterialTheme.typography.labelSmall, color = if (item.status == ChallengeStatus.ACTIVE) cs.tertiary else cs.onSurfaceVariant
                        )
                    }
                    if (item.joined) Text("Participando", style = MaterialTheme.typography.labelSmall, color = accent)
                }
            }
            OutlinedButton(a.newChallenge, Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                Icon(Icons.Default.Add, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Novo desafio neste grupo")
            }
        }

        if (canManage && group.inviteCode != null) {
            val context = LocalContext.current
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).border(1.dp, cs.outlineVariant, RoundedCornerShape(14.dp)).padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Código de convite", style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant)
                    Text(group.inviteCode!!, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = accent)
                }
                IconButton({
                    val send = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, "Entra no meu grupo \"${group.name}\" no GymTracker! Código de convite: ${group.inviteCode}")
                    }
                    context.startActivity(Intent.createChooser(send, "Convidar para o grupo"))
                }) { Icon(Icons.Default.Share, "Compartilhar convite", tint = accent) }
            }
        }
    }
}

// ───────────────────────── Aba Ranking ─────────────────────────

@Composable
private fun RankingTab(d: GroupSheetData, a: GroupSheetActions, myRole: String?) {
    val cs = MaterialTheme.colorScheme
    val ranked = rankMembers(d.board)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Ranking da semana", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        if (ranked.isEmpty()) Text("Carregando os membros...", style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
        ranked.forEach { (pos, m) ->
            var menu by remember { mutableStateOf(false) }
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(if (m.isMe) cs.primary.copy(alpha = 0.10f) else Color.Transparent).padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PositionBadge(pos)
                Spacer(Modifier.width(10.dp))
                UserAvatar(m.avatarUrl, 38.dp, fallbackName = m.username)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(if (m.isMe) "Você" else "@${m.username}", style = MaterialTheme.typography.bodyMedium, fontWeight = if (m.isMe) FontWeight.Bold else FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                        if (m.role != GroupRole.MEMBER) {
                            Spacer(Modifier.width(6.dp))
                            Text(GroupRole.label(m.role), Modifier.clip(CircleShape).background(cs.tertiary.copy(alpha = 0.18f)).padding(horizontal = 8.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall, color = cs.tertiary)
                        }
                    }
                    Text(
                        if (m.workouts == 0) "Sem treino nesta semana" else "${m.workouts} ${if (m.workouts == 1) "treino" else "treinos"} · ${m.activeDays} ${if (m.activeDays == 1) "dia" else "dias"} · ${m.minutes} min",
                        style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant
                    )
                }
                if (!m.isMe && (canRemoveMember(myRole, m.role) || canChangeRole(myRole, m.role))) {
                    Box {
                        IconButton({ menu = true }) { Icon(Icons.Default.MoreVert, "Opções") }
                        DropdownMenu(menu, { menu = false }) {
                            if (canChangeRole(myRole, m.role)) DropdownMenuItem(
                                text = { Text(if (m.role == GroupRole.ADMIN) "Tornar membro" else "Promover a admin") },
                                onClick = { menu = false; a.setRole(m.profileId, toggledRole(m.role)) }
                            )
                            if (canRemoveMember(myRole, m.role)) DropdownMenuItem(
                                text = { Text("Remover do grupo", color = cs.error) },
                                onClick = { menu = false; a.removeMember(m.profileId) }
                            )
                        }
                    }
                }
            }
        }
        Text("De segunda a domingo. Conta o melhor de cada um: treinos terminados, dias ativos e minutos.", style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant)
    }
}

/** Medalha de ouro, prata e bronze nas três primeiras posições; as outras só o número; sem treino, um traço. */
@Composable
private fun PositionBadge(pos: Int?) {
    val color = when (pos) { 1 -> Color(0xFFFFC53D); 2 -> Color(0xFFC9D1D9); 3 -> Color(0xFFCD7F32); else -> null }
    Box(
        Modifier.size(28.dp).clip(CircleShape).background(color ?: Color.Transparent).then(if (color == null) Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape) else Modifier),
        Alignment.Center
    ) {
        Text(pos?.toString() ?: "-", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = if (color != null) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ───────────────────────── Aba Mural ─────────────────────────

@Composable
private fun WallTab(d: GroupSheetData, a: GroupSheetActions, myRole: String?) {
    val cs = MaterialTheme.colorScheme
    var text by rememberSaveable { mutableStateOf("") }
    val byId = remember(d.board) { d.board.associateBy { it.profileId } }
    val now = remember(d.messages) { Clock.System.now() }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Mural", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Row(verticalAlignment = Alignment.Bottom) {
            OutlinedTextField(
                text, { text = it.take(MAX_MESSAGE_LENGTH) }, Modifier.weight(1f), placeholder = { Text("Escreva para o grupo...") }, maxLines = 4,
                shape = RoundedCornerShape(16.dp), supportingText = { Text("${text.length}/$MAX_MESSAGE_LENGTH") }
            )
            Spacer(Modifier.width(8.dp))
            IconButton({ a.post(text); text = "" }, enabled = text.isNotBlank()) { Icon(Icons.AutoMirrored.Filled.Send, "Enviar", tint = if (text.isNotBlank()) cs.primary else cs.onSurfaceVariant) }
        }
        if (d.messages.isEmpty()) Text("Ninguém escreveu ainda. Puxa o assunto!", style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
        d.messages.forEach { msg ->
            val author = byId[msg.profileId]
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(cs.surfaceContainerHigh).padding(12.dp), verticalAlignment = Alignment.Top) {
                UserAvatar(author?.avatarUrl, 34.dp, fallbackName = author?.username)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(if (author?.isMe == true) "Você" else author?.username?.let { "@$it" } ?: "ex-membro", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(8.dp))
                        Text(messageAge(msg.createdAt, now), style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant)
                    }
                    Text(msg.body, style = MaterialTheme.typography.bodyMedium)
                }
                if (canDeleteMessage(myRole, msg.profileId, d.me)) IconButton({ a.deleteMessage(msg.id) }, Modifier.size(32.dp)) {
                    Icon(Icons.Default.Close, "Apagar", Modifier.size(16.dp), tint = cs.onSurfaceVariant)
                }
            }
        }
    }
}
