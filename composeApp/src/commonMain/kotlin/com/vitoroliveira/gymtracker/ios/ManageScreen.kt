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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vitoroliveira.gymtracker.data.catalog.CatalogExercise
import com.vitoroliveira.gymtracker.data.catalog.CatalogMuscle
import com.vitoroliveira.gymtracker.data.catalog.ExerciseCatalog
import com.vitoroliveira.gymtracker.data.catalog.WorkoutTemplates
import com.vitoroliveira.gymtracker.data.model.MuscleGroup
import com.vitoroliveira.gymtracker.presentation.viewmodel.ExerciseViewModel
import com.vitoroliveira.gymtracker.presentation.viewmodel.MuscleGroupViewModel

private val GroupColors = listOf("#F44336", "#FF9800", "#FFC107", "#4CAF50", "#00BCD4", "#2196F3", "#9C27B0", "#E91E63")

/** Montar treinos no iPhone: treinos prontos, novo grupo e exercícios do catálogo (mesmos ViewModels do Android). */
@Composable
fun ManageScreen(groupsVm: MuscleGroupViewModel, onClose: () -> Unit) {
    val g by groupsVm.uiState.collectAsState()
    val exercisesVm = remember { ExerciseViewModel(Services.exercises) }
    val e by exercisesVm.uiState.collectAsState()
    var picking by remember { mutableStateOf<MuscleGroup?>(null) }
    var newName by remember { mutableStateOf("") }
    var newColor by remember { mutableStateOf(GroupColors[0]) }

    picking?.let { group ->
        CatalogPicker(group, onCancel = { picking = null }) { chosen ->
            exercisesVm.createMany(chosen, group.id)
            picking = null
        }
        return
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onClose) { Text("‹ Voltar") }
                Text("Montar treinos", fontSize = 24.sp, fontWeight = FontWeight.Black)
            }
        }
        listOfNotNull(g.successMessage, e.successMessage).firstOrNull()?.let { item { Text(it, color = Lime) } }
        listOfNotNull(g.errorMessage, e.errorMessage).firstOrNull()?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }

        item { Text("Treinos prontos", fontWeight = FontWeight.Bold, fontSize = 18.sp) }
        items(WorkoutTemplates.all, key = { "t" + it.name }) { t ->
            Panel {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(12.dp).clip(CircleShape).background(hexColor(t.colorHex)))
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(t.name, fontWeight = FontWeight.Bold)
                        Text("${t.description} · ${t.exercises.size} exercícios", color = Muted, fontSize = 12.sp)
                    }
                    TextButton(onClick = { groupsVm.createFromTemplate(t) }, enabled = !g.isLoading) { Text("CRIAR", fontWeight = FontWeight.Black) }
                }
            }
        }

        item { Text("Novo grupo", fontWeight = FontWeight.Bold, fontSize = 18.sp) }
        item {
            Panel {
                OutlinedTextField(newName, { newName = it }, label = { Text("Nome (ex.: Peito e tríceps)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GroupColors.forEach { c ->
                        Box(
                            Modifier.size(if (c == newColor) 28.dp else 22.dp).clip(CircleShape).background(hexColor(c))
                                .then(if (c == newColor) Modifier.border(2.dp, Color.White, CircleShape) else Modifier)
                                .clickable { newColor = c }
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = { groupsVm.createMuscleGroup(newName.trim(), newColor); newName = "" },
                    enabled = newName.isNotBlank() && !g.isLoading,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("CRIAR GRUPO", fontWeight = FontWeight.Black) }
            }
        }

        if (g.muscleGroups.isNotEmpty()) item { Text("Seus grupos", fontWeight = FontWeight.Bold, fontSize = 18.sp) }
        items(g.muscleGroups, key = { "g" + (it.id ?: it.name) }) { group ->
            val count = e.exercises.count { group.id in it.groupIds }
            Panel {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(12.dp).clip(CircleShape).background(hexColor(group.colorHex)))
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(group.name, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("$count exercícios", color = Muted, fontSize = 12.sp)
                    }
                    OutlinedButton(onClick = { picking = group }) { Text("+ Exercícios", fontSize = 12.sp) }
                }
            }
        }
    }
}

/** Catálogo com busca e filtro por músculo; marca vários e adiciona de uma vez ao grupo. */
@Composable
private fun CatalogPicker(group: MuscleGroup, onCancel: () -> Unit, onAdd: (List<CatalogExercise>) -> Unit) {
    var query by remember { mutableStateOf("") }
    var muscle by remember { mutableStateOf<CatalogMuscle?>(null) }
    var chosen by remember { mutableStateOf(setOf<String>()) }
    val list = ExerciseCatalog.search(query, muscle)

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.padding(start = 8.dp, end = 20.dp, top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onCancel) { Text("‹ Voltar") }
            Text("Exercícios para ${group.name}", fontWeight = FontWeight.Black, fontSize = 18.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        OutlinedTextField(
            query, { query = it }, label = { Text("Buscar") }, singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)
        )
        LazyRow(contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item { FilterPill("Todos", muscle == null) { muscle = null } }
            items(CatalogMuscle.entries) { m -> FilterPill(m.label, muscle == m) { muscle = if (muscle == m) null else m } }
        }
        LazyColumn(
            Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(list, key = { it.name }) { ex ->
                val on = ex.name in chosen
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                        .background(if (on) Lime.copy(alpha = 0.15f) else Card)
                        .clickable { chosen = if (on) chosen - ex.name else chosen + ex.name }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(ex.name, fontWeight = FontWeight.Bold)
                        Text("${ex.muscle.label} · ${ex.equipment.label} · ${ex.sets}×${ex.reps}", color = Muted, fontSize = 12.sp)
                    }
                    Box(
                        Modifier.size(22.dp).clip(CircleShape)
                            .background(if (on) Lime else Color.Transparent)
                            .border(2.dp, if (on) Lime else Muted, CircleShape)
                    )
                }
            }
        }
        Button(
            onClick = { onAdd(list.filter { it.name in chosen } + ExerciseCatalog.all.filter { it.name in chosen && it !in list }) },
            enabled = chosen.isNotEmpty(),
            modifier = Modifier.fillMaxWidth().padding(20.dp).height(52.dp)
        ) { Text(if (chosen.isEmpty()) "ESCOLHA OS EXERCÍCIOS" else "ADICIONAR ${chosen.size}", fontWeight = FontWeight.Black) }
    }
}

@Composable
private fun FilterPill(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.clip(RoundedCornerShape(16.dp))
            .background(if (selected) Lime else Card)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp)
    ) { Text(label, fontSize = 13.sp, color = if (selected) Color.Black else Color.White) }
}
