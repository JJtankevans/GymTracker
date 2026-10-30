package com.vitoroliveira.gymtracker.presentation.viewmodel

/** Nomes de grupo que aparecem no histórico, do mais treinado ao menos (empate: ordem alfabética). */
fun historyGroupNames(items: List<WorkoutHistoryItem>): List<String> =
    items.groupingBy { it.muscleGroupName }.eachCount().entries
        .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
        .map { it.key }

/** Só os treinos do grupo [group]; null (ou um grupo que não existe mais) mostra todos. */
fun filterHistory(items: List<WorkoutHistoryItem>, group: String?): List<WorkoutHistoryItem> =
    if (group == null || items.none { it.muscleGroupName == group }) items else items.filter { it.muscleGroupName == group }
