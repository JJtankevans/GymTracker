package com.vitoroliveira.gymtracker.wear

import android.content.Context
import android.os.Build
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.CurvedLayout
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.Colors
import androidx.wear.compose.material.HorizontalPageIndicator
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.PageIndicatorState
import androidx.wear.compose.material.Text
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val Accents = listOf(
    "Limão" to Color(0xFFC6FF00),
    "Neon" to Color(0xFFFF2BD6),
    "Ciano" to Color(0xFF22E3FF),
    "Magma" to Color(0xFFFF7A1A)
)

private fun Context.vibrator(): Vibrator =
    if (Build.VERSION.SDK_INT >= 31) (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
    else @Suppress("DEPRECATION") getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

private fun Context.buzz(long: Boolean) {
    val v = vibrator()
    val pattern = if (long) longArrayOf(0, 250, 120, 250, 120, 500) else longArrayOf(0, 60)
    v.vibrate(VibrationEffect.createWaveform(pattern, -1))
}

@Composable
fun WatchApp(store: WatchStore) {
    var accentIdx by remember { mutableIntStateOf(store.accent.coerceIn(0, Accents.lastIndex)) }
    val accent = Accents[accentIdx].second

    MaterialTheme(colors = Colors(primary = accent, secondary = accent, onPrimary = Color.Black, background = Color.Black, surface = Color(0xFF1B1B1B))) {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            WatchPages(store, accent, accentIdx) { accentIdx = it; store.accent = it }
        }
    }
}

@Composable
private fun WatchPages(store: WatchStore, accent: Color, accentIdx: Int, onAccent: (Int) -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val pager = rememberPagerState(initialPage = 0) { 3 }

    // ── estado do descanso (por prazo, não por contagem: não atrasa) ──
    var restTotal by remember { mutableIntStateOf(store.restSeconds) }
    var deadline by remember { mutableLongStateOf(0L) }          // elapsedRealtime
    var pausedLeftMs by remember { mutableLongStateOf(restTotal * 1000L) }
    var running by remember { mutableStateOf(false) }
    var leftMs by remember { mutableLongStateOf(restTotal * 1000L) }

    fun start(seconds: Int = restTotal) {
        restTotal = seconds; store.restSeconds = seconds
        pausedLeftMs = seconds * 1000L
        deadline = SystemClock.elapsedRealtime() + pausedLeftMs
        running = true
    }
    fun pause() { pausedLeftMs = (deadline - SystemClock.elapsedRealtime()).coerceAtLeast(0); running = false; leftMs = pausedLeftMs }
    fun resume() { deadline = SystemClock.elapsedRealtime() + pausedLeftMs; running = true }
    fun reset() { running = false; pausedLeftMs = restTotal * 1000L; leftMs = pausedLeftMs }

    LaunchedEffect(running, deadline) {
        while (running) {
            leftMs = (deadline - SystemClock.elapsedRealtime()).coerceAtLeast(0)
            if (leftMs == 0L) { running = false; pausedLeftMs = restTotal * 1000L; ctx.buzz(long = true); break }
            delay(100)
        }
    }

    // ── série em edição ──
    var reps by remember { mutableIntStateOf(store.reps) }
    var kg by remember { mutableFloatStateOf(store.kg) }
    var sets by remember { mutableIntStateOf(store.sets) }
    var volume by remember { mutableFloatStateOf(store.volume) }
    var startedAt by remember { mutableLongStateOf(store.startedAt) }

    Box(Modifier.fillMaxSize()) {
        HorizontalPager(pager, Modifier.fillMaxSize()) { page ->
            when (page) {
                0 -> RestPage(leftMs, restTotal, running, accent,
                    onPick = { start(it) },
                    onToggle = { if (running) pause() else if (pausedLeftMs in 1 until restTotal * 1000L) resume() else start() },
                    onReset = ::reset)
                1 -> SetPage(sets + 1, reps, kg, accent, { reps = it; store.reps = it }, { kg = it; store.kg = it }) {
                    if (startedAt == 0L) { startedAt = System.currentTimeMillis(); store.startedAt = startedAt }
                    sets += 1; volume += reps * kg; store.sets = sets; store.volume = volume
                    ctx.buzz(long = false)
                    start()
                    scope.launch { pager.animateScrollToPage(0) }
                }
                else -> SummaryPage(sets, volume, startedAt, accent, accentIdx, onAccent) {
                    store.resetSession(); sets = 0; volume = 0f; startedAt = 0L
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
        HorizontalPageIndicator(indicator, Modifier.align(Alignment.BottomCenter), selectedColor = accent)
    }
}

private fun fmt(ms: Long): String {
    val s = ((ms + 999) / 1000).toInt()
    return "%d:%02d".format(s / 60, s % 60)
}

// ───────────────────────── descanso ─────────────────────────

@Composable
private fun RestPage(leftMs: Long, totalSec: Int, running: Boolean, accent: Color, onPick: (Int) -> Unit, onToggle: () -> Unit, onReset: () -> Unit) {
    val progress = if (totalSec <= 0) 0f else (leftMs / (totalSec * 1000f)).coerceIn(0f, 1f)
    Box(Modifier.fillMaxSize().padding(6.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(
            progress = progress, modifier = Modifier.fillMaxSize(),
            startAngle = 270f, endAngle = 270f + 360f - 0.01f, strokeWidth = 8.dp,
            indicatorColor = accent, trackColor = accent.copy(alpha = 0.18f)
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("DESCANSO", fontSize = 11.sp, letterSpacing = 2.sp, color = Color.Gray)
            Text(fmt(leftMs), fontSize = 46.sp, fontWeight = FontWeight.Black, color = Color.White)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(60, 90, 120).forEach { s ->
                    val sel = s == totalSec
                    Box(
                        Modifier.clip(CircleShape).background(if (sel) accent else Color(0xFF262626))
                            .clickable { onPick(s) }.padding(horizontal = 10.dp, vertical = 5.dp)
                    ) { Text("${s}s", fontSize = 12.sp, color = if (sel) Color.Black else Color.White, fontWeight = FontWeight.Bold) }
                }
            }
            Spacer(Modifier.height(2.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = onReset, modifier = Modifier.size(38.dp), colors = ButtonDefaults.secondaryButtonColors()) { Text("↺", fontSize = 16.sp) }
                Button(onClick = onToggle, modifier = Modifier.size(52.dp), colors = ButtonDefaults.buttonColors(backgroundColor = accent, contentColor = Color.Black)) {
                    Text(if (running) "❚❚" else "▶", fontSize = 18.sp, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

// ───────────────────────── série ─────────────────────────

@Composable
private fun SetPage(setNumber: Int, reps: Int, kg: Float, accent: Color, onReps: (Int) -> Unit, onKg: (Float) -> Unit, onSave: () -> Unit) {
    var field by remember { mutableIntStateOf(1) }       // 0 = reps, 1 = kg (coroa mexe no selecionado)
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }

    Column(
        Modifier.fillMaxSize().padding(horizontal = 14.dp)
            .onRotaryScrollEvent { e ->
                val dir = if (e.verticalScrollPixels > 0) 1 else -1
                if (field == 0) onReps((reps + dir).coerceIn(1, 99)) else onKg((kg + 2.5f * dir).coerceIn(0f, 500f))
                true
            }.focusRequester(focus).focusable(),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
    ) {
        Text("SÉRIE $setNumber", fontSize = 11.sp, letterSpacing = 2.sp, color = Color.Gray)
        Spacer(Modifier.height(6.dp))
        Stepper("reps", "$reps", field == 0, accent, { field = 0 }, { onReps((reps - 1).coerceAtLeast(1)) }, { onReps((reps + 1).coerceAtMost(99)) })
        Spacer(Modifier.height(4.dp))
        Stepper("kg", if (kg % 1f == 0f) "${kg.toInt()}" else "%.1f".format(kg), field == 1, accent, { field = 1 }, { onKg((kg - 2.5f).coerceAtLeast(0f)) }, { onKg((kg + 2.5f).coerceAtMost(500f)) })
        Spacer(Modifier.height(8.dp))
        Button(onClick = onSave, modifier = Modifier.fillMaxWidth(0.8f).height(40.dp), colors = ButtonDefaults.buttonColors(backgroundColor = accent, contentColor = Color.Black)) {
            Text("REGISTRAR", fontWeight = FontWeight.Black, fontSize = 13.sp, letterSpacing = 1.sp)
        }
    }
}

@Composable
private fun Stepper(label: String, value: String, selected: Boolean, accent: Color, onSelect: () -> Unit, onMinus: () -> Unit, onPlus: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(CircleShape)
            .background(if (selected) accent.copy(alpha = 0.16f) else Color.Transparent)
            .clickable(onClick = onSelect).padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Button(onClick = onMinus, modifier = Modifier.size(34.dp), colors = ButtonDefaults.secondaryButtonColors()) { Text("−", fontSize = 18.sp) }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, fontSize = 26.sp, fontWeight = FontWeight.Black, color = if (selected) accent else Color.White)
            Text(label, fontSize = 10.sp, color = Color.Gray)
        }
        Button(onClick = onPlus, modifier = Modifier.size(34.dp), colors = ButtonDefaults.secondaryButtonColors()) { Text("+", fontSize = 18.sp) }
    }
}

// ───────────────────────── resumo ─────────────────────────

@Composable
private fun SummaryPage(sets: Int, volume: Float, startedAt: Long, accent: Color, accentIdx: Int, onAccent: (Int) -> Unit, onReset: () -> Unit) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { now = System.currentTimeMillis(); delay(1000) } }
    val elapsed = if (startedAt == 0L) 0L else (now - startedAt) / 1000
    val time = "%d:%02d".format(elapsed / 60, elapsed % 60)

    Column(Modifier.fillMaxSize().padding(horizontal = 18.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("TREINO", fontSize = 11.sp, letterSpacing = 2.sp, color = Color.Gray)
        Text(time, fontSize = 38.sp, fontWeight = FontWeight.Black, color = accent)
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            Stat("$sets", "séries")
            Stat(if (volume >= 1000) "%.1ft".format(volume / 1000) else "${volume.toInt()}kg", "volume")
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Accents.forEachIndexed { i, (_, c) ->
                Box(
                    Modifier.size(if (i == accentIdx) 26.dp else 20.dp).clip(CircleShape).background(c)
                        .clickable { onAccent(i) }
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Button(onClick = onReset, modifier = Modifier.height(34.dp), colors = ButtonDefaults.secondaryButtonColors()) {
            Text("Zerar", fontSize = 12.sp, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun Stat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Text(label, fontSize = 10.sp, color = Color.Gray)
    }
}
