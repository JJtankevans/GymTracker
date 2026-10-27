package com.vitoroliveira.gymtracker.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Um exercício dentro de um treino compartilhado (cópia: nome, séries, reps e observação). */
@Serializable
data class SharedExercise(
    val name: String,
    val sets: Int = 3,
    val reps: Int = 12,
    val notes: String? = null
)

/** Linha de `get_shared_routine()` (migração 011): o treino que alguém compartilhou por código. */
@Serializable
data class SharedRoutine(
    val code: String,
    val name: String,
    @SerialName("color_hex") val colorHex: String = "#6200EE",
    val exercises: List<SharedExercise> = emptyList(),
    @SerialName("owner_username") val ownerUsername: String? = null,
    val imports: Int = 0,
    @SerialName("created_at") val createdAt: String? = null
)

/** O que vai para `shared_routines` ao compartilhar (o banco gera o código e o dono). */
@Serializable
data class NewSharedRoutine(
    val name: String,
    @SerialName("color_hex") val colorHex: String,
    val exercises: List<SharedExercise>
)
