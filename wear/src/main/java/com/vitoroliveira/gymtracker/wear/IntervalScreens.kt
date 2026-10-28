package com.vitoroliveira.gymtracker.wear

import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.CompactChip
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.ListHeader
import androidx.wear.compose.material.Text
import com.vitoroliveira.gymtracker.watch.HrZone
import com.vitoroliveira.gymtracker.watch.IntervalPhase
import com.vitoroliveira.gymtracker.watch.IntervalPlan
import com.vitoroliveira.gymtracker.watch.IntervalPresets

// ───────────────────────── timer de intervalos (HIIT, Tabata, EMOM) ─────────────────────────

private val WorkColor = Color(0xFFFF5A4E)
private val RestColor = Color(0xFF22E3FF)

/** Lista: o personalizado (último usado), os prontos e "Ajustar". */
@Composable
fun IntervalMenuScreen(c: WatchController, accent: Color, onRun: () -> Unit, onEdit: () -> Unit) {
    WatchList {
        item { ListHeader { Text("Intervalos", color = accent, fontWeight = FontWeight.Bold) } }
        c.intervalRun?.takeIf { !it.done }?.let {
            item {
                Chip(
                    onClick = onRun,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Voltar ao timer", fontWeight = FontWeight.Bold) },
                    secondaryLabel = { Text(it.plan.label) },
                    icon = { Icon(Icons.Filled.PlayArrow, contentDescription = null) },
                    colors = ChipDefaults.primaryChipColors()
                )
            }
        }
        item {
            Chip(
                onClick = { c.startInterval(c.intervalPlan); onRun() },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Meu timer") },
                secondaryLabel = { Text("${c.intervalPlan.label} · ${duration(c.intervalPlan.totalMs)}", color = Muted, maxLines = 1) },
                icon = { Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = accent) },
                colors = ChipDefaults.secondaryChipColors()
            )
        }
        item {
            Chip(
                onClick = onEdit,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Ajustar meu timer") },
                icon = { Icon(Icons.Filled.Settings, contentDescription = null) },
                colors = ChipDefaults.secondaryChipColors()
            )
        }
        item { ListHeader { Text("Prontos", color = Muted) } }
        IntervalPresets.forEach { (name, plan) ->
            item {
                Chip(
                    onClick = { c.startInterval(plan); onRun() },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(name) },
                    secondaryLabel = { Text("${plan.label} · ${duration(plan.totalMs)}", color = Muted, maxLines = 1) },
                    colors = ChipDefaults.secondaryChipColors()
                )
            }
        }
        item {
            Text(
                "O timer vibra nas trocas. A tela do timer fica acesa; fora dela o timer continua vibrando enquanto o app estiver aberto.",
                color = Muted, fontSize = 10.sp, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)
            )
        }
    }
}

/** Ajustar esforço, pausa e rodadas com − / +. */
@Composable
fun IntervalEditScreen(c: WatchController, accent: Color, onStart: () -> Unit) {
    var plan by remember { mutableStateOf(c.intervalPlan) }
    fun update(p: IntervalPlan) { plan = p.normalized(); c.saveIntervalPlan(plan) }
    WatchList {
        item { ListHeader { Text("Meu timer", color = accent, fontWeight = FontWeight.Bold) } }
        item { Stepper("Esforço", "${plan.workSec}s", { update(plan.copy(workSec = plan.workSec - 5)) }, { update(plan.copy(workSec = plan.workSec + 5)) }) }
        item { Stepper("Pausa", if (plan.restSec == 0) "sem" else "${plan.restSec}s", { update(plan.copy(restSec = plan.restSec - 5)) }, { update(plan.copy(restSec = plan.restSec + 5)) }) }
        item { Stepper("Rodadas", "${plan.rounds}", { update(plan.copy(rounds = plan.rounds - 1)) }, { update(plan.copy(rounds = plan.rounds + 1)) }) }
        item { Stepper("Preparo", "${plan.prepSec}s", { update(plan.copy(prepSec = plan.prepSec - 5)) }, { update(plan.copy(prepSec = plan.prepSec + 5)) }) }
        item {
            Text("Total ${duration(plan.totalMs)}", color = Muted, fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }
        item {
            Chip(
                onClick = { c.startInterval(plan); onStart() },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("COMEÇAR", fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Filled.PlayArrow, contentDescription = null) },
                colors = ChipDefaults.primaryChipColors()
            )
        }
    }
}

@Composable
private fun Stepper(label: String, value: String, onMinus: () -> Unit, onPlus: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Button(onClick = onMinus, modifier = Modifier.size(ButtonDefaults.SmallButtonSize), colors = ButtonDefaults.secondaryButtonColors()) { Text("−", fontSize = 18.sp) }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, color = Muted, fontSize = 10.sp)
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
        Button(onClick = onPlus, modifier = Modifier.size(ButtonDefaults.SmallButtonSize), colors = ButtonDefaults.secondaryButtonColors()) { Text("+", fontSize = 18.sp) }
    }
}

/** O timer rodando: anel da fase, contagem, rodada, batimentos, pausar e parar. */
@Composable
fun IntervalRunScreen(c: WatchController, accent: Color, onClose: () -> Unit) {
    val run = c.intervalRun
    if (run == null) {
        LaunchedEffect(Unit) { onClose() }
        return
    }
    AskPermissionsOnce(c)
    val view = LocalView.current
    DisposableEffect(Unit) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }
    val now = c.intervalNow
    val st = run.plan.stateAt(run.elapsed(now))
    val color = when (st.phase) {
        IntervalPhase.WORK -> WorkColor
        IntervalPhase.REST -> RestColor
        IntervalPhase.PREP -> accent
        IntervalPhase.DONE -> Gold
    }
    val progress by animateFloatAsState(st.fraction, label = "intervalo")

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(
            progress = if (st.phase == IntervalPhase.DONE) 1f else progress,
            modifier = Modifier.fillMaxSize().padding(3.dp),
            startAngle = 270f, endAngle = 270f + 359.9f, strokeWidth = 8.dp,
            indicatorColor = color, trackColor = color.copy(alpha = 0.15f)
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                when (st.phase) {
                    IntervalPhase.PREP -> "PREPARE-SE"
                    IntervalPhase.WORK -> "VAI!"
                    IntervalPhase.REST -> "PAUSA"
                    IntervalPhase.DONE -> "FIM!"
                },
                color = color, fontWeight = FontWeight.Black, fontSize = 14.sp, letterSpacing = 2.sp
            )
            if (st.phase == IntervalPhase.DONE) {
                Text(duration(run.plan.totalMs), fontSize = 40.sp, fontWeight = FontWeight.Black)
                Text("${run.plan.rounds} rodadas", color = Muted, fontSize = 12.sp)
            } else {
                Text(duration(st.remainingMs + 999), fontSize = 46.sp, fontWeight = FontWeight.Black, color = if (run.paused) Muted else Color.White)
                Text("rodada ${st.round} de ${run.plan.rounds}", color = Muted, fontSize = 12.sp)
            }
            c.heartRate?.let { bpm ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    HeartChip(bpm)
                    c.heartZone?.takeIf { it != HrZone.REST }?.let { z ->
                        Text("  zona ${z.number}", color = zoneColor(z), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                if (st.phase == IntervalPhase.DONE) {
                    CompactChip(
                        onClick = { c.startInterval(run.plan) },
                        label = { Text("De novo") },
                        icon = { Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(14.dp)) },
                        colors = ChipDefaults.secondaryChipColors()
                    )
                } else {
                    CompactChip(
                        onClick = { c.toggleIntervalPause() },
                        label = { Text(if (run.paused) "Seguir" else "Pausar") },
                        colors = ChipDefaults.chipColors(backgroundColor = color, contentColor = Color.Black)
                    )
                }
                Spacer(Modifier.width(2.dp))
                CompactChip(
                    onClick = { c.stopInterval(); onClose() },
                    label = { Text(if (st.phase == IntervalPhase.DONE) "Sair" else "Parar") },
                    icon = { Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(14.dp)) },
                    colors = ChipDefaults.secondaryChipColors()
                )
            }
        }
    }
}
