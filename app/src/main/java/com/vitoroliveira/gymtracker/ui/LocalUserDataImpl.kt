package com.vitoroliveira.gymtracker.ui

import android.content.Context
import com.vitoroliveira.gymtracker.domain.repository.LocalUserData
import com.vitoroliveira.gymtracker.presentation.body.BodyRepository
import com.vitoroliveira.gymtracker.presentation.circuit.TimingStore
import com.vitoroliveira.gymtracker.presentation.schedule.ScheduleRepository
import com.vitoroliveira.gymtracker.ui.exercise.CatalogPrefs
import java.io.File

/**
 * Tudo o que o app guarda no aparelho e é do usuário logado. Ficam de fora as preferências do aparelho (tema,
 * intensidade) e o cache de fotos de perfil (são públicas).
 */
class LocalUserDataImpl(
    context: Context,
    private val body: BodyRepository,
    private val schedule: ScheduleRepository,
    private val catalog: CatalogPrefs,
    private val timing: TimingStore
) : LocalUserData {
    private val appContext = context.applicationContext
    private val cacheDir = appContext.cacheDir

    /** Peso, perfil corporal e medidas pendentes vão para a nuvem antes de sair (medidas: migração 014). */
    override suspend fun flush() = body.sync(force = true)

    override fun clear() {
        body.clear()
        schedule.clear()
        catalog.clear()
        timing.save(emptyMap())
        com.vitoroliveira.gymtracker.ui.social.SocialAlertsWorker.clearSeen(appContext)
        // CSV exportado tem o histórico inteiro
        File(cacheDir, "exports").deleteRecursively()
    }
}
