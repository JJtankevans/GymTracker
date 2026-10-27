package com.vitoroliveira.gymtracker.ui.social

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Surface
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vitoroliveira.gymtracker.data.model.SocialGroup
import com.vitoroliveira.gymtracker.data.model.SocialProfile
import com.vitoroliveira.gymtracker.presentation.social.ChallengeItem
import com.vitoroliveira.gymtracker.presentation.social.ChallengeStatus
import com.vitoroliveira.gymtracker.presentation.social.FeedItem
import com.vitoroliveira.gymtracker.presentation.social.PersonItem
import com.vitoroliveira.gymtracker.ui.profile.UserAvatar
import com.vitoroliveira.gymtracker.presentation.social.ReactionKind
import com.vitoroliveira.gymtracker.presentation.social.Relation
import com.vitoroliveira.gymtracker.presentation.social.SocialUiState
import com.vitoroliveira.gymtracker.presentation.social.SocialViewModel
import com.vitoroliveira.gymtracker.presentation.social.feedGroupName
import com.vitoroliveira.gymtracker.presentation.social.formatFeedStats
import com.vitoroliveira.gymtracker.ui.theme.ThemedCard
import com.vitoroliveira.gymtracker.ui.theme.ThemedScaffold
import com.vitoroliveira.gymtracker.ui.theme.ThemedTopAppBar
import org.koin.compose.viewmodel.koinViewModel

private val TABS = listOf("Feed", "Grupos", "Desafios", "Pessoas")

/** O que a tela pode pedir ao ViewModel. Separado pra a parte visual poder ser testada sem rede. */
class SocialActions(
    val refresh: () -> Unit = {},
    val clear: () -> Unit = {},
    val saveSettings: (com.vitoroliveira.gymtracker.data.model.SocialSettings) -> Unit = {},
    val follow: (String) -> Unit = {},
    val unfollow: (String) -> Unit = {},
    val acceptFollower: (String) -> Unit = {},
    val rejectFollower: (String) -> Unit = {},
    val react: (String, ReactionKind) -> Unit = { _, _ -> },
    val editPost: (String, String?, String) -> Unit = { _, _, _ -> },
    val createGroup: (com.vitoroliveira.gymtracker.presentation.social.GroupForm) -> Unit = {},
    val joinGroup: (String) -> Unit = {},
    val joinCode: (String) -> Unit = {},
    val leaveGroup: (String) -> Unit = {},
    val createChallenge: (com.vitoroliveira.gymtracker.presentation.social.ChallengeForm) -> Unit = {},
    val joinChallenge: (String, String?) -> Unit = { _, _ -> },
    val leaveChallenge: (String) -> Unit = {},
    val loadStandings: (String) -> Unit = {},
    val openProfile: (PersonItem) -> Unit = {},
    val closeProfile: () -> Unit = {}
)

private fun SocialViewModel.actions() = SocialActions(
    refresh = ::refresh, clear = ::clearMessages, saveSettings = ::saveSettings,
    follow = ::follow, unfollow = ::unfollow, acceptFollower = ::acceptFollower, rejectFollower = ::rejectFollower,
    react = ::toggleReaction, editPost = ::editPost, createGroup = ::createGroup, joinGroup = ::joinGroup, joinCode = ::joinGroupByCode,
    leaveGroup = ::leaveGroup, createChallenge = ::createChallenge, joinChallenge = ::joinChallenge,
    leaveChallenge = ::leaveChallenge, loadStandings = ::loadStandings,
    openProfile = ::openProfile, closeProfile = ::closeProfile
)

@Composable
fun SocialScreen(onBack: () -> Unit, vm: SocialViewModel = koinViewModel()) {
    val state by vm.state.collectAsState()
    SocialContent(state, vm.actions(), onBack)
}

@Composable
fun SocialContent(state: SocialUiState, a: SocialActions, onBack: () -> Unit) {
    var tab by remember { mutableIntStateOf(0) }
    var showSettings by remember { mutableStateOf(false) }
    var showGroupDialog by remember { mutableStateOf(false) }
    var showCodeDialog by remember { mutableStateOf(false) }
    var showChallengeDialog by remember { mutableStateOf(false) }
    var openChallenge by remember { mutableStateOf<ChallengeItem?>(null) }
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(state.message, state.error) {
        (state.error ?: state.message)?.let { snackbar.showSnackbar(it); a.clear() }
    }

    ThemedScaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            ThemedTopAppBar(
                title = { Text("Social") },
                navigationIcon = { IconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar") } },
                actions = {
                    IconButton({ a.refresh() }) { Icon(Icons.Default.Refresh, "Atualizar") }
                    IconButton({ showSettings = true }) { Icon(Icons.Default.Settings, "Privacidade") }
                }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            ScrollableTabRow(selectedTabIndex = tab, edgePadding = 8.dp, containerColor = androidx.compose.ui.graphics.Color.Transparent) {
                TABS.forEachIndexed { i, t -> Tab(tab == i, { tab = i }, text = { Text(t) }) }
            }
            if (state.loadError != null && !state.isLoading) {
                Box(Modifier.fillMaxSize(), Alignment.Center) { com.vitoroliveira.gymtracker.ui.components.ErrorState(state.loadError!!, a.refresh) }
            } else if (state.isLoading && state.feed.isEmpty() && state.myGroups.isEmpty() && state.challenges.isEmpty()) {
                Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
            } else when (tab) {
                0 -> FeedTab(state, a, onOpenSettings = { showSettings = true })
                1 -> GroupsTab(state, a, onCreate = { showGroupDialog = true }, onCode = { showCodeDialog = true })
                2 -> ChallengesTab(state, a, onCreate = { showChallengeDialog = true }, onOpen = { openChallenge = it })
                else -> PeopleTab(state, a)
            }
        }
    }

    val settings = state.settings
    if (showSettings && settings != null) {
        SettingsDialog(settings, onSave = { a.saveSettings(it); showSettings = false }, onDismiss = { showSettings = false })
    }
    if (showGroupDialog) GroupDialog(onConfirm = { a.createGroup(it); showGroupDialog = false }, onDismiss = { showGroupDialog = false })
    if (showCodeDialog) CodeDialog(onConfirm = { a.joinCode(it); showCodeDialog = false }, onDismiss = { showCodeDialog = false })
    if (showChallengeDialog) ChallengeDialog(
        myGroups = state.myGroups,
        onConfirm = { a.createChallenge(it); showChallengeDialog = false },
        onDismiss = { showChallengeDialog = false }
    )
    state.profileView?.let { pv ->
        // relação atualizada (seguir/deixar de seguir muda o botão sem fechar a folha)
        val live = state.people.firstOrNull { it.profile.id == pv.person.profile.id }
        ProfileSheet(
            view = if (live != null) pv.copy(person = live) else pv,
            title = state.titles[pv.person.profile.id],
            onFollow = { a.follow(pv.person.profile.id) },
            onUnfollow = { a.unfollow(pv.person.profile.id) },
            onDismiss = a.closeProfile
        )
    }
    openChallenge?.let { item ->
        val current = state.challenges.firstOrNull { it.challenge.id == item.challenge.id } ?: item
        ChallengeSheet(
            item = current, standings = state.standings[item.challenge.id], me = state.me, titles = state.titles,
            onLoad = { a.loadStandings(item.challenge.id) },
            onJoin = { team -> a.joinChallenge(item.challenge.id, team) },
            onLeave = { a.leaveChallenge(item.challenge.id) },
            onDismiss = { openChallenge = null }
        )
    }
}

@Composable
private fun Empty(title: String, text: String, action: (@Composable () -> Unit)? = null) {
    Column(Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (action != null) { Spacer(Modifier.height(14.dp)); action() }
    }
}

// ───────────────────────── Feed ─────────────────────────

@Composable
private fun FeedTab(state: SocialUiState, a: SocialActions, onOpenSettings: () -> Unit) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (state.settings?.shareWorkouts == false) item(key = "optin") {
            ThemedCard(Modifier.fillMaxWidth(), containerColor = MaterialTheme.colorScheme.primaryContainer) {
                Column(Modifier.padding(14.dp)) {
                    Text("Seus treinos não aparecem no feed", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text(
                        "Por padrão tudo é privado. Ative o compartilhamento para seus treinos finalizados virarem posts.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    TextButton(onOpenSettings) { Text("Abrir privacidade") }
                }
            }
        }
        if (state.feed.isEmpty()) item(key = "empty") {
            Empty("Feed vazio", "Quando você (ou quem você segue) finalizar um treino compartilhado, ele aparece aqui.")
        }
        items(state.feed, key = { it.post.id }) {
            FeedCard(it, state.titles[it.post.profileId], onEdit = { c, v -> a.editPost(it.post.id, c, v) }) { kind -> a.react(it.post.id, kind) }
        }
    }
}

@Composable
private fun FeedCard(
    item: FeedItem, title: com.vitoroliveira.gymtracker.data.model.ProfileTitle?,
    onEdit: (String?, String) -> Unit, onReact: (ReactionKind) -> Unit
) {
    var editing by remember { mutableStateOf(false) }
    if (editing) PostDialog(item.post.caption.orEmpty(), item.post.visibility, onSave = { c, v -> onEdit(c, v); editing = false }, onDismiss = { editing = false })
    ThemedCard(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                UserAvatar(url = item.avatarUrl, size = 36.dp, fallbackName = item.author)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(item.author, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    title?.let { TitleChip(it, Modifier.padding(top = 2.dp)) }
                }
                if (item.isMine) TextButton({ editing = true }) { Text("Editar") }
                Text(
                    item.post.createdAt?.take(10)?.split("-")?.let { if (it.size == 3) "${it[2]}/${it[1]}" else "" }.orEmpty(),
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            val group = feedGroupName(item.post.stats)
            Text(
                "Treino finalizado" + (group?.let { " · $it" } ?: ""),
                style = MaterialTheme.typography.bodyMedium
            )
            formatFeedStats(item.post.stats).takeIf { it.isNotEmpty() }?.let {
                Text(it, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
            }
            item.post.caption?.takeIf { it.isNotBlank() }?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ReactionKind.entries.forEach { k ->
                    ReactionPill(k, item.reactions.count(k), k in item.reactions.mine) { onReact(k) }
                }
            }
        }
    }
}

/** Botão de reação: ícone desenhado na cor do tema (cheio quando é seu) + contagem. */
@Composable
private fun ReactionPill(kind: ReactionKind, count: Int, mine: Boolean, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val tint = if (mine) cs.primary else cs.onSurfaceVariant
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = if (mine) cs.primary.copy(alpha = 0.16f) else Color.Transparent,
        border = BorderStroke(1.dp, if (mine) cs.primary else cs.outlineVariant)
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(kind.icon(), kind.label, Modifier.size(20.dp), tint = tint)
            if (count > 0) {
                Spacer(Modifier.width(6.dp))
                Text("$count", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = tint)
            }
        }
    }
}

private fun ReactionKind.icon(): ImageVector = when (this) {
    ReactionKind.FLEX -> Icons.Default.FitnessCenter
    ReactionKind.FIRE -> Icons.Default.LocalFireDepartment
    ReactionKind.CLAP -> Icons.Default.ThumbUp
    ReactionKind.HEADPHONES -> Icons.Default.Headphones
}

// ───────────────────────── Grupos ─────────────────────────

@Composable
private fun GroupsTab(state: SocialUiState, a: SocialActions, onCreate: () -> Unit, onCode: () -> Unit) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item(key = "actions") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onCreate, Modifier.weight(1f)) { Icon(Icons.Default.Add, null); Spacer(Modifier.width(6.dp)); Text("Criar grupo") }
                OutlinedButton(onCode, Modifier.weight(1f)) { Text("Tenho um código") }
            }
        }
        item(key = "mine-title") { Text("Meus grupos", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
        if (state.myGroups.isEmpty()) item(key = "mine-empty") {
            Text("Você ainda não está em nenhum grupo.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        items(state.myGroups, key = { "m" + it.id }) { g ->
            GroupCard(g, mine = true, isOwner = g.ownerId == state.me,
                onAction = { a.leaveGroup(g.id) })
        }
        item(key = "pub-title") { Spacer(Modifier.height(6.dp)); Text("Grupos públicos", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
        if (state.publicGroups.isEmpty()) item(key = "pub-empty") {
            Text("Nenhum grupo público por aqui ainda.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        items(state.publicGroups, key = { "p" + it.id }) { g -> GroupCard(g, mine = false, isOwner = false, onAction = { a.joinGroup(g.id) }) }
    }
}

@Composable
private fun GroupCard(g: SocialGroup, mine: Boolean, isOwner: Boolean, onAction: () -> Unit) {
    ThemedCard(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(g.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(
                    (if (g.isPublic) "Público" else "Por convite") + (g.maxMembers?.let { " · até $it membros" } ?: ""),
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                g.description?.takeIf { it.isNotBlank() }?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                if (isOwner && g.inviteCode != null) Text(
                    "Código de convite: ${g.inviteCode}", style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary
                )
            }
            when {
                mine && isOwner -> Text("Dono", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.tertiary)
                mine -> OutlinedButton(onAction) { Text("Sair") }
                else -> Button(onAction) { Text("Entrar") }
            }
        }
    }
}

// ───────────────────────── Desafios ─────────────────────────

@Composable
private fun ChallengesTab(state: SocialUiState, a: SocialActions, onCreate: () -> Unit, onOpen: (ChallengeItem) -> Unit) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item(key = "new") { Button(onCreate, Modifier.fillMaxWidth()) { Icon(Icons.Default.Add, null); Spacer(Modifier.width(6.dp)); Text("Novo desafio") } }
        if (state.challenges.isEmpty()) item(key = "empty") {
            Empty("Nenhum desafio", "Crie um desafio aberto ou dentro de um grupo e chame a galera. O placar sai dos treinos reais de cada um.")
        }
        items(state.challenges, key = { it.challenge.id }) { ChallengeCard(it, onClick = { onOpen(it) }, onJoin = { if (it.challenge.mode == "team") onOpen(it) else a.joinChallenge(it.challenge.id, null) }) }
    }
}

@Composable
private fun ChallengeCard(item: ChallengeItem, onClick: () -> Unit, onJoin: () -> Unit) {
    ThemedCard(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium, onClick = onClick) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(item.challenge.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(
                    (item.metric?.label ?: item.challenge.metric) + (item.challenge.exerciseName?.let { " · $it" } ?: ""),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    buildString {
                        append(item.status.label)
                        if (item.status == ChallengeStatus.ACTIVE) append(" · faltam ${item.daysLeft} ${if (item.daysLeft == 1) "dia" else "dias"}")
                        item.groupName?.let { append(" · $it") }
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = if (item.status == ChallengeStatus.ACTIVE) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (item.joined) Text("Participando", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            else if (item.status != ChallengeStatus.ENDED) Button(onJoin) { Text("Entrar") }
        }
    }
}

// ───────────────────────── Pessoas ─────────────────────────

@Composable
private fun PeopleTab(state: SocialUiState, a: SocialActions) {
    var query by remember { mutableStateOf("") }
    val list = state.people.filter { query.isBlank() || it.profile.displayName.contains(query, true) || it.profile.username.contains(query, true) }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (state.followRequests.isNotEmpty()) {
            item(key = "req-title") { Text("Pedidos para te seguir", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
            items(state.followRequests, key = { "r" + it.id }) { p -> RequestRow(p, onAccept = { a.acceptFollower(p.id) }, onReject = { a.rejectFollower(p.id) }) }
        }
        item(key = "search") {
            OutlinedTextField(
                query, { query = it }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Buscar pessoas") }
            )
        }
        if (list.isEmpty()) item(key = "empty") {
            Empty(
                "Ninguém por aqui",
                "Aparecem perfis públicos e os que aceitam seguidores. Peça pra galera ajustar a privacidade do perfil."
            )
        }
        items(list, key = { "u" + it.profile.id }) { PersonRow(it, a, state.titles[it.profile.id]) }
    }
}

@Composable
private fun RequestRow(p: SocialProfile, onAccept: () -> Unit, onReject: () -> Unit) {
    ThemedCard(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(p.displayName, Modifier.weight(1f), fontWeight = FontWeight.Bold)
            TextButton(onReject) { Text("Recusar") }
            Button(onAccept) { Text("Aceitar") }
        }
    }
}

@Composable
private fun PersonRow(item: PersonItem, a: SocialActions, title: com.vitoroliveira.gymtracker.data.model.ProfileTitle? = null) {
    val p = item.profile
    ThemedCard(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium, onClick = { a.openProfile(item) }) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            UserAvatar(url = p.avatarUrl, size = 44.dp, fallbackName = p.displayName)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(p.displayName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(
                    "@${p.username}" + if (item.followsMe) " · segue você" else "",
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                title?.let { TitleChip(it, Modifier.padding(top = 4.dp)) }
                p.bio?.takeIf { it.isNotBlank() }?.let { Text(it, style = MaterialTheme.typography.bodySmall, maxLines = 2) }
            }
            when (item.relation) {
                Relation.NONE -> Button({ a.follow(p.id) }) { Text("Seguir") }
                Relation.REQUESTED -> OutlinedButton({ a.unfollow(p.id) }) { Text("Pedido enviado") }
                Relation.FOLLOWING -> OutlinedButton({ a.unfollow(p.id) }) { Text("Seguindo") }
            }
        }
    }
}
