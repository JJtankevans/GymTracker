package com.vitoroliveira.gymtracker.ui.utils

fun formatWeightInput(input: String): String {
    // 1. Converte ponto em vírgula e mantém apenas dígitos e vírgula
    var filtered = input.replace('.', ',').filter { it.isDigit() || it == ',' }

    // 2. Garante no máximo uma vírgula
    val firstComma = filtered.indexOf(',')
    if (firstComma != -1) {
        filtered = filtered.substring(0, firstComma + 1) +
                filtered.substring(firstComma + 1).replace(",", "")
    }

    // 3. Limita a 2 casas decimais
    val parts = filtered.split(",")
    if (parts.size == 2 && parts[1].length > 2) {
        filtered = "${parts[0]},${parts[1].take(2)}"
    }

    return filtered
}