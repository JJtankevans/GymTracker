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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vitoroliveira.gymtracker.data.model.MuscleGroup
import com.vitoroliveira.gymtracker.presentation.viewmodel.HistoryViewModel
import com.vitoroliveira.gymtracker.presentation.viewmodel.HomeViewModel
import com.vitoroliveira.gymtracker.presentation.viewmodel.MuscleGroupViewModel
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

private enum class Tab(val label: String) { HOME("Início"), HISTORY("Histórico"), RECORDS("Recordes"), PROFILE("Perfil") }

/** O app depois do login: abas Início / Histórico / Perfil; o treino abre por cima. */
@Composable
fun MainScreen(onSignedOut: () -> Unit) {
    var tab by remember { mutableStateOf(Tab.HOME) }
    // treino aberto: (id do grupo ou null, nome)
    var workout by remember { mutableStateOf<Pair<String?, String>?>(null) }
    // cada aba guarda o próprio ViewModel enquanto o app está aberto
    val home = remember { HomeViewModel(Services.workouts, Services.profiles, Services.groups) }
    val groups = remember { MuscleGroupViewModel(Services.groups) }
    val history = remember { HistoryViewModel(Services.workouts, Services.groups) }
    val records = remember { com.vitoroliveira.gymtracker.presentation.records.RecordsViewModel(Services.exercises, Services.workouts) }

    workout?.let { (groupId, name) ->
        WorkoutScreen(groupId, name, onClose = {
            workout = null
            home.loadDashboard(); history.loadHistory(); records.load()
        })
        return
    }

    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = Card) {
                Tab.entries.forEach { t ->
                    NavigationBarItem(
                        selected = tab == t,
                        onClick = { tab = t },
                        icon = { Dot(tab == t) },
                        label = { Text(t.label) }
                    )
                }
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (tab) {
                Tab.HOME -> HomeTab(home, groups) { g -> workout = g.id to g.name }
                Tab.HISTORY -> HistoryTab(history)
                Tab.RECORDS -> RecordsTab(records)
                Tab.PROFILE -> ProfileTab(onSignedOut)
            }
        }
    }
}

@Composable
private fun Dot(selected: Boolean) {
    Box(
        Modifier.size(if (selected) 12.dp else 8.dp).clip(CircleShape)
            .background(if (selected) Lime else Muted.copy(alpha = 0.5f))
    )
}

// ───────────────────────── Início ─────────────────────────

@Composable
private fun HomeTab(home: HomeViewModel, groupsVm: MuscleGroupViewModel, onStart: (MuscleGroup) -> Unit) {
    val s by home.uiState.collectAsState()
    val g by groupsVm.uiState.collectAsState()
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("GYMTRACKER", color = Lime, fontWeight = FontWeight.Black, letterSpacing = 3.sp, fontSize = 14.sp)
            Spacer(Modifier.height(6.dp))
            Text("Bora treinar", fontSize = 30.sp, fontWeight = FontWeight.Black)
        }
        item {
            Panel {
                Text("Esta semana", color = Muted, fontSize = 13.sp)
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("${s.weeklyCount}", fontSize = 44.sp, fontWeight = FontWeight.Black, color = Lime)
                    Text(" de ${s.weeklyGoal} treinos", fontSize = 16.sp, modifier = Modifier.padding(bottom = 8.dp))
                }
                LinearProgressIndicator(
                    progress = { (s.weeklyCount.toFloat() / s.weeklyGoal.coerceAtLeast(1)).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                    color = Lime, trackColor = Lime.copy(alpha = 0.15f)
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    Stat("${s.streakDays}", "dias seguidos")
                    Stat("${s.totalWorkouts}", "treinos no total")
                    s.lastWorkoutDate?.let { Stat(it, "último") }
                }
            }
        }
        item { Text("Seus treinos", fontWeight = FontWeight.Bold, fontSize = 18.sp) }
        if (g.isLoading && g.muscleGroups.isEmpty()) item { CircularProgressIndicator() }
        g.errorMessage?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
        items(g.muscleGroups, key = { it.id ?: it.name }) { group ->
            val suggested = group.id != null && group.id == s.suggestedGroupId
            Panel(Modifier.clickable { onStart(group) }, highlight = suggested) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(14.dp).clip(CircleShape).background(hexColor(group.colorHex)))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(group.name, fontWeight = FontWeight.Bold, fontSize = 17.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (suggested) Text("Sugerido hoje", color = Lime, fontSize = 12.sp)
                    }
                    Text("INICIAR", color = Lime, fontWeight = FontWeight.Black, fontSize = 13.sp)
                }
            }
        }
        item {
            OutlinedButton(onClick = { onStart(MuscleGroup(id = null, name = "Treino livre")) }, modifier = Modifier.fillMaxWidth()) {
                Text("Treino livre (todos os exercícios)")
            }
        }
    }
}

@Composable
fun Panel(modifier: Modifier = Modifier, highlight: Boolean = false, content: @Composable () -> Unit) {
    Column(
        modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Card)
            .then(if (highlight) Modifier.border(1.5.dp, Lime, RoundedCornerShape(20.dp)) else Modifier)
            .padding(16.dp)
    ) { content() }
}

@Composable
fun Stat(value: String, label: String) {
    Column {
        Text(value, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text(label, color = Muted, fontSize = 11.sp)
    }
}

fun hexColor(hex: String): Color = runCatching {
    val v = hex.removePrefix("#").toLong(16)
    Color(if (hex.removePrefix("#").length <= 6) v or 0xFF000000 else v)
}.getOrDefault(Lime)

// ───────────────────────── Histórico ─────────────────────────

@Composable
private fun HistoryTab(vm: HistoryViewModel) {
    val s by vm.uiState.collectAsState()
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item { Text("Histórico", fontSize = 30.sp, fontWeight = FontWeight.Black) }
        if (s.isLoading && s.workouts.isEmpty()) item { CircularProgressIndicator() }
        s.errorMessage?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
        if (!s.isLoading && s.workouts.isEmpty()) item { Text("Nenhum treino ainda.", color = Muted) }
        items(s.workouts, key = { it.workoutId }) { w ->
            // "2026-10-28T22:30:00" → "28/10 · 22:30"
            val iso = w.startedAt.toLocalDateTime(TimeZone.currentSystemDefault()).toString()
            val date = "${iso.substring(8, 10)}/${iso.substring(5, 7)} · ${iso.substring(11, 16)}"
            Panel {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(12.dp).clip(CircleShape).background(hexColor(w.muscleGroupColorHex)))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(w.muscleGroupName, fontWeight = FontWeight.Bold)
                        Text(date, color = Muted, fontSize = 12.sp)
                    }
                    w.durationMinutes?.let { Text("$it min", color = Lime, fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}

// ───────────────────────── Perfil ─────────────────────────

@Composable
private fun ProfileTab(onSignedOut: () -> Unit) {
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) { name = runCatching { Services.profiles.getCurrentProfile().username }.getOrNull() }
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Perfil", fontSize = 30.sp, fontWeight = FontWeight.Black)
        Panel {
            Text(name?.let { "@$it" } ?: "...", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text("Versão de iPhone em construção: treino, histórico e semana. Temas, social e relógio vêm depois.", color = Muted, fontSize = 13.sp)
        }
        Button(onClick = {
            scope.launch {
                runCatching { Services.auth.signOut() }
                onSignedOut()
            }
        }, modifier = Modifier.fillMaxWidth()) { Text("Sair da conta") }
    }
}

// ───────────────────────── Recordes ─────────────────────────

@Composable
private fun RecordsTab(vm: com.vitoroliveira.gymtracker.presentation.records.RecordsViewModel) {
    val s by vm.state.collectAsState()
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text("Recordes", fontSize = 30.sp, fontWeight = FontWeight.Black)
            if (s.recentCount > 0) Text("${s.recentCount} batidos nos últimos 30 dias", color = Lime, fontSize = 13.sp)
        }
        if (s.isLoading && s.records.isEmpty()) item { CircularProgressIndicator() }
        s.errorMessage?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
        if (!s.isLoading && s.records.isEmpty()) item { Text("Registre séries com carga para ver seus recordes.", color = Muted) }
        items(s.records, key = { it.exerciseId }) { r ->
            val date = r.bestDate.let { if (it.length >= 10) "${it.substring(8, 10)}/${it.substring(5, 7)}" else it }
            Panel {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(r.exerciseName, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("em $date · 1RM ~${com.vitoroliveira.gymtracker.watch.formatKg(r.bestOneRmKg)} kg · ${r.sessions} sessões", color = Muted, fontSize = 12.sp)
                    }
                    Text("${com.vitoroliveira.gymtracker.watch.formatKg(r.bestWeightKg)} kg", color = Lime, fontWeight = FontWeight.Black, fontSize = 18.sp)
                }
            }
        }
    }
}
