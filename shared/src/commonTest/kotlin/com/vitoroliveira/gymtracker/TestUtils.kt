package com.vitoroliveira.gymtracker

/** `replaceAll` para listas em código comum (o do Kotlin/Native ainda é experimental e quebra a compilação no iOS). */
fun <T> MutableList<T>.mapInPlace(transform: (T) -> T) {
    for (i in indices) this[i] = transform(this[i])
}
