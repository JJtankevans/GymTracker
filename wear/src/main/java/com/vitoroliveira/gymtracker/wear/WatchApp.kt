package com.vitoroliveira.gymtracker.wear

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.ScalingLazyListState
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.Colors
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.ListHeader
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Switch
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.material.ToggleChip
import androidx.wear.compose.material.Vignette
import androidx.wear.compose.material.VignettePosition
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import com.vitoroliveira.gymtracker.watch.WatchRoutine
import com.vitoroliveira.gymtracker.watch.formatKg
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

val Accents = listOf(
    "Limão" to Color(0xFFC6FF00),
    "Neon" to Color(0xFFFF2BD6),
    "Ciano" to Color(0xFF22E3FF),
    "Magma" to Color(0xFFFF7A1A),
    "Ouro" to Color(0xFFFFC53D),
    "Gelo" to Color(0xFFE8F1FF)
)

val Surface = Color(0xFF1A1C1A)
val Muted = Color(0xFF9AA09A)
val Gold = Color(0xFFFFC53D)
val Danger = Color(0xFFFF5A4E)

fun parseHex(hex: String, fallback: Color): Color =
    runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrDefault(fallback)

fun clock(ms: Long): String = SimpleDateFormat("HH:mm", Locale("pt", "BR")).format(Date(ms))

fun duration(ms: Long): String {
    val s = (ms / 1000).coerceAtLeast(0)
    val h = s / 3600
    return if (h > 0) "%d:%02d:%02d".format(h, (s % 3600) / 60, s % 60) else "%d:%02d".format(s / 60, s % 60)
}

private object Routes {
    const val HOME = "inicio"
    const val WORKOUT = "treino"
    const val SUMMARY = "resumo"
    const val SETTINGS = "ajustes"
    const val SWITCH = "trocar/{i}"
    const val FINISH = "terminar"
    const val DISCARD = "descartar"
}

@Composable
fun WatchApp(c: WatchController) {
    val accent = Accents[c.accent.coerceIn(0, Accents.lastIndex)].second
    MaterialTheme(
        colors = Colors(
            primary = accent, primaryVariant = accent, secondary = accent, secondaryVariant = accent,
            onPrimary = Color.Black, onSecondary = Color.Black,
            background = Color.Black, surface = Surface, onSurface = Color.White, onBackground = Color.White,
            error = Danger
        )
    ) {
        val nav = rememberSwipeDismissableNavController()
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            SwipeDismissableNavHost(navController = nav, startDestination = Routes.HOME) {
                composable(Routes.HOME) {
                    HomeScreen(
                        c, accent,
                        onContinue = { nav.navigate(Routes.WORKOUT) },
                        onRoutine = { i, r ->
                            if (c.session != null) nav.navigate("trocar/$i")
                            else { c.startRoutine(r); nav.navigate(Routes.WORKOUT) }
                        },
                        onQuick = {
                            if (c.session != null) nav.navigate("trocar/-1")
                            else { c.startQuick(); nav.navigate(Routes.WORKOUT) }
                        },
                        onSettings = { nav.navigate(Routes.SETTINGS) }
                    )
                }
                composable(Routes.WORKOUT) {
                    WorkoutScreen(
                        c, accent,
                        onFinish = { nav.navigate(Routes.FINISH) },
                        onDiscard = { nav.navigate(Routes.DISCARD) },
                        onGone = { nav.popBackStack(Routes.HOME, inclusive = false) }
                    )
                }
                composable(Routes.FINISH) {
                    ConfirmScreen(
                        accent, "Terminar o treino?",
                        c.session?.let { "${it.totalSets} séries · ${duration(System.currentTimeMillis() - it.startedAt)}" } ?: "",
                        onYes = {
                            c.finish()
                            nav.navigate(Routes.SUMMARY) { popUpTo(Routes.HOME) }
                        },
                        onNo = { nav.popBackStack() }
                    )
                }
                composable(Routes.DISCARD) {
                    ConfirmScreen(
                        Danger, "Descartar o treino?", "As séries deste treino serão apagadas do relógio.",
                        onYes = { c.discard(); nav.popBackStack(Routes.HOME, inclusive = false) },
                        onNo = { nav.popBackStack() }
                    )
                }
                composable(Routes.SWITCH) { entry ->
                    val i = entry.arguments?.getString("i")?.toIntOrNull() ?: -1
                    val routine = c.routines?.routines?.getOrNull(i)
                    ConfirmScreen(
                        Danger, "Trocar de treino?",
                        "O treino em andamento (${c.session?.title ?: ""}) será descartado.",
                        onYes = {
                            if (routine != null) c.startRoutine(routine) else c.startQuick()
                            nav.navigate(Routes.WORKOUT) { popUpTo(Routes.HOME) }
                        },
                        onNo = { nav.popBackStack() }
                    )
                }
                composable(Routes.SUMMARY) {
                    SummaryScreen(c, accent) { nav.popBackStack(Routes.HOME, inclusive = false) }
                }
                composable(Routes.SETTINGS) { SettingsScreen(c, accent) }
            }
            if (c.ambient) AmbientScreen(c)
        }
    }
}

/** Lista com hora no topo, vinheta e indicador de rolagem (o jeito Wear OS). */
@Composable
fun WatchList(state: ScalingLazyListState = rememberScalingLazyListState(), content: androidx.wear.compose.foundation.lazy.ScalingLazyListScope.() -> Unit) {
    Scaffold(
        timeText = { TimeText() },
        vignette = { Vignette(vignettePosition = VignettePosition.TopAndBottom) },
        positionIndicator = { PositionIndicator(scalingLazyListState = state) }
    ) {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = state,
            verticalArrangement = Arrangement.spacedBy(6.dp),
            content = content
        )
    }
}

// ───────────────────────── início ─────────────────────────

@Composable
private fun HomeScreen(
    c: WatchController,
    accent: Color,
    onContinue: () -> Unit,
    onRoutine: (Int, WatchRoutine) -> Unit,
    onQuick: () -> Unit,
    onSettings: () -> Unit
) {
    val routines = c.routines
    WatchList {
        item {
            ListHeader {
                Text("GYMTRACKER", color = accent, fontWeight = FontWeight.Black, letterSpacing = 2.sp, fontSize = 13.sp)
            }
        }
        c.session?.let { s ->
            item {
                Chip(
                    onClick = onContinue,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Continuar treino", fontWeight = FontWeight.Bold) },
                    secondaryLabel = {
                        Text(
                            if (s.isQuick) "${s.totalSets} séries" else "${s.title} · ${s.doneExercises}/${s.exercises.size}",
                            maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                    },
                    icon = { Icon(Icons.Filled.PlayArrow, contentDescription = null) },
                    colors = ChipDefaults.primaryChipColors()
                )
            }
        }
        val list = routines?.routines.orEmpty()
        if (list.isNotEmpty()) {
            item { ListHeader { Text("Treinos", color = Muted) } }
            items(list.size) { i ->
                val r = list[i]
                Chip(
                    onClick = { onRoutine(i, r) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(r.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    secondaryLabel = { Text("${r.exercises.size} exercício" + if (r.exercises.size == 1) "" else "s", color = Muted) },
                    icon = {
                        Box(
                            Modifier.size(22.dp).clip(CircleShape)
                                .background(parseHex(r.colorHex, accent).copy(alpha = 0.25f))
                                .border(2.dp, parseHex(r.colorHex, accent), CircleShape)
                        )
                    },
                    colors = ChipDefaults.secondaryChipColors()
                )
            }
        } else {
            item { EmptyRoutines(c, accent) }
        }
        item {
            Chip(
                onClick = onQuick,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Treino rápido") },
                secondaryLabel = { Text("Sem exercício · fica no relógio", color = Muted, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                icon = { Icon(Icons.Filled.Favorite, contentDescription = null, tint = accent) },
                colors = ChipDefaults.secondaryChipColors()
            )
        }
        if (c.outbox.items.isNotEmpty()) {
            item {
                val n = c.outbox.items.size
                Chip(
                    onClick = { c.refresh() },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(if (n == 1) "1 treino na fila" else "$n treinos na fila") },
                    secondaryLabel = { Text("Vai quando o celular estiver perto", color = Muted, maxLines = 2) },
                    icon = { Icon(Icons.Filled.Warning, contentDescription = null, tint = Gold) },
                    colors = ChipDefaults.secondaryChipColors()
                )
            }
        }
        item {
            Chip(
                onClick = { c.refresh() },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Sincronizar") },
                secondaryLabel = { Text(syncText(c), color = Muted, maxLines = 2) },
                icon = { Icon(Icons.Filled.Refresh, contentDescription = null) },
                colors = ChipDefaults.secondaryChipColors()
            )
        }
        item {
            Chip(
                onClick = onSettings,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Ajustes") },
                icon = { Icon(Icons.Filled.Settings, contentDescription = null) },
                colors = ChipDefaults.secondaryChipColors()
            )
        }
    }
}

private fun syncText(c: WatchController): String = when {
    c.phone == PhoneStatus.CHECKING -> "Procurando o celular…"
    c.routines?.loggedIn == false -> "Entre na sua conta no celular"
    c.phone == PhoneStatus.OUT_OF_RANGE && c.routinesAt > 0 -> "Celular longe · dados de ${clock(c.routinesAt)}"
    c.phone == PhoneStatus.OUT_OF_RANGE -> "Celular fora de alcance"
    c.phone == PhoneStatus.ERROR -> "Sem os serviços do Google no relógio"
    c.routinesAt > 0 -> "Atualizado às ${clock(c.routinesAt)}"
    else -> "Abra o GymTracker no celular"
}

@Composable
private fun EmptyRoutines(c: WatchController, accent: Color) {
    val (title, body) = when {
        c.routines?.loggedIn == false -> "Sem conta" to "Entre no GymTracker do celular e toque em Sincronizar."
        c.routines != null -> "Nenhum treino" to "Crie grupos com exercícios no celular."
        else -> "Ligue ao celular" to "Abra o GymTracker no celular (instalado do mesmo build) e toque em Sincronizar."
    }
    Column(
        Modifier.fillMaxWidth().clip(androidx.compose.foundation.shape.RoundedCornerShape(18.dp)).background(Surface).padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Filled.Info, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
        Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp, textAlign = TextAlign.Center)
        Text(body, color = Muted, fontSize = 11.sp, textAlign = TextAlign.Center)
    }
}

// ───────────────────────── confirmação ─────────────────────────

@Composable
fun ConfirmScreen(color: Color, title: String, body: String, onYes: () -> Unit, onNo: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(horizontal = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(title, fontSize = 17.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        if (body.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            Text(body, fontSize = 12.sp, color = Muted, textAlign = TextAlign.Center, maxLines = 3)
        }
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            Button(onClick = onNo, colors = ButtonDefaults.secondaryButtonColors(), modifier = Modifier.size(52.dp)) {
                Icon(Icons.Filled.Close, contentDescription = "Não")
            }
            Button(
                onClick = onYes, modifier = Modifier.size(52.dp),
                colors = ButtonDefaults.buttonColors(backgroundColor = color, contentColor = Color.Black)
            ) {
                Icon(Icons.Filled.Check, contentDescription = "Sim")
            }
        }
    }
}

// ───────────────────────── ajustes ─────────────────────────

private val RestOptions = listOf(45, 60, 75, 90, 120, 150, 180, 240)
private val KgSteps = listOf(1f, 2.5f, 5f)

@Composable
private fun SettingsScreen(c: WatchController, accent: Color) {
    WatchList {
        item { ListHeader { Text("Ajustes", color = accent, fontWeight = FontWeight.Bold) } }
        item {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text("Cor", color = Muted, fontSize = 11.sp)
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Accents.forEachIndexed { i, (_, color) ->
                        val sel = i == c.accent
                        Box(
                            Modifier.size(if (sel) 26.dp else 20.dp).clip(CircleShape).background(color)
                                .then(if (sel) Modifier.border(2.dp, Color.White, CircleShape) else Modifier)
                                .clickable { c.setAccentIndex(i) }
                        )
                    }
                }
            }
        }
        item {
            Chip(
                onClick = {
                    val i = RestOptions.indexOf(c.restSeconds)
                    c.setRestDefault(RestOptions[(i + 1) % RestOptions.size])
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Descanso: ${duration(c.restSeconds * 1000L)}") },
                secondaryLabel = { Text("Toque para mudar", color = Muted) },
                colors = ChipDefaults.secondaryChipColors()
            )
        }
        item {
            Chip(
                onClick = {
                    val i = KgSteps.indexOf(c.kgStep)
                    c.setKgStepValue(KgSteps[(i + 1) % KgSteps.size])
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Passo da carga: ${formatKg(c.kgStep.toDouble())} kg") },
                secondaryLabel = { Text("Coroa e botões − / +", color = Muted) },
                colors = ChipDefaults.secondaryChipColors()
            )
        }
        item {
            ToggleChip(
                checked = c.autoRest,
                onCheckedChange = { c.setAutoRestOn(it) },
                label = { Text("Descanso automático") },
                secondaryLabel = { Text("Começa ao registrar a série", color = Muted, maxLines = 2) },
                toggleControl = { Switch(checked = c.autoRest) },
                modifier = Modifier.fillMaxWidth()
            )
        }
        item {
            ToggleChip(
                checked = c.heartRateOn,
                onCheckedChange = { c.setHeartRateEnabled(it) },
                label = { Text("Batimentos") },
                secondaryLabel = { Text("Durante o treino", color = Muted) },
                toggleControl = { Switch(checked = c.heartRateOn) },
                modifier = Modifier.fillMaxWidth()
            )
        }
        item {
            Text(
                "GymTracker para relógio 2.0",
                color = Muted, fontSize = 10.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
            )
        }
    }
}

// ───────────────────────── modo ambiente ─────────────────────────

/** Tela sempre ligada (pouca luz): só texto branco sobre preto, sem animação, atualiza pouco. */
@Composable
private fun AmbientScreen(c: WatchController) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { now = System.currentTimeMillis(); delay(30_000) } }
    Box(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(clock(now), fontSize = 34.sp, fontWeight = FontWeight.Light, color = Color.White)
            val rest = c.rest
            val s = c.session
            when {
                rest != null -> {
                    Text("DESCANSO", fontSize = 11.sp, letterSpacing = 2.sp, color = Color.Gray)
                    Text("até ${clock(rest.endsAt)}", fontSize = 16.sp, color = Color.White)
                }
                s != null -> {
                    Text(
                        if (s.isQuick) "Treino rápido" else s.current.name,
                        fontSize = 14.sp, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                    Text(
                        if (s.isQuick) "${s.totalSets} séries" else "série ${s.setsDone(s.current.id) + 1} de ${s.current.sets}",
                        fontSize = 12.sp, color = Color.Gray
                    )
                }
            }
        }
    }
}
