package com.vitoroliveira.gymtracker.ui.body

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import com.vitoroliveira.gymtracker.ui.workout.shareWorkoutImage
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.vitoroliveira.gymtracker.presentation.body.*
import com.vitoroliveira.gymtracker.ui.components.ChartReference
import com.vitoroliveira.gymtracker.ui.components.LineChart
import com.vitoroliveira.gymtracker.ui.components.currentYear
import com.vitoroliveira.gymtracker.ui.components.todayEpochDay
import com.vitoroliveira.gymtracker.ui.theme.ThemedCard
import com.vitoroliveira.gymtracker.ui.theme.ThemedScaffold
import com.vitoroliveira.gymtracker.ui.theme.ThemedTopAppBar
import com.vitoroliveira.gymtracker.ui.utils.formatKg
import kotlinx.datetime.LocalDate
import org.koin.compose.viewmodel.koinViewModel

private fun dayLabel(epochDay: Int): String =
    LocalDate.fromEpochDays(epochDay).let { "%02d/%02d".format(it.dayOfMonth, it.monthNumber) }

private fun isoLabel(iso: String): String = iso.split("-").let { if (it.size == 3) "${it[2]}/${it[1]}" else iso }

@Composable
fun EvolutionScreen(
    onBack: () -> Unit,
    bodyVm: BodyViewModel = koinViewModel(),
    evoVm: EvolutionViewModel = koinViewModel()
) {
    val body by bodyVm.data.collectAsState()
    val evo by evoVm.state.collectAsState()
    EvolutionContent(
        body, evo, onBack, bodyVm::logWeight, bodyVm::deleteWeight, bodyVm::setProfile,
        onLogMeasure = { site, cm -> bodyVm.logMeasurement(todayEpochDay(), site, cm) },
        onDeleteMeasure = bodyVm::deleteMeasurement
    )
}

@Composable
fun EvolutionContent(
    body: BodyData, evo: EvolutionUiState, onBack: () -> Unit,
    onLog: (Int, Double) -> Unit, onDelete: (Int) -> Unit, onSaveProfile: (BodyProfile) -> Unit,
    onLogMeasure: (MeasureSite, Double) -> Unit = { _, _ -> },
    onDeleteMeasure: (Int, MeasureSite) -> Unit = { _, _ -> }
) {
    var showData by remember { mutableStateOf(false) }

    ThemedScaffold(
        topBar = {
            ThemedTopAppBar(
                title = { Text("Evolução") },
                navigationIcon = { IconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar") } },
                actions = { IconButton({ showData = true }) { Icon(Icons.Default.Tune, "Meus dados") } }
            )
        }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item(key = "weight") { BodyWeightCard(body, onLog = onLog, onDelete = onDelete, onEditData = { showData = true }) }
            item(key = "measures") { MeasurementsCard(body.measurements, onLogMeasure, onDeleteMeasure) }
            if (evo.recap?.isEmpty == false || evo.yearRecap?.isEmpty == false) item(key = "recap") { RecapCard(evo.recap, evo.yearRecap) }
            if (evo.weeklySets.any { it.sets > 0 }) item(key = "weekly-sets") { WeeklySetsCard(evo.weeklySets) }
            item(key = "lifts-title") {
                Text("Cargas nos básicos", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            if (evo.isLoading) item { CircularProgressIndicator() }
            if (!evo.isLoading && evo.lifts.isEmpty()) item {
                Text(
                    "Quando você treinar supino, agachamento, levantamento terra ou desenvolvimento (com barra), a evolução e o seu nível aparecem aqui.",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            items(evo.lifts, key = { it.exerciseName }) { LiftCard(it, body) }
            item(key = "foot") {
                Text(
                    "Referências de nível: StrengthLog (logs de usuários que registram treino, mais ativos que a média). " +
                        "O 1RM é estimado (Epley) e o IMC não separa músculo de gordura: use como guia, não como diagnóstico.",
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    if (showData) BodyDataDialog(body.profile, onSave = { onSaveProfile(it); showData = false }, onDismiss = { showData = false })
}

private val MONTHS_PT = listOf("Janeiro", "Fevereiro", "Março", "Abril", "Maio", "Junho", "Julho", "Agosto", "Setembro", "Outubro", "Novembro", "Dezembro")

/** Retrospectiva do mês (#354): números do período e botão para compartilhar como imagem. */
@Composable
private fun RecapCard(month: com.vitoroliveira.gymtracker.presentation.body.MonthlyRecap?, year: com.vitoroliveira.gymtracker.presentation.body.MonthlyRecap?) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val cs = MaterialTheme.colorScheme
    var yearly by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(month?.isEmpty != false && year != null) }
    val r = (if (yearly) year else month) ?: return
    val monthName = if (r.month == 0) "${r.year}" else MONTHS_PT[(r.month - 1).coerceIn(0, 11)]
    com.vitoroliveira.gymtracker.ui.theme.ThemedCard(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Retrospectiva de $monthName", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                if (month != null && year != null) {
                    androidx.compose.material3.FilterChip(!yearly, { yearly = false }, { Text("Mês") })
                    Spacer(Modifier.width(6.dp))
                    androidx.compose.material3.FilterChip(yearly, { yearly = true }, { Text("Ano") })
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                RecapStat("${r.workouts}", if (r.workouts == 1) "treino" else "treinos")
                RecapStat("${r.daysTrained}", if (r.daysTrained == 1) "dia" else "dias")
                RecapStat("${r.sets}", "séries")
                RecapStat(com.vitoroliveira.gymtracker.ui.utils.formatVolume(r.volumeKg), "volume")
            }
            r.topGroup?.let {
                Text("Mais treinado: ${com.vitoroliveira.gymtracker.ui.components.prettyGroupName(it)} (${r.topGroupWorkouts}x)", style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
            }
            androidx.compose.material3.OutlinedButton(
                onClick = {
                    val colors = com.vitoroliveira.gymtracker.ui.workout.ShareColors(
                        bgTop = cs.background.toArgb(),
                        bgBottom = cs.surfaceContainerHighest.compositeOver(cs.background).toArgb(),
                        text = cs.onBackground.toArgb(), muted = cs.onSurfaceVariant.toArgb(),
                        accent = cs.primary.toArgb(), onAccent = cs.onPrimary.toArgb()
                    )
                    val stats = com.vitoroliveira.gymtracker.ui.workout.ShareStats(
                        title = if (r.month == 0) "Meu ${r.year}" else "$monthName de ${r.year}",
                        dateText = "${r.year}",
                        subtitle = if (r.month == 0) "Minha retrospectiva do ano" else "Minha retrospectiva do mês",
                        blocks = listOf(
                            "TREINOS" to "${r.workouts}",
                            "SÉRIES" to "${r.sets}",
                            "VOLUME" to com.vitoroliveira.gymtracker.ui.utils.formatVolume(r.volumeKg)
                        ),
                        records = emptyList(),
                        footer = "${r.daysTrained} dias treinados" + (r.topGroup?.let { " · mais treinado: ${com.vitoroliveira.gymtracker.ui.components.prettyGroupName(it)}" } ?: "") + " · GymTracker"
                    )
                    runCatching { ctx.shareWorkoutImage(com.vitoroliveira.gymtracker.ui.workout.renderWorkoutImage(stats, colors), "gymtracker_retrospectiva.png") }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Compartilhar como imagem") }
        }
    }
}

@Composable
private fun RecapStat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Séries por músculo nos últimos 7 dias, com a faixa de referência marcada. */
@Composable
private fun WeeklySetsCard(rows: List<com.vitoroliveira.gymtracker.presentation.body.MuscleSets>) {
    val range = com.vitoroliveira.gymtracker.presentation.body.WEEKLY_SETS_RANGE
    com.vitoroliveira.gymtracker.ui.theme.ThemedCard(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Séries por músculo (7 dias)", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            // #353: mapa muscular (frente e costas) pintado pelas séries da semana
            com.vitoroliveira.gymtracker.ui.components.MuscleMap(rows, fullAtSets = range.last)
            rows.forEach { r ->
                val inRange = r.sets in range
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(r.label, Modifier.width(96.dp), style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                    LinearProgressIndicator(
                        progress = { (r.sets / range.last.toFloat()).coerceIn(0f, 1f) },
                        modifier = Modifier.weight(1f).height(10.dp),
                        color = when {
                            r.sets == 0 -> MaterialTheme.colorScheme.outline
                            inRange -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.tertiary
                        }
                    )
                    Text(
                        "${r.sets}", Modifier.width(36.dp), textAlign = androidx.compose.ui.text.style.TextAlign.End,
                        fontWeight = if (inRange) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
            Text(
                "Referência comum para hipertrofia: ${range.first} a ${range.last} séries por músculo por semana (Schoenfeld e colegas, 2017). " +
                    "É uma faixa, não regra: iniciantes crescem com menos. Exercícios fora do catálogo contam em \"Outros\".",
                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Medidas com fita: último valor de cada local e quanto mudou desde a primeira medida. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MeasurementsCard(list: List<BodyMeasurement>, onLog: (MeasureSite, Double) -> Unit, onDelete: (Int, MeasureSite) -> Unit) {
    var site by remember { mutableStateOf(MeasureSite.WAIST) }
    var input by remember { mutableStateOf("") }
    val summary = remember(list) { summarizeMeasurements(list) }
    ThemedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Medidas", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            if (summary.isEmpty()) Text(
                "Meça com fita (sempre no mesmo lugar e horário) para ver se a cintura cai e o braço cresce, mesmo quando a balança não mexe.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            summary.forEach { s ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(s.site.label, Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                    Text("${formatKg(s.latestCm)} cm", fontWeight = FontWeight.Bold)
                    if (s.firstDay != s.latestDay) {
                        val sign = if (s.changeCm > 0) "+" else ""
                        Text(
                            "  ($sign${formatKg(s.changeCm)} desde ${dayLabel(s.firstDay)})",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton({ onDelete(s.latestDay, s.site) }) { Icon(Icons.Default.Close, "Apagar a última medida de ${s.site.label}") }
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                MeasureSite.entries.forEach { m -> FilterChip(site == m, { site = m }, { Text(m.label) }) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    input, { input = it.filter { c -> c.isDigit() || c == ',' || c == '.' }.take(5) },
                    Modifier.weight(1f), label = { Text("${site.label} hoje (cm)") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                Button(
                    onClick = { input.replace(',', '.').toDoubleOrNull()?.let { onLog(site, it); input = "" } },
                    enabled = input.replace(',', '.').toDoubleOrNull() != null
                ) { Text("Registrar") }
            }
        }
    }
}

@Composable
private fun BodyWeightCard(body: BodyData, onLog: (Int, Double) -> Unit, onDelete: (Int) -> Unit, onEditData: () -> Unit) {
    var input by remember { mutableStateOf("") }
    val h = body.profile.heightCm
    val latest = body.latestKg
    val bmi = if (latest != null && h != null) Bmi.value(latest, h) else null

    ThemedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Peso corporal", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            if (latest != null) {
                Text("${formatKg(latest)} kg", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black)
                if (bmi != null) Text(
                    "IMC ${"%.1f".format(bmi).replace('.', ',')} · ${Bmi.category(bmi).label}",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant
                ) else TextButton(onEditData) { Text("Informe sua altura para ver o IMC") }
            }
            val range = h?.let { Bmi.normalRangeKg(it) }
            if (body.weights.size >= 2) {
                val kgs = body.weights.map { it.kg }
                // Data-Dense Pro: números tabulares em fileira, gráfico como herói logo abaixo
                DataStatRow(
                    listOf(
                        "Variação" to signedKg(kgs.last() - kgs.first()),
                        "Mínimo" to "${formatKg(kgs.min())} kg",
                        "Máximo" to "${formatKg(kgs.max())} kg",
                        "Registros" to "${kgs.size}"
                    )
                )
                LineChart(
                    values = body.weights.map { it.kg },
                    xLabels = body.weights.map { dayLabel(it.epochDay) },
                    modifier = Modifier.fillMaxWidth().height(220.dp),
                    band = range, bandLabel = range?.let { "faixa normal (IMC)" }
                )
            } else Text(
                if (latest == null) "Registre seu peso para acompanhar a evolução." else "Registre mais um dia para ver o gráfico.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    input, { input = it.filter { c -> c.isDigit() || c == ',' || c == '.' } },
                    Modifier.weight(1f), label = { Text("Peso de hoje (kg)") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                Button(
                    onClick = { input.replace(',', '.').toDoubleOrNull()?.let { onLog(todayEpochDay(), it); input = "" } },
                    enabled = input.replace(',', '.').toDoubleOrNull() != null
                ) { Text("Registrar") }
            }
            body.weights.takeLast(5).reversed().forEach { w ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("${dayLabel(w.epochDay)}  ·  ${formatKg(w.kg)} kg", Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                    IconButton({ onDelete(w.epochDay) }) { Icon(Icons.Default.Close, "Apagar") }
                }
            }
        }
    }
}

private fun signedKg(v: Double): String {
    val r = Math.round(v * 10) / 10.0
    return (if (r > 0) "+" else "") + formatKg(r) + " kg"
}

/** Fileira de números "densos": rótulo pequeno em cima, valor em algarismos tabulares (não dançam ao mudar). */
@Composable
private fun DataStatRow(stats: List<Pair<String, String>>) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        stats.forEach { (label, value) ->
            Column(Modifier.weight(1f)) {
                Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                Text(
                    value, style = MaterialTheme.typography.titleSmall.copy(fontFeatureSettings = "tnum"),
                    fontWeight = FontWeight.Bold, maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun LiftCard(s: LiftSeries, body: BodyData) {
    val bw = body.latestKg
    val sex = body.profile.sex
    val levels = bw?.let { StrengthStandards.oneRepMaxByLevel(s.lift, it, sex) }
    val level = bw?.let { StrengthStandards.levelFor(s.lift, s.bestOneRm, it, sex) }
    val next = StrengthStandards.nextLevel(level)
    val refs = buildList {
        if (levels != null) {
            level?.let { add(ChartReference(it.label, levels.getValue(it))) }
            next?.let { add(ChartReference(it.label, levels.getValue(it), MaterialTheme.colorScheme.tertiary)) }
        }
    }
    ThemedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(s.exerciseName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("1RM estimado: ${formatKg(Math.round(s.bestOneRm * 10) / 10.0)} kg", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (bw != null) AssistChip(onClick = {}, label = { Text(level?.label ?: "Abaixo de Iniciante") })
            }
            if (s.oneRepMaxes.size >= 2) {
                val first = s.oneRepMaxes.first()
                val last = s.oneRepMaxes.last()
                DataStatRow(
                    listOf(
                        "Atual" to "${formatKg(Math.round(last * 10) / 10.0)} kg",
                        "Melhor" to "${formatKg(Math.round(s.bestOneRm * 10) / 10.0)} kg",
                        "Variação" to (signedKg(last - first) + if (first > 0) " (${"%+.0f".format((last - first) / first * 100)}%)" else ""),
                        "Sessões" to "${s.oneRepMaxes.size}"
                    )
                )
            }
            LineChart(
                values = s.oneRepMaxes, xLabels = s.dates.map(::isoLabel),
                modifier = Modifier.fillMaxWidth().height(200.dp), references = refs
            )
            if (bw == null) Text("Registre seu peso corporal para ver as referências de nível.", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            else if (next != null && levels != null) {
                val missing = levels.getValue(next) - s.bestOneRm
                Text("Para ${next.label}: faltam ~${formatKg(Math.round(missing * 10) / 10.0)} kg de 1RM " +
                    "(${next.percentile}% das pessoas ficam abaixo desse nível).",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else if (level == StrengthLevel.ELITE) Text("Nível Elite: mais forte que ~95% dos usuários.", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun BodyDataDialog(profile: BodyProfile, onSave: (BodyProfile) -> Unit, onDismiss: () -> Unit) {
    var height by remember { mutableStateOf(profile.heightCm?.toString() ?: "") }
    var year by remember { mutableStateOf(profile.birthYear?.toString() ?: "") }
    var sex by remember { mutableStateOf(profile.sex) }
    var ask by remember { mutableStateOf(profile.askWeightEachWorkout) }
    val yearOk = year.isBlank() || year.toIntOrNull()?.let { currentYear() - it in 10..110 } == true
    val heightOk = height.isBlank() || height.toIntOrNull()?.let { it in 100..250 } == true
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Meus dados") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Tudo é opcional. Fica no aparelho e com uma cópia na sua conta (só você vê), para não perder ao trocar de celular. Serve para sugerir cargas e mostrar referências.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(height, { height = it.filter(Char::isDigit).take(3) }, label = { Text("Altura (cm)") }, singleLine = true,
                    isError = !heightOk, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                OutlinedTextField(year, { year = it.filter(Char::isDigit).take(4) }, label = { Text("Ano de nascimento") }, singleLine = true,
                    isError = !yearOk, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                Text("Sexo (usado só nas referências de força)", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Sex.entries.forEach { s -> FilterChip(sex == s, { sex = if (sex == s) null else s }, { Text(s.label) }) }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Perguntar meu peso ao iniciar cada treino", Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                    Switch(ask, { ask = it })
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = yearOk && heightOk,
                onClick = { onSave(BodyProfile(height.toIntOrNull(), year.toIntOrNull(), sex, ask)) }
            ) { Text("Salvar") }
        },
        dismissButton = { TextButton(onDismiss) { Text("Cancelar") } }
    )
}
