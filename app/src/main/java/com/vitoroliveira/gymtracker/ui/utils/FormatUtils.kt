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

/** 80.0 → "80", 82.5 → "82,5" (vírgula, como no teclado do app). */
fun formatKg(value: Double): String {
    val rounded = kotlin.math.round(value * 100) / 100.0
    return if (rounded % 1.0 == 0.0) rounded.toLong().toString()
    else rounded.toString().trimEnd('0').replace('.', ',')
}

/** 3725 → "1:02:05", 125 → "02:05". */
fun formatElapsed(totalSeconds: Long): String {
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    val s = totalSeconds % 60
    val mm = m.toString().padStart(2, '0')
    val ss = s.toString().padStart(2, '0')
    return if (h > 0) "$h:$mm:$ss" else "$mm:$ss"
}

/** 1840.0 → "1.840 kg" ; >= 10 t → "12,4 t". */
fun formatVolume(kg: Double): String {
    if (kg >= 10_000) return "${formatKg(kotlin.math.round(kg / 100) / 10.0)} t"
    val whole = kotlin.math.round(kg).toLong().toString()
    val grouped = whole.reversed().chunked(3).joinToString(".").reversed()
    return "$grouped kg"
}
