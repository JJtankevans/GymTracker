@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.vitoroliveira.gymtracker.ui.social

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.vitoroliveira.gymtracker.data.catalog.ExerciseCatalog
import com.vitoroliveira.gymtracker.data.model.SocialGroup
import com.vitoroliveira.gymtracker.data.model.SocialSettings
import com.vitoroliveira.gymtracker.data.model.Standing
import com.vitoroliveira.gymtracker.presentation.social.ChallengeForm
import com.vitoroliveira.gymtracker.presentation.social.ChallengeItem
import com.vitoroliveira.gymtracker.presentation.social.ChallengeMessages
import com.vitoroliveira.gymtracker.presentation.social.challengeMood
import com.vitoroliveira.gymtracker.ui.components.todayEpochDay
import com.vitoroliveira.gymtracker.ui.profile.UserAvatar
import com.vitoroliveira.gymtracker.presentation.social.ChallengeMetric
import com.vitoroliveira.gymtracker.presentation.social.ChallengeStatus
import com.vitoroliveira.gymtracker.presentation.social.GroupForm
import com.vitoroliveira.gymtracker.presentation.social.visibilityLabel
import com.vitoroliveira.gymtracker.ui.theme.ThemedCard

// ───────────────────────── Privacidade ─────────────────────────

@Composable
fun SettingsDialog(settings: SocialSettings, onSave: (SocialSettings) -> Unit, onDismiss: () -> Unit) {
    var bio by remember { mutableStateOf(settings.bio.orEmpty()) }
    var visibility by remember { mutableStateOf(settings.visibility) }
    var shareWorkouts by remember { mutableStateOf(settings.shareWorkouts) }
    // a captura de músicas saiu (Play Protect); o valor guardado continua o mesmo
    val shareMusic = settings.shareMusic

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Privacidade") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Tudo começa privado. Você escolhe o que os outros podem ver.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    bio, { bio = it.take(280) }, label = { Text("Bio (até 280 caracteres)") },
                    modifier = Modifier.fillMaxWidth(), minLines = 2
                )
                Text("Quem pode ver seu perfil", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("public", "followers", "private").forEach { v ->
                        FilterChip(visibility == v, { visibility = v }, { Text(visibilityLabel(v)) })
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Compartilhar treinos", fontWeight = FontWeight.Bold)
                        Text("Treinos finalizados viram posts no feed.", style = MaterialTheme.typography.bodySmall)
                    }
                    Switch(shareWorkouts, { shareWorkouts = it })
                }
            }
        },
        confirmButton = { Button({ onSave(SocialSettings(bio, visibility, shareWorkouts, shareMusic)) }) { Text("Salvar") } },
        dismissButton = { TextButton(onDismiss) { Text("Cancelar") } }
    )
}

// ───────────────────────── Grupos ─────────────────────────

@Composable
fun GroupDialog(onConfirm: (GroupForm) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var isPublic by remember { mutableStateOf(false) }
    var maxText by remember { mutableStateOf("") }
    var look by remember { mutableStateOf(com.vitoroliveira.gymtracker.presentation.social.Look()) }
    var goalText by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Novo grupo") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                IdentityPicker(look, onChange = { look = it })
                OutlinedTextField(name, { name = it.take(60) }, label = { Text("Nome") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(description, { description = it.take(500) }, label = { Text("Descrição (opcional)") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                OutlinedTextField(
                    maxText, { maxText = it.filter(Char::isDigit).take(3) }, label = { Text("Limite de membros (opcional)") },
                    singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    goalText, { goalText = it.filter(Char::isDigit).take(3) }, label = { Text("Meta de treinos do grupo por semana (opcional)") },
                    singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth()
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Grupo público", fontWeight = FontWeight.Bold)
                        Text(
                            if (isPublic) "Qualquer um pode entrar." else "Só entra quem tiver o código de convite.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Switch(isPublic, { isPublic = it })
                }
            }
        },
        confirmButton = { Button({ onConfirm(GroupForm(name, description, isPublic, maxText.toIntOrNull(), look, goalText.toIntOrNull())) }) { Text("Criar") } },
        dismissButton = { TextButton(onDismiss) { Text("Cancelar") } }
    )
}

@Composable
fun CodeDialog(onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var code by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Entrar com código") },
        text = {
            OutlinedTextField(code, { code = it.take(12) }, label = { Text("Código do convite") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        },
        confirmButton = { Button({ onConfirm(code) }, enabled = code.isNotBlank()) { Text("Entrar") } },
        dismissButton = { TextButton(onDismiss) { Text("Cancelar") } }
    )
}

// ───────────────────────── Desafios ─────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChallengeDialog(myGroups: List<SocialGroup>, onConfirm: (ChallengeForm) -> Unit, onDismiss: () -> Unit, initialGroupId: String? = null) {
    var title by remember { mutableStateOf("") }
    var metric by remember { mutableStateOf(ChallengeMetric.WORKOUTS) }
    var exercise by remember { mutableStateOf("") }
    var days by remember { mutableStateOf("30") }
    var groupId by remember { mutableStateOf(initialGroupId) }
    var minMinutes by remember { mutableStateOf("20") }
    var maxPerDay by remember { mutableStateOf("1") }
    var target by remember { mutableStateOf("") }
    var teamsText by remember { mutableStateOf("") }
    var failText by remember { mutableStateOf("") }
    var streakText by remember { mutableStateOf("") }
    var look by remember { mutableStateOf(com.vitoroliveira.gymtracker.presentation.social.Look()) }
    var prize by remember { mutableStateOf("") }
    var metricOpen by remember { mutableStateOf(false) }
    var groupOpen by remember { mutableStateOf(false) }
    var exOpen by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Novo desafio") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                IdentityPicker(look, onChange = { look = it })
                OutlinedTextField(title, { title = it.take(80) }, label = { Text("Título") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(prize, { prize = it.take(120) }, label = { Text("Prêmio (opcional): quem perder paga o açaí...") }, singleLine = true, modifier = Modifier.fillMaxWidth())

                ExposedDropdownMenuBox(metricOpen, { metricOpen = it }) {
                    OutlinedTextField(
                        metric.label, {}, readOnly = true, label = { Text("O que conta pontos") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(metricOpen) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(metricOpen, { metricOpen = false }) {
                        ChallengeMetric.entries.forEach { m ->
                            DropdownMenuItem({ Text(m.label) }, { metric = m; metricOpen = false })
                        }
                    }
                }

                if (metric.needsExercise) {
                    ExposedDropdownMenuBox(exOpen, { exOpen = it }) {
                        OutlinedTextField(
                            exercise, { exercise = it }, label = { Text("Exercício") }, singleLine = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(exOpen) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(exOpen, { exOpen = false }) {
                            ExerciseCatalog.search(exercise).take(8).forEach { ex ->
                                DropdownMenuItem({ Text(ex.name) }, { exercise = ex.name; exOpen = false })
                            }
                        }
                    }
                }

                ExposedDropdownMenuBox(groupOpen, { groupOpen = it }) {
                    OutlinedTextField(
                        myGroups.firstOrNull { it.id == groupId }?.name ?: "Aberto (qualquer um)", {}, readOnly = true,
                        label = { Text("Onde") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(groupOpen) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(groupOpen, { groupOpen = false }) {
                        DropdownMenuItem({ Text("Aberto (qualquer um)") }, { groupId = null; groupOpen = false })
                        myGroups.forEach { g -> DropdownMenuItem({ Text(g.name) }, { groupId = g.id; groupOpen = false }) }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumberField("Dias", days, { days = it }, Modifier.weight(1f))
                    NumberField("Treinos/dia", maxPerDay, { maxPerDay = it }, Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumberField("Mín. minutos", minMinutes, { minMinutes = it }, Modifier.weight(1f))
                    NumberField("Meta (opcional)", target, { target = it }, Modifier.weight(1f))
                }
                OutlinedTextField(
                    teamsText, { teamsText = it.take(120) }, label = { Text("Equipes (opcional)") },
                    placeholder = { Text("Ex.: Azuis, Vermelhos") }, singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                Text("Títulos no perfil (opcional)", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumberField("Falhas permitidas", failText, { failText = it }, Modifier.weight(1f))
                    NumberField("Dias seguidos", streakText, { streakText = it }, Modifier.weight(1f))
                }
                Text(
                    "Falha = treino abaixo do mínimo de minutos; passou das falhas permitidas, o perfil ganha um título de falha. " +
                        "Dias seguidos com treino válido dão os títulos Alpha, Chad (2x), Giga Chad (3x) e Mr. Olimpia (4x). " +
                        (streakText.toIntOrNull()?.takeIf { it > 0 }?.let { "Com $it: " + com.vitoroliveira.gymtracker.presentation.social.ProfileTitles.ladder(it) + "." } ?: ""),
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "As regras de mínimo de minutos e máximo de treinos por dia dificultam trapaça. O placar vem dos treinos reais.",
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button({
                onConfirm(
                    ChallengeForm(
                        title = title, metric = metric, exerciseName = exercise.takeIf { metric.needsExercise },
                        days = days.toIntOrNull() ?: 0, groupId = groupId,
                        minMinutesPerWorkout = minMinutes.toIntOrNull() ?: 0,
                        maxWorkoutsPerDay = maxPerDay.toIntOrNull() ?: 1,
                        target = target.toDoubleOrNull(),
                        teams = teamsText.split(",").map { it.trim() }.filter { it.isNotEmpty() },
                        failThreshold = failText.toIntOrNull(),
                        streakGoal = streakText.toIntOrNull(),
                        look = look,
                        prize = prize
                    )
                )
            }) { Text("Criar") }
        },
        dismissButton = { TextButton(onDismiss) { Text("Cancelar") } }
    )
}

@Composable
private fun NumberField(label: String, value: String, onChange: (String) -> Unit, modifier: Modifier) {
    OutlinedTextField(
        value, { onChange(it.filter { c -> c.isDigit() }.take(5)) }, label = { Text(label, maxLines = 1) }, singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = modifier
    )
}

/** Detalhe do desafio: regras, entrar/sair e o placar ao vivo. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChallengeSheet(
    item: ChallengeItem, standings: List<Standing>?, me: String?,
    titles: Map<String, com.vitoroliveira.gymtracker.data.model.ProfileTitle> = emptyMap(),
    onLoad: () -> Unit, onJoin: (String?) -> Unit, onLeave: () -> Unit, onDismiss: () -> Unit
) {
    LaunchedEffect(item.challenge.id) { onLoad() }
    val c = item.challenge
    var team by remember { mutableStateOf<String?>(null) }
    val isTeam = c.mode == "team" && c.teams.isNotEmpty()
    val mine = standings?.firstOrNull { it.profileId == me }
    val progress = mine?.let { com.vitoroliveira.gymtracker.presentation.social.progressToTarget(it.score, c.target) }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            Modifier.fillMaxWidth().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (c.cover != null || c.accentHex != null || c.icon != null) {
                CoverBox(c.look, Modifier.fillMaxWidth().height(130.dp), animated = true) {
                    Row(Modifier.align(Alignment.BottomStart).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        IdentityBadge(c.look, 48.dp)
                        Spacer(Modifier.width(12.dp))
                        Text(c.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = androidx.compose.ui.graphics.Color.White)
                    }
                }
            } else Text(c.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            c.prize?.takeIf { it.isNotBlank() }?.let {
                Text("Prêmio: $it", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.tertiary, fontWeight = FontWeight.Bold)
            }
            Text(
                (item.metric?.label ?: c.metric) + (c.exerciseName?.let { " · $it" } ?: ""),
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            c.description?.takeIf { it.isNotBlank() }?.let { Text(it) }
            Text(
                item.status.label + if (item.status == ChallengeStatus.ACTIVE) " · faltam ${item.daysLeft} ${if (item.daysLeft == 1) "dia" else "dias"}" else "",
                style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.tertiary
            )
            Text(
                "Regras: mínimo de ${c.minMinutesPerWorkout} min por treino, até ${c.maxWorkoutsPerDay} treino(s) por dia" +
                    (c.target?.let { " · meta ${it.toLong()}" } ?: "") + (item.groupName?.let { " · grupo $it" } ?: ""),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            // mensagem do dia (muda uma vez por dia e fica fixa durante o dia)
            val mood = if (item.joined) challengeMood(item.status, c.target, standings, me) else null
            if (mood != null) {
                val message = remember(c.id, me, mood, todayEpochDay()) { ChallengeMessages.pick(mood, "${c.id}|$me|${todayEpochDay()}".hashCode()) }
                ChallengeMessageCard(message, Modifier.padding(vertical = 4.dp))
            }
            if (c.failThreshold != null || c.streakGoal != null) {
                Text(
                    listOfNotNull(
                        c.failThreshold?.let { "Mais de $it treino(s) abaixo do mínimo = título de falha no perfil." },
                        c.streakGoal?.let { "Dias seguidos: " + com.vitoroliveira.gymtracker.presentation.social.ProfileTitles.ladder(it) + "." }
                    ).joinToString(" "),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (progress != null && mine != null) {
                Text(
                    "Sua meta: ${item.metric?.format(mine.score) ?: mine.score.toLong()} de ${item.metric?.format(c.target ?: 0.0) ?: c.target?.toLong()}",
                    style = MaterialTheme.typography.labelLarge
                )
                androidx.compose.material3.LinearProgressIndicator(
                    progress = { progress }, modifier = Modifier.fillMaxWidth().height(8.dp)
                )
            }
            if (item.joined) OutlinedButton(onLeave, Modifier.fillMaxWidth()) { Text("Sair do desafio") }
            else if (item.status != ChallengeStatus.ENDED) {
                if (isTeam) {
                    Text("Escolha sua equipe", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        c.teams.forEach { t -> FilterChip(team == t, { team = t }, { Text(t) }) }
                    }
                }
                Button({ onJoin(team) }, Modifier.fillMaxWidth(), enabled = !isTeam || team != null) {
                    Text(if (isTeam) "Entrar na equipe${team?.let { " $it" } ?: ""}" else "Entrar no desafio")
                }
            }

            if (isTeam && !standings.isNullOrEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text("Equipes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                com.vitoroliveira.gymtracker.presentation.social.teamTotals(standings, item.metric).forEachIndexed { i, (name, total) ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("${i + 1}º", Modifier.size(width = 40.dp, height = 24.dp), fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                        Text(name, Modifier.weight(1f), fontWeight = FontWeight.Bold)
                        Text(item.metric?.format(total) ?: total.toLong().toString(), fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(Modifier.height(4.dp))
            Text("Placar", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            when {
                standings == null -> CircularProgressIndicator(Modifier.size(28.dp))
                standings.isEmpty() -> Text("Ninguém participando ainda.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                else -> standings.forEach { s ->
                    ThemedCard(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.small) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("${s.rank}º", Modifier.size(width = 40.dp, height = 24.dp), fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                            UserAvatar(url = s.avatarUrl, size = 36.dp, fallbackName = s.username)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    s.username + (s.team?.takeIf { it.isNotBlank() }?.let { " · $it" } ?: "") + if (s.profileId == me) " (você)" else "",
                                    fontWeight = if (s.profileId == me) FontWeight.Bold else FontWeight.Normal
                                )
                                titles[s.profileId]?.let { TitleChip(it, Modifier.padding(top = 2.dp)) }
                            }
                            Spacer(Modifier.width(12.dp))
                            Text(item.metric?.format(s.score) ?: s.score.toLong().toString(), fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                        }
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

/** Perfil de outra pessoa: bio, números e as músicas mais tocadas nos treinos (se ela liberou). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSheet(
    view: com.vitoroliveira.gymtracker.presentation.social.ProfileView,
    onFollow: () -> Unit, onUnfollow: () -> Unit, onDismiss: () -> Unit,
    title: com.vitoroliveira.gymtracker.data.model.ProfileTitle? = null
) {
    val p = view.person.profile
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            Modifier.fillMaxWidth().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                UserAvatar(url = p.avatarUrl, size = 64.dp, fallbackName = p.displayName)
                Spacer(Modifier.width(14.dp))
                Text(p.displayName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            Text(
                "@${p.username} · ${visibilityLabel(p.visibility)}" + if (view.person.followsMe) " · segue você" else "",
                style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            title?.let { TitleChip(it) }
            p.bio?.takeIf { it.isNotBlank() }?.let { Text(it) }

            when (view.person.relation) {
                com.vitoroliveira.gymtracker.presentation.social.Relation.NONE -> Button(onFollow, Modifier.fillMaxWidth()) { Text("Seguir") }
                com.vitoroliveira.gymtracker.presentation.social.Relation.REQUESTED -> OutlinedButton(onUnfollow, Modifier.fillMaxWidth()) { Text("Cancelar pedido") }
                com.vitoroliveira.gymtracker.presentation.social.Relation.FOLLOWING -> OutlinedButton(onUnfollow, Modifier.fillMaxWidth()) { Text("Deixar de seguir") }
            }

            if (view.isLoading) CircularProgressIndicator(Modifier.size(28.dp))
            view.stats?.let { s ->
                Text("Números", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                ThemedCard(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium) {
                    Row(Modifier.padding(vertical = 14.dp, horizontal = 8.dp)) {
                        Stat("${s.workouts}", "treinos", Modifier.weight(1f))
                        Stat("${s.activeDays}", "dias ativos", Modifier.weight(1f))
                        Stat("${s.sets}", "séries", Modifier.weight(1f))
                        Stat(com.vitoroliveira.gymtracker.presentation.social.formatInt(s.volumeKg), "kg", Modifier.weight(1f))
                    }
                }
            }
            if (!view.isLoading && view.stats == null) {
                Text("Esta pessoa não compartilha os números.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (view.tracks.isNotEmpty()) {
                Text("Mais tocadas nos treinos", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                view.tracks.forEachIndexed { i, t ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("${i + 1}", Modifier.size(width = 28.dp, height = 22.dp), fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                        Column(Modifier.weight(1f)) {
                            Text(t.title, fontWeight = FontWeight.Bold, maxLines = 1)
                            t.artist?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1) }
                        }
                        Text("${t.plays}x", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun Stat(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary, maxLines = 1)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Editar legenda e visibilidade de um post seu. */
@Composable
fun PostDialog(caption: String, visibility: String, onSave: (String?, String) -> Unit, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf(caption) }
    var vis by remember { mutableStateOf(visibility) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar post") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(text, { text = it.take(280) }, label = { Text("Legenda") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                Text("Quem pode ver", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("public", "followers", "private").forEach { v -> FilterChip(vis == v, { vis = v }, { Text(visibilityLabel(v)) }) }
                }
                Text("As estatísticas do treino não mudam.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = { Button({ onSave(text.ifBlank { null }, vis) }) { Text("Salvar") } },
        dismissButton = { TextButton(onDismiss) { Text("Cancelar") } }
    )
}
