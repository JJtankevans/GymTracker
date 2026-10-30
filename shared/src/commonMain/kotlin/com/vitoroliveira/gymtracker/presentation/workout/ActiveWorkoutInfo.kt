package com.vitoroliveira.gymtracker.presentation.workout

/** Treino que foi iniciado e ainda não terminou, para a Home avisar e deixar continuar ou descartar. */
data class ActiveWorkoutInfo(
    val workoutId: String,
    val groupId: String?,
    /** "Peito", ou "Treino livre" sem grupo. */
    val groupName: String,
    /** Minutos desde o início, na hora em que a Home carregou. */
    val minutes: Long
)

/** "agora mesmo", "12 min", "1 h 05 min" (usa [formatMinutes]), "1 dia", "3 dias". */
fun formatElapsedSince(minutes: Long): String = when {
    minutes < 1 -> "agora mesmo"
    minutes < 24 * 60 -> formatMinutes(minutes)
    else -> (minutes / (24 * 60)).let { if (it == 1L) "1 dia" else "$it dias" }
}
