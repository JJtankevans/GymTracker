package com.vitoroliveira.gymtracker.domain.repository

import com.vitoroliveira.gymtracker.data.model.NewSharedRoutine
import com.vitoroliveira.gymtracker.data.model.SharedRoutine

/** Compartilhar um grupo de treino por código (migração 011). */
interface RoutineShareRepository {
    /** Grava a cópia do treino e devolve o código (8 caracteres) para mandar aos amigos. */
    suspend fun share(routine: NewSharedRoutine): String

    /** Busca pelo código; null se não existe. [countImport] soma 1 nas importações (quando a pessoa confirma). */
    suspend fun find(code: String, countImport: Boolean = false): SharedRoutine?
}
