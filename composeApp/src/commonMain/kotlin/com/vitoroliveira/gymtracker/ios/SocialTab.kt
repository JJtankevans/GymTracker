package com.vitoroliveira.gymtracker.ios

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vitoroliveira.gymtracker.data.model.LeagueRow
import com.vitoroliveira.gymtracker.presentation.social.FeedItem
import com.vitoroliveira.gymtracker.presentation.social.ReactionKind
import com.vitoroliveira.gymtracker.presentation.social.SocialViewModel
import com.vitoroliveira.gymtracker.presentation.social.feedGroupName
import com.vitoroliveira.gymtracker.presentation.social.formatFeedStats
import com.vitoroliveira.gymtracker.presentation.social.formatInt

/** Social no iPhone: liga da semana e feed com reações (o mesmo SocialViewModel do Android). */
@Composable
fun SocialTab(vm: SocialViewModel) {
    val s by vm.state.collectAsState()
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Text("Social", fontSize = 30.sp, fontWeight = FontWeight.Black) }
        if (s.isLoading && s.feed.isEmpty()) item { CircularProgressIndicator() }
        s.error?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
        if (s.league.size > 1) item { LeaguePanel(s.league) }
        if (!s.isLoading && s.feed.isEmpty()) item {
            Text("Feed vazio. Treinos compartilhados seus e de quem você segue aparecem aqui.", color = Muted)
        }
        items(s.feed, key = { it.post.id }) { FeedPanel(it) { kind -> vm.toggleReaction(it.post.id, kind) } }
    }
}

@Composable
private fun LeaguePanel(rows: List<LeagueRow>) {
    Panel {
        Text("Liga da semana", fontWeight = FontWeight.Bold, fontSize = 17.sp)
        Text("Você e quem você segue, de segunda a domingo", color = Muted, fontSize = 12.sp)
        Spacer(Modifier.height(8.dp))
        rows.take(8).forEachIndexed { i, r ->
            Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("${i + 1}", fontWeight = FontWeight.Black, color = if (i == 0) Gold else Muted, modifier = Modifier.width(24.dp))
                Column(Modifier.weight(1f)) {
                    Text(if (r.isMe) "Você" else "@${r.username}", fontWeight = if (r.isMe) FontWeight.Black else FontWeight.Medium, maxLines = 1)
                    Text("${r.activeDays} dias · ${formatInt(r.volumeKg)} kg · ${r.minutes} min", color = Muted, fontSize = 11.sp)
                }
                Text("${r.workouts}", fontWeight = FontWeight.Black, fontSize = 18.sp, color = if (r.isMe) Lime else Color.White)
            }
        }
    }
}

@Composable
private fun FeedPanel(item: FeedItem, onReact: (ReactionKind) -> Unit) {
    val group = feedGroupName(item.post.stats)
    Panel {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(34.dp).clip(CircleShape).background(Lime.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
                Text(item.author.take(1).uppercase(), fontWeight = FontWeight.Black, color = Lime)
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(if (item.isMine) "Você" else "@${item.author}", fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(group?.let { "Treino de $it" } ?: "Treino", color = Muted, fontSize = 12.sp)
            }
        }
        val stats = formatFeedStats(item.post.stats)
        if (stats.isNotBlank()) { Spacer(Modifier.height(8.dp)); Text(stats, fontWeight = FontWeight.Bold) }
        item.post.caption?.let { Spacer(Modifier.height(4.dp)); Text(it) }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ReactionKind.entries.filter { it != ReactionKind.HEADPHONES }.forEach { k ->
                val mine = k in item.reactions.mine
                val n = item.reactions.count(k)
                Box(
                    Modifier.clip(RoundedCornerShape(14.dp))
                        .background(if (mine) Lime.copy(alpha = 0.2f) else Color.Transparent)
                        .border(1.dp, if (mine) Lime else Muted.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                        .clickable { onReact(k) }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(if (n > 0) "${k.label} $n" else k.label, fontSize = 12.sp, color = if (mine) Lime else Color.White)
                }
            }
        }
    }
}
