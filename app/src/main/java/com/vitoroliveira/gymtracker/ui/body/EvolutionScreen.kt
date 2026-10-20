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
    EvolutionContent(body, evo, onBack, bodyVm::logWeight, bodyVm::deleteWeight, bodyVm::setProfile)
}

@Composable
fun EvolutionContent(
    body: BodyData, evo: EvolutionUiState, onBack: () -> Unit,
    onLog: (Int, Double) -> Unit, onDelete: (Int) -> Unit, onSaveProfile: (BodyProfile) -> Unit
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
                LineChart(
                    values = body.weights.map { it.kg },
                    xLabels = body.weights.map { dayLabel(it.epochDay) },
                    modifier = Modifier.fillMaxWidth().height(190.dp),
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
            LineChart(
                values = s.oneRepMaxes, xLabels = s.dates.map(::isoLabel),
                modifier = Modifier.fillMaxWidth().height(180.dp), references = refs
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
                Text("Tudo é opcional e fica só neste aparelho. Servem para sugerir cargas e mostrar referências.",
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
