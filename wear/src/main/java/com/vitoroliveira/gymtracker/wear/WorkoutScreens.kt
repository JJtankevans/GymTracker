package com.vitoroliveira.gymtracker.wear

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.CompactChip
import androidx.wear.compose.material.HorizontalPageIndicator
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.ListHeader
import androidx.wear.compose.material.PageIndicatorState
import androidx.wear.compose.material.Text
import com.vitoroliveira.gymtracker.watch.WatchExercise
import com.vitoroliveira.gymtracker.watch.WatchSession
import com.vitoroliveira.gymtracker.watch.formatKg
import com.vitoroliveira.gymtracker.watch.formatVolume
import com.vitoroliveira.gymtracker.watch.progressionHint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

private const val PAGE_EXERCISES = 0
private const val PAGE_SET = 1
private const val PAGE_STATS = 2

/** Treino em andamento: ← lista de exercícios · série/descanso (meio) · tempo e números →. */
@Composable
fun WorkoutScreen(c: WatchController, accent: Color, onPlates: () -> Unit, onAddExercise: () -> Unit, onFinish: () -> Unit, onDiscard: () -> Unit, onGone: () -> Unit) {
    val session = c.session
    if (session == null) {
        LaunchedEffect(Unit) { onGone() }
        return
    }
    AskPermissionsOnce(c)

    val scope = rememberCoroutineScope()
    val pager = rememberPagerState(initialPage = PAGE_SET) { 3 }
    Box(Modifier.fillMaxSize()) {
        HorizontalPager(pager, Modifier.fillMaxSize()) { page ->
            // a coroa vai para a página visível (lista de exercícios rola; na série, muda reps/kg)
            @Suppress("DEPRECATION")
            androidx.wear.compose.foundation.HierarchicalFocusCoordinator(requiresFocus = { pager.currentPage == page }) {
            when (page) {
                PAGE_EXERCISES -> ExercisesPage(c, session, accent, onFinish, onDiscard, onAddExercise) { i ->
                    c.select(i)
                    scope.launch { pager.animateScrollToPage(PAGE_SET) }
                }
                PAGE_SET -> if (c.rest != null) RestPage(c, session, accent) else SetPage(c, session, accent, active = pager.currentPage == PAGE_SET, onPlates = onPlates)
                else -> StatsPage(c, session, accent, onFinish)
            }
            }
        }
        val indicator = remember(pager) {
            object : PageIndicatorState {
                override val pageOffset: Float get() = pager.currentPageOffsetFraction
                override val selectedPage: Int get() = pager.currentPage
                override val pageCount: Int get() = 3
            }
        }
        HorizontalPageIndicator(indicator, Modifier.align(Alignment.BottomCenter).padding(bottom = 2.dp), selectedColor = accent)
    }
}

/** Pede batimentos e notificações na primeira vez que um treino abre (uma vez só). */
@Composable
private fun AskPermissionsOnce(c: WatchController) {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { c.onPermissionsAnswered() }
    LaunchedEffect(Unit) {
        if (c.shouldAskPermissions) {
            val perms = buildList {
                add(HEART_RATE_PERMISSION)
                if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
            }
            launcher.launch(perms.toTypedArray())
        }
    }
}

// ───────────────────────── série ─────────────────────────

@Composable
private fun SetPage(c: WatchController, s: WatchSession, accent: Color, active: Boolean, onPlates: () -> Unit) {
    val ex = s.current
    var field by remember { mutableIntStateOf(1) }          // 0 = repetições, 1 = carga
    var acc by remember { mutableFloatStateOf(0f) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(active) { if (active) runCatching { focus.requestFocus() } }

    Column(
        Modifier.fillMaxSize()
            .onRotaryScrollEvent { e ->
                // coroa/bezel: um "clique" por ~30 px (o bezel do Galaxy manda um evento grande por clique)
                acc += e.verticalScrollPixels
                if (abs(acc) >= 30f) {
                    val dir = if (acc > 0) 1 else -1
                    if (field == 0) c.changeReps(dir) else c.changeKg(dir)
                    acc = 0f
                }
                true
            }
            .focusRequester(focus).focusable()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            if (s.isQuick) "TREINO RÁPIDO" else ex.name.uppercase(),
            fontSize = 12.sp, fontWeight = FontWeight.Bold, color = accent, letterSpacing = 1.sp,
            maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 18.dp)
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            val done = s.setsDone(ex.id)
            Text(if (s.isQuick) "série ${done + 1}" else "série ${done + 1} de ${ex.sets}", fontSize = 11.sp, color = Muted)
            c.heartRate?.let { HeartChip(it) }
        }
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            BigValue("${c.draftReps}", "reps", field == 0, accent) { field = 0 }
            Text("×", fontSize = 22.sp, color = Muted, modifier = Modifier.padding(horizontal = 6.dp))
            BigValue(formatKg(c.draftKg), "kg", field == 1, accent) { field = 1 }
        }
        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
            RoundButton("−") { if (field == 0) c.changeReps(-1) else c.changeKg(-1) }
            // com a carga selecionada, o meio vira atalho para as anilhas
            Text(
                if (field == 0) "reps" else "anilhas",
                fontSize = 10.sp, color = if (field == 0) Muted else accent, textAlign = TextAlign.Center,
                fontWeight = if (field == 0) FontWeight.Normal else FontWeight.Bold,
                modifier = Modifier.width(52.dp).clip(RoundedCornerShape(10.dp))
                    .then(if (field == 1 && c.draftKg >= 20.0) Modifier.clickable(onClick = onPlates) else Modifier)
                    .padding(vertical = 4.dp)
            )
            RoundButton("+") { if (field == 0) c.changeReps(1) else c.changeKg(1) }
        }
        LastTimeHint(s, ex)
        Spacer(Modifier.height(6.dp))
        Button(
            onClick = { c.registerSet() },
            modifier = Modifier.fillMaxWidth(0.78f).height(40.dp),
            colors = ButtonDefaults.buttonColors(backgroundColor = accent, contentColor = Color.Black),
            shape = RoundedCornerShape(20.dp)
        ) {
            Text("REGISTRAR", fontWeight = FontWeight.Black, fontSize = 14.sp, letterSpacing = 1.sp)
        }
    }
}

@Composable
private fun BigValue(value: String, label: String, selected: Boolean, accent: Color, onClick: () -> Unit) {
    Column(
        Modifier.clip(RoundedCornerShape(14.dp))
            .background(if (selected) accent.copy(alpha = 0.16f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, fontSize = 34.sp, fontWeight = FontWeight.Black, color = if (selected) accent else Color.White, maxLines = 1)
        Box(Modifier.width(26.dp).height(2.dp).background(if (selected) accent else Color.Transparent))
    }
}

@Composable
private fun RoundButton(symbol: String, onClick: () -> Unit) {
    Button(onClick = onClick, modifier = Modifier.size(34.dp), colors = ButtonDefaults.secondaryButtonColors()) {
        Text(symbol, fontSize = 20.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun HeartChip(bpm: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Filled.Favorite, contentDescription = "Batimentos", tint = Danger, modifier = Modifier.size(11.dp))
        Text(" $bpm", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
    }
}

/** "Última: 40 × 10 · tente 42,5" ou o recorde. */
@Composable
private fun LastTimeHint(s: WatchSession, ex: WatchExercise) {
    if (s.isQuick) return
    val text = when {
        s.setsDone(ex.id) > 0 -> s.bestKg(ex)?.let { "recorde ${formatKg(it)} kg" }
        ex.lastKg != null -> {
            val tip = progressionHint(ex)?.let { " · tente ${formatKg(it)}" } ?: ""
            "última ${formatKg(ex.lastKg!!)} × ${ex.lastReps ?: ex.reps}$tip"
        }
        else -> "primeira vez · ${ex.sets}×${ex.reps}"
    } ?: return
    Text(text, fontSize = 10.sp, color = Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
}

// ───────────────────────── descanso ─────────────────────────

@Composable
private fun RestPage(c: WatchController, s: WatchSession, accent: Color) {
    val rest = c.rest ?: return
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(rest, c.ambient) {
        while (!c.ambient) {
            now = System.currentTimeMillis()
            c.onRestTick(now)
            if (c.rest == null) break
            delay(200)
        }
    }
    val leftMs = (rest.endsAt - now).coerceAtLeast(0)
    val target = if (rest.totalSec <= 0) 0f else (leftMs / (rest.totalSec * 1000f)).coerceIn(0f, 1f)
    val progress by animateFloatAsState(target, label = "descanso")
    val ending = leftMs <= 10_000
    val ring = if (ending) Gold else accent

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(
            progress = progress, modifier = Modifier.fillMaxSize().padding(3.dp),
            startAngle = 270f, endAngle = 270f + 359.9f, strokeWidth = 7.dp,
            indicatorColor = ring, trackColor = ring.copy(alpha = 0.15f)
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (c.recordFlash) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Star, contentDescription = null, tint = Gold, modifier = Modifier.size(14.dp))
                    Text(" RECORDE!", color = Gold, fontWeight = FontWeight.Black, fontSize = 13.sp, letterSpacing = 1.sp)
                }
            } else {
                Text("DESCANSO", fontSize = 11.sp, letterSpacing = 2.sp, color = Muted)
            }
            Text(duration(leftMs + 999), fontSize = 46.sp, fontWeight = FontWeight.Black, color = if (ending) Gold else Color.White)
            c.heartRate?.let { HeartChip(it) }
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                CompactChip(onClick = { c.addRest(-15) }, label = { Text("−15") }, colors = ChipDefaults.secondaryChipColors())
                CompactChip(onClick = { c.addRest(15) }, label = { Text("+15") }, colors = ChipDefaults.secondaryChipColors())
            }
            Spacer(Modifier.height(4.dp))
            NextUp(s)
            Spacer(Modifier.height(2.dp))
            CompactChip(
                onClick = { c.skipRest() },
                label = { Text("PULAR", fontWeight = FontWeight.Bold) },
                colors = ChipDefaults.chipColors(backgroundColor = ring, contentColor = Color.Black)
            )
        }
    }
}

@Composable
private fun NextUp(s: WatchSession) {
    if (s.allDone) {
        Text("Tudo feito! Termine →", fontSize = 11.sp, color = Gold)
        return
    }
    val ex = s.current
    val (reps, kg) = s.suggestion(ex)
    val label = if (s.isQuick) "próxima: ${formatKg(kg)} kg × $reps"
    else "${ex.name} · ${s.setsDone(ex.id) + 1}/${ex.sets} · ${formatKg(kg)}×$reps"
    Text(
        label, fontSize = 10.sp, color = Muted, maxLines = 1, overflow = TextOverflow.Ellipsis,
        textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 26.dp)
    )
}

// ───────────────────────── exercícios ─────────────────────────

@Composable
private fun ExercisesPage(
    c: WatchController, s: WatchSession, accent: Color,
    onFinish: () -> Unit, onDiscard: () -> Unit, onAddExercise: () -> Unit, onPick: (Int) -> Unit
) {
    WatchList {
        item {
            ListHeader {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(s.title, color = accent, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (!s.isQuick) Text("${s.doneExercises} de ${s.exercises.size} exercícios", color = Muted, fontSize = 11.sp)
                }
            }
        }
        if (!s.isQuick) {
            items(s.exercises.size) { i ->
                val ex = s.exercises[i]
                val done = s.isDone(ex)
                val sets = s.setsDone(ex.id)
                val selected = i == s.currentIndex
                Chip(
                    onClick = { onPick(i) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(ex.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    secondaryLabel = {
                        val kg = s.sets.lastOrNull { it.exerciseId == ex.id }?.weightKg ?: ex.lastKg
                        Text("$sets/${ex.sets} séries" + (kg?.let { " · ${formatKg(it)} kg" } ?: ""), maxLines = 1)
                    },
                    icon = {
                        if (done) Icon(Icons.Filled.Check, contentDescription = "Feito", tint = if (selected) Color.Black else accent)
                        else Text("${i + 1}", fontWeight = FontWeight.Black, fontSize = 15.sp)
                    },
                    colors = if (selected) ChipDefaults.primaryChipColors() else ChipDefaults.secondaryChipColors()
                )
            }
        }
        if (!s.isQuick && c.extraExercises.isNotEmpty()) {
            item {
                Chip(
                    onClick = onAddExercise,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Adicionar exercício") },
                    secondaryLabel = { Text("Fora deste grupo", color = Muted) },
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    colors = ChipDefaults.secondaryChipColors()
                )
            }
        }
        if (s.sets.isNotEmpty()) {
            item {
                Chip(
                    onClick = { c.undoLastSet() },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Desfazer última série") },
                    secondaryLabel = {
                        val last = s.sets.last()
                        Text("${formatKg(last.weightKg)} kg × ${last.reps}", color = Muted)
                    },
                    icon = { Icon(Icons.Filled.Refresh, contentDescription = null) },
                    colors = ChipDefaults.secondaryChipColors()
                )
            }
        }
        item {
            Chip(
                onClick = onFinish,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Terminar treino", fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Filled.Check, contentDescription = null) },
                colors = ChipDefaults.primaryChipColors()
            )
        }
        item {
            Chip(
                onClick = onDiscard,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Descartar", color = Danger) },
                icon = { Icon(Icons.Filled.Delete, contentDescription = null, tint = Danger) },
                colors = ChipDefaults.secondaryChipColors()
            )
        }
    }
}

// ───────────────────────── números do treino ─────────────────────────

@Composable
private fun StatsPage(c: WatchController, s: WatchSession, accent: Color, onFinish: () -> Unit) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(c.ambient) { while (!c.ambient) { now = System.currentTimeMillis(); delay(1000) } }
    Column(
        Modifier.fillMaxSize().padding(horizontal = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
    ) {
        Text("TEMPO", fontSize = 10.sp, letterSpacing = 2.sp, color = Muted)
        Text(duration(now - s.startedAt), fontSize = 34.sp, fontWeight = FontWeight.Black, color = accent)
        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Stat("${s.totalSets}", "séries")
            Stat(formatVolume(s.volumeKg), "volume")
        }
        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Stat(c.heartRate?.toString() ?: "—", "bpm agora", Danger)
            Stat(s.maxHeartRate?.toString() ?: "—", "bpm máx")
            if (s.records > 0) Stat("${s.records}", "recordes", Gold)
        }
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = onFinish,
            modifier = Modifier.fillMaxWidth(0.7f).height(36.dp),
            colors = ButtonDefaults.buttonColors(backgroundColor = accent, contentColor = Color.Black),
            shape = RoundedCornerShape(18.dp)
        ) { Text("TERMINAR", fontWeight = FontWeight.Black, fontSize = 13.sp) }
    }
}

@Composable
private fun Stat(value: String, label: String, color: Color = Color.White) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = color)
        Text(label, fontSize = 9.sp, color = Muted)
    }
}

// ───────────────────────── resumo final ─────────────────────────

@Composable
fun SummaryScreen(c: WatchController, accent: Color, onDone: () -> Unit) {
    val f = c.lastFinish
    if (f == null) {
        LaunchedEffect(Unit) { onDone() }
        return
    }
    val queued = f.workoutId != null && c.outbox.items.any { it.workout.id == f.workoutId }
    WatchList {
        item {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text("TREINO CONCLUÍDO", fontSize = 11.sp, letterSpacing = 2.sp, color = Muted)
                Text(f.title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = accent, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(duration(f.durationMs), fontSize = 34.sp, fontWeight = FontWeight.Black)
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                Stat("${f.sets}", "séries")
                Stat(formatVolume(f.volumeKg), "volume")
                if (f.exercisesTotal > 0) Stat("${f.exercisesDone}/${f.exercisesTotal}", "exercícios")
            }
        }
        if (f.avgHeartRate != null || f.records > 0) {
            item {
                Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                    f.avgHeartRate?.let { Stat("$it", "bpm médio", Danger) }
                    f.maxHeartRate?.let { Stat("$it", "bpm máx") }
                    if (f.records > 0) Stat("${f.records}", if (f.records == 1) "recorde" else "recordes", Gold)
                }
            }
        }
        if (f.bestLiftName != null && f.bestLiftKg != null) {
            item {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text("MELHOR SÉRIE", fontSize = 9.sp, letterSpacing = 1.sp, color = Muted)
                    Text(
                        "${f.bestLiftName} · 1RM ~${formatKg(f.bestLiftKg)} kg",
                        fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 2, textAlign = TextAlign.Center
                    )
                }
            }
        }
        item {
            val (icon, tint, text) = when {
                f.workoutId == null -> Triple(Icons.Filled.Close, Muted, "Treino rápido: fica só no relógio")
                queued -> Triple(Icons.Filled.Warning, Gold, "Na fila: vai para o celular quando ele estiver perto")
                else -> Triple(Icons.Filled.Check, accent, "Salvo no celular")
            }
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Surface).padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(text, fontSize = 11.sp, color = Color.White)
            }
        }
        item {
            Chip(
                onClick = onDone,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Concluir", fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Filled.Check, contentDescription = null) },
                colors = ChipDefaults.primaryChipColors()
            )
        }
    }
}

// ───────────────────────── adicionar exercício ─────────────────────────

/** Lista dos exercícios da conta que não estão no treino; tocar põe no treino e volta para a série. */
@Composable
fun AddExerciseScreen(c: WatchController, accent: Color, onDone: () -> Unit) {
    val list = c.extraExercises
    WatchList {
        item { ListHeader { Text("Adicionar", color = accent, fontWeight = FontWeight.Bold) } }
        if (list.isEmpty()) {
            item { Text("Todos os exercícios já estão no treino.", color = Muted, fontSize = 12.sp, textAlign = TextAlign.Center) }
        }
        items(list.size) { i ->
            val ex = list[i]
            Chip(
                onClick = { c.addExercise(ex); onDone() },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(ex.name, maxLines = 2, overflow = TextOverflow.Ellipsis) },
                secondaryLabel = { Text("${ex.sets}×${ex.reps}" + (ex.lastKg?.let { " · ${formatKg(it)} kg" } ?: ""), color = Muted) },
                colors = ChipDefaults.secondaryChipColors()
            )
        }
    }
}
