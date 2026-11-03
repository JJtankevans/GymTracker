package com.vitoroliveira.gymtracker.ui.ranked

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.vitoroliveira.gymtracker.data.model.RankedBoardRow
import com.vitoroliveira.gymtracker.data.model.RankedDuel
import com.vitoroliveira.gymtracker.presentation.ranked.BoardAnalysis
import com.vitoroliveira.gymtracker.presentation.ranked.RankedUiState
import com.vitoroliveira.gymtracker.presentation.ranked.RankedViewModel
import com.vitoroliveira.gymtracker.presentation.ranked.Zone
import com.vitoroliveira.gymtracker.presentation.ranked.canDuel
import com.vitoroliveira.gymtracker.presentation.ranked.rankFor
import com.vitoroliveira.gymtracker.presentation.ranked.zoneFor
import com.vitoroliveira.gymtracker.ui.components.ErrorState
import com.vitoroliveira.gymtracker.ui.profile.UserAvatar
import com.vitoroliveira.gymtracker.ui.theme.ThemedCard
import kotlin.time.Clock
import kotlin.time.Instant
import org.koin.compose.viewmodel.koinViewModel

/** Aba "Ranqueada" do Social. */
@Composable
fun RankedScreen(vm: RankedViewModel = koinViewModel()) {
    val state by vm.state.collectAsState()
    RankedContent(
        state = state,
        onRefresh = vm::refresh, onJoin = vm::join, onChallenge = vm::challenge, onRespond = vm::respond, onClear = vm::clearMessages
    )
}

@Composable
fun RankedContent(
    state: RankedUiState,
    onRefresh: () -> Unit = {},
    onJoin: () -> Unit = {},
    onChallenge: (String, Boolean) -> Unit = { _, _ -> },
    onRespond: (String, Boolean) -> Unit = { _, _ -> },
    onClear: () -> Unit = {}
) {
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(state.message, state.error) {
        (state.error ?: state.message)?.let { snackbar.showSnackbar(it); onClear() }
    }
    var duelWith by remember { mutableStateOf<RankedBoardRow?>(null) }

    Box(Modifier.fillMaxSize()) {
        when {
            state.loadError != null -> Box(Modifier.fillMaxSize(), Alignment.Center) { ErrorState(state.loadError!!, onRefresh) }
            state.isLoading && state.player == null -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
            state.player == null -> JoinCard(state.busy, onJoin)
            else -> LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item(key = "hero") { HeroCard(state) }
                state.analysis?.let { a -> item(key = "week") { WeekCard(state, a) } }
                state.lastResult?.takeIf { it.delta != null }?.let { r ->
                    item(key = "last") { LastResultCard(r.pos, r.cohortSize, r.points ?: 0, r.delta ?: 0) }
                }
                if (state.board.isNotEmpty()) item(key = "board") {
                    BoardCard(state, onDuel = { duelWith = it })
                }
                if (state.duels.isNotEmpty()) item(key = "duels") { DuelsCard(state, onRespond) }
                item(key = "how") { HowCard() }
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }

    duelWith?.let { opp ->
        DuelDialog(
            opponent = opp, sameTier = state.player?.let { canDuel(it.rp, opp.rp) } ?: false,
            onConfirm = { ranked -> onChallenge(opp.profileId, ranked); duelWith = null },
            onDismiss = { duelWith = null }
        )
    }
}

// ───────────────────────── Entrada ─────────────────────────

@Composable
private fun JoinCard(busy: Boolean, onJoin: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        RankEmblem(com.vitoroliveira.gymtracker.presentation.ranked.RankTier.GOLD, 2, 140.dp)
        Spacer(Modifier.height(16.dp))
        Text("Ranqueada", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
        Text(
            "Treine, ganhe pontos e suba de elo: Ferro, Bronze, Prata, Ouro, Platina, Diamante e Lenda. " +
                "Toda semana você disputa com uma turma do seu elo: o topo sobe, o fundo desce. E dá para marcar x1 com quem é do seu nível.",
            style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))
        Button(onJoin, enabled = !busy) { Text("Entrar na ranqueada") }
        Spacer(Modifier.height(8.dp))
        Text("Todo mundo começa em Ferro III. Os pontos vêm dos seus treinos reais.", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ───────────────────────── Elo ─────────────────────────

@Composable
private fun HeroCard(state: RankedUiState) {
    val rank = state.rank ?: return
    val tierColor = rank.tier.color()
    ThemedCard(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            RankEmblem(rank.tier, rank.division, 112.dp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(rank.label, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, color = tierColor)
                if (rank.isTop) {
                    Text("${rank.rp} RP · você está no topo", style = MaterialTheme.typography.bodyMedium)
                } else {
                    val next = rankFor(rank.rp + rank.rpToNext)
                    LinearProgressIndicator(progress = { rank.progress }, Modifier.fillMaxWidth().height(8.dp).clip(CircleShape), color = tierColor)
                    Text("${rank.rpInDivision} / 100 RP · faltam ${rank.rpToNext} para ${next.label}", style = MaterialTheme.typography.labelMedium)
                }
                state.season?.let {
                    Text("Temporada ${it.number} · semana ${it.weekInSeason} de 8" + if (it.isLastWeek) " · última semana: o RP encolhe 40% no fim" else "",
                        style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                state.player?.let {
                    Text("Melhor: ${rankFor(it.peakRp).label} · ${it.weeksPlayed} ${if (it.weeksPlayed == 1) "semana" else "semanas"} disputadas",
                        style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

// ───────────────────────── Rodada da semana ─────────────────────────

@Composable
private fun WeekCard(state: RankedUiState, a: BoardAnalysis) {
    val cs = MaterialTheme.colorScheme
    val (zoneLabel, zoneColor) = zoneStyle(a.zone)
    ThemedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Rodada da semana" + (state.daysLeft?.let { " · fecha domingo (${if (it == 1) "último dia" else "faltam $it dias"})" } ?: ""),
                style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Row(verticalAlignment = Alignment.Bottom) {
                Text("${a.myPoints}", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black, color = cs.primary)
                Text(" pts", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 6.dp))
                Spacer(Modifier.weight(1f))
                Text("${a.position}º de ${a.size}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(zoneLabel, Modifier.clip(CircleShape).background(zoneColor.copy(alpha = 0.18f)).border(1.dp, zoneColor, CircleShape).padding(horizontal = 12.dp, vertical = 5.dp),
                    style = MaterialTheme.typography.labelLarge, color = zoneColor, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(10.dp))
                Text(
                    (if (a.projectedDelta >= 0) "+" else "") + "${a.projectedDelta} RP se fechasse agora",
                    style = MaterialTheme.typography.labelLarge, color = if (a.projectedDelta >= 0) cs.primary else cs.error
                )
            }
            a.pointsToPromotion?.let { Text("Faltam $it pontos para a zona de subida.", style = MaterialTheme.typography.bodyMedium) }
            a.pointsToSafety?.let { Text("Faltam $it pontos para sair da zona de descida.", style = MaterialTheme.typography.bodyMedium, color = cs.error) }
            Text(
                "Cada treino de 20 min ou mais vale 100 pontos, mais esforço e séries. Só o melhor treino de cada dia conta. " +
                    "Treinar em 3 dias dá +50, e bater a meta semanal do perfil dá +50.",
                style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun zoneStyle(zone: Zone): Pair<String, Color> = when (zone) {
    Zone.PROMOTE -> "Zona de subida" to Color(0xFF3DDC84)
    Zone.SAFE -> "Zona segura" to MaterialTheme.colorScheme.onSurfaceVariant
    Zone.DEMOTE -> "Zona de descida" to MaterialTheme.colorScheme.error
}

@Composable
private fun LastResultCard(pos: Int?, size: Int, points: Int, delta: Int) {
    val up = delta > 0
    val color = if (up) Color(0xFF3DDC84) else if (delta < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
    ThemedCard(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Semana passada", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("${pos ?: "-"}º de $size · $points pts", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }
            Text((if (delta > 0) "+" else "") + "$delta RP", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = color)
        }
    }
}

// ───────────────────────── Turma ─────────────────────────

@Composable
private fun BoardCard(state: RankedUiState, onDuel: (RankedBoardRow) -> Unit) {
    val cs = MaterialTheme.colorScheme
    val myRp = state.player?.rp ?: 0
    val size = state.board.size
    ThemedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(vertical = 8.dp)) {
            Text("Sua turma", Modifier.padding(horizontal = 16.dp, vertical = 8.dp), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            state.board.forEach { r ->
                val (_, zoneColor) = zoneStyle(zoneFor(r.pos, size, r.points))
                val zone = zoneFor(r.pos, size, r.points)
                Row(
                    Modifier.fillMaxWidth().background(if (r.isMe) cs.primary.copy(alpha = 0.10f) else Color.Transparent).padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.width(4.dp).height(34.dp).background(if (zone == Zone.SAFE) Color.Transparent else zoneColor))
                    Text("${r.pos}º", Modifier.width(38.dp).padding(start = 6.dp), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    UserAvatar(r.avatarUrl, 32.dp, fallbackName = r.username)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(if (r.isMe) "Você" else "@${r.username}", style = MaterialTheme.typography.bodyMedium, fontWeight = if (r.isMe) FontWeight.Bold else FontWeight.Normal, maxLines = 1)
                        Text(rankFor(r.rp).label, style = MaterialTheme.typography.labelSmall, color = rankFor(r.rp).tier.color())
                    }
                    Text("${r.points} pts", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    if (!r.isMe && canDuel(myRp, r.rp)) TextButton(onClick = { onDuel(r) }, contentPadding = PaddingValues(horizontal = 8.dp)) { Text("x1") }
                }
            }
            Text(
                "Verde sobe, vermelho desce. A turma tem até 30 pessoas do mesmo elo e fecha no domingo.",
                Modifier.padding(horizontal = 16.dp, vertical = 8.dp), style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant
            )
        }
    }
}

// ───────────────────────── x1 ─────────────────────────

@Composable
private fun DuelDialog(opponent: RankedBoardRow, sameTier: Boolean, onConfirm: (ranked: Boolean) -> Unit, onDismiss: () -> Unit) {
    var ranked by remember { mutableStateOf(sameTier) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("x1 com @${opponent.username}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Valem 7 dias a partir do aceite. Quem somar mais pontos de treino vence.")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(ranked, { if (sameTier) ranked = true }, { Text("Valendo RP") }, enabled = sameTier)
                    FilterChip(!ranked, { ranked = false }, { Text("Amistoso") })
                }
                Text(
                    if (ranked) "Quem vence ganha RP e quem perde cai. Contra alguém mais forte, a vitória rende mais."
                    else "Só pela moral: não mexe no RP de ninguém.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = { Button({ onConfirm(ranked) }) { Text("Desafiar") } },
        dismissButton = { TextButton(onDismiss) { Text("Cancelar") } }
    )
}

@Composable
private fun DuelsCard(state: RankedUiState, onRespond: (String, Boolean) -> Unit) {
    val me = state.me
    ThemedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Seus x1", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            state.duels.forEach { d -> DuelRow(d, me, onRespond) }
        }
    }
}

@Composable
private fun DuelRow(d: RankedDuel, me: String?, onRespond: (String, Boolean) -> Unit) {
    val cs = MaterialTheme.colorScheme
    val iAmChallenger = d.challengerId == me
    val myName = "Você"
    val oppName = "@" + if (iAmChallenger) d.opponentName else d.challengerName
    val myPts = if (iAmChallenger) d.ptChallenger else d.ptOpponent
    val oppPts = if (iAmChallenger) d.ptOpponent else d.ptChallenger
    val myDelta = if (iAmChallenger) d.deltaChallenger else d.deltaOpponent
    val kind = if (d.ranked) "valendo RP" else "amistoso"
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("$myName x $oppName", Modifier.weight(1f), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(kind, style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant)
        }
        when {
            d.isPending && !iAmChallenger -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("${d.challengerName} te desafiou.", Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                Button({ onRespond(d.id, true) }) { Text("Aceitar") }
                OutlinedButton({ onRespond(d.id, false) }) { Text("Recusar") }
            }
            d.isPending -> Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Esperando a resposta.", Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
                TextButton({ onRespond(d.id, false) }) { Text("Cancelar", color = cs.error) }
            }
            d.isActive -> {
                val total = ((myPts ?: 0) + (oppPts ?: 0)).coerceAtLeast(1)
                LinearProgressIndicator(progress = { (myPts ?: 0) / total.toFloat() }, Modifier.fillMaxWidth().height(10.dp).clip(CircleShape))
                Text("$myName ${myPts ?: 0} x ${oppPts ?: 0} $oppName" + (daysLeft(d.endsAt)?.let { " · $it" } ?: ""), style = MaterialTheme.typography.bodySmall)
            }
            d.isFinished -> {
                val won = d.winnerId == me
                val draw = d.winnerId == null
                val color = if (draw) cs.onSurfaceVariant else if (won) Color(0xFF3DDC84) else cs.error
                Text(
                    (if (draw) "Empate" else if (won) "Você venceu" else "Você perdeu") + " · ${myPts ?: 0} x ${oppPts ?: 0}" +
                        (if (d.ranked && myDelta != null && myDelta != 0) " · ${if (myDelta > 0) "+" else ""}$myDelta RP" else ""),
                    style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = color
                )
            }
            else -> Text(if (d.status == "declined") "Recusado." else "Cancelado.", style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
        }
    }
}

/** "termina hoje", "falta 1 dia", "faltam 3 dias" a partir de `ends_at`. */
private fun daysLeft(endsAt: String?): String? {
    val end = endsAt?.let { runCatching { Instant.parse(it) }.getOrNull() } ?: return null
    val hours = (end - Clock.System.now()).inWholeHours
    return when {
        hours <= 0 -> "termina hoje"
        hours < 24 -> "falta menos de 1 dia"
        else -> (hours / 24).let { if (it == 1L) "falta 1 dia" else "faltam $it dias" }
    }
}

// ───────────────────────── Como funciona ─────────────────────────

@Composable
private fun HowCard() {
    ThemedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Como funciona", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            listOf(
                "Elos: Ferro, Bronze, Prata, Ouro, Platina e Diamante têm 3 divisões (III, II, I) de 100 RP cada. Acima vem a Lenda.",
                "Toda semana (segunda a domingo) você disputa com uma turma de até 30 pessoas do seu elo.",
                "No domingo, os 20% do topo ganham RP e sobem; os 20% do fundo perdem. Quem não treinou perde mais.",
                "O x1 dura 7 dias entre duas pessoas do mesmo elo. Contra alguém mais forte, a vitória rende mais RP.",
                "Os pontos vêm só de treinos reais e terminados. Só o melhor treino de cada dia conta, para ninguém ganhar na marra.",
                "A cada 8 semanas começa uma temporada nova e o RP de todo mundo encolhe 40%."
            ).forEach { Text("• $it", style = MaterialTheme.typography.bodySmall) }
        }
    }
}
