package com.vitoroliveira.gymtracker.presentation.body

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

/**
 * Fonte única dos dados corporais (um só objeto no app, para todas as telas verem o mesmo valor).
 * O aparelho é a fonte principal; [cloud] (Supabase) é uma cópia para não perder os dados ao trocar de celular.
 * Sem internet tudo continua funcionando localmente e é enviado na próxima sincronização.
 */
class BodyRepository(private val store: BodyStore, private val cloud: BodyCloud? = null) {
    private val _data = MutableStateFlow(store.load())
    val data: StateFlow<BodyData> = _data.asStateFlow()
    private var synced = false
    /** Muda a cada [clear]: uma sincronização que começou antes (da conta anterior) para de gravar e é descartada. */
    private var generation = 0

    /** Há cópia na nuvem configurada (no app sempre há; nos testes sem nuvem nada é disparado). */
    val hasCloud: Boolean get() = cloud != null

    private fun change(f: (BodyData) -> BodyData) {
        _data.update(f)
        store.save(_data.value)
    }

    fun setProfile(p: BodyProfile) = change { it.copy(profile = p) }

    /** Um registro por dia: se já existe, substitui. Valores fora de 20–400 kg são ignorados. */
    fun logWeight(epochDay: Int, kg: Double) {
        if (kg !in 20.0..400.0) return
        change { d ->
            d.copy(
                weights = (d.weights.filter { it.epochDay != epochDay } + BodyWeightEntry(epochDay, kg)).sortedBy { it.epochDay },
                pendingDeletes = d.pendingDeletes - epochDay
            )
        }
    }

    /** Uma medida por dia e local; valores fora de 10–300 cm são ignorados. */
    fun logMeasurement(epochDay: Int, site: MeasureSite, cm: Double) {
        if (cm !in 10.0..300.0) return
        change { d ->
            d.copy(
                measurements = (d.measurements.filterNot { it.epochDay == epochDay && it.site == site } + BodyMeasurement(epochDay, site, cm))
                    .sortedWith(compareBy({ it.epochDay }, { it.site.ordinal })),
                pendingMeasureDeletes = d.pendingMeasureDeletes - measureKey(epochDay, site)
            )
        }
    }

    fun deleteMeasurement(epochDay: Int, site: MeasureSite) =
        change { d ->
            d.copy(
                measurements = d.measurements.filterNot { it.epochDay == epochDay && it.site == site },
                pendingMeasureDeletes = d.pendingMeasureDeletes + measureKey(epochDay, site)
            )
        }

    fun deleteWeight(epochDay: Int) =
        change { d -> d.copy(weights = d.weights.filter { it.epochDay != epochDay }, pendingDeletes = d.pendingDeletes + epochDay) }

    /**
     * Junta aparelho e nuvem (uma vez por abertura do app, ou sempre com [force]). Lança a exceção se a rede falhar:
     * quem chama decide se mostra ou ignora.
     */
    suspend fun sync(force: Boolean = false) {
        val c = cloud ?: return
        if (synced && !force) return
        val gen = generation
        val start = _data.value
        // cada ida à nuvem confere se ninguém saiu da conta no meio: senão os dados da conta anterior iriam para a nova
        start.pendingDeletes.forEach { if (gen != generation) return; c.deleteWeight(it) }
        if (gen != generation) return
        val remote = c.fetchWeights()
        val remoteKg = remote.associate { it.epochDay to it.kg }
        start.weights.filter { remoteKg[it.epochDay] != roundWeight(it.kg) }.forEach { if (gen != generation) return; c.upsertWeight(it) }
        if (gen != generation) return
        val profile = if (start.profile == BodyProfile()) c.fetchProfile() else { c.saveProfile(start.profile); null }
        if (gen != generation) return
        // medidas: opcionais (sem a migração 014 a tabela não existe e só elas ficam de fora)
        val remoteMeasures = try {
            start.pendingMeasureDeletes.forEach { key ->
                if (gen != generation) return
                val (day, site) = key.split('|').let { it[0].toInt() to MeasureSite.valueOf(it[1]) }
                c.deleteMeasurement(day, site)
            }
            val remote = c.fetchMeasurements()
            val remoteCm = remote.associate { measureKey(it.epochDay, it.site) to it.cm }
            start.measurements.filter { remoteCm[measureKey(it.epochDay, it.site)] != roundWeight(it.cm) }
                .forEach { if (gen != generation) return; c.upsertMeasurement(it) }
            remote
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            null
        }
        if (gen != generation) return
        // junta com o estado de AGORA (o usuário pode ter mexido durante a sincronização)
        change { cur ->
            val stillPending = cur.pendingDeletes - start.pendingDeletes
            val measurePending = if (remoteMeasures != null) cur.pendingMeasureDeletes - start.pendingMeasureDeletes else cur.pendingMeasureDeletes
            cur.copy(
                weights = mergeWeights(cur.weights, remote, cur.pendingDeletes),
                pendingDeletes = stillPending,
                measurements = remoteMeasures?.let { mergeMeasurements(cur.measurements, it, cur.pendingMeasureDeletes) } ?: cur.measurements,
                pendingMeasureDeletes = measurePending,
                profile = if (cur.profile == BodyProfile() && profile != null) profile else cur.profile
            )
        }
        synced = true
    }

    /**
     * Apaga os dados corporais do aparelho (ao sair da conta). Pesos, perfil corporal e medidas (migração 014) têm cópia
     * na nuvem e voltam na próxima sincronização da mesma conta.
     */
    fun clear() {
        generation++
        synced = false
        _data.value = BodyData()
        store.save(BodyData())
    }

    /** Envia um peso já gravado no aparelho. */
    suspend fun pushWeight(epochDay: Int) {
        val c = cloud ?: return
        _data.value.weights.firstOrNull { it.epochDay == epochDay }?.let { c.upsertWeight(it) }
    }

    /** Apaga na nuvem um peso já apagado no aparelho e tira da lista de pendentes. */
    suspend fun pushDelete(epochDay: Int) {
        val c = cloud ?: return
        c.deleteWeight(epochDay)
        change { it.copy(pendingDeletes = it.pendingDeletes - epochDay) }
    }

    suspend fun pushProfile() {
        cloud?.saveProfile(_data.value.profile)
    }

    /** Envia uma medida já gravada no aparelho. */
    suspend fun pushMeasurement(epochDay: Int, site: MeasureSite) {
        val c = cloud ?: return
        _data.value.measurements.firstOrNull { it.epochDay == epochDay && it.site == site }?.let { c.upsertMeasurement(it) }
    }

    /** Apaga na nuvem uma medida já apagada no aparelho e tira da lista de pendentes. */
    suspend fun pushMeasurementDelete(epochDay: Int, site: MeasureSite) {
        val c = cloud ?: return
        c.deleteMeasurement(epochDay, site)
        change { it.copy(pendingMeasureDeletes = it.pendingMeasureDeletes - measureKey(epochDay, site)) }
    }
}

class BodyViewModel(private val repo: BodyRepository) : ViewModel() {
    val data: StateFlow<BodyData> = repo.data

    init { quietly { repo.sync() } }

    fun setProfile(p: BodyProfile) { repo.setProfile(p); quietly { repo.pushProfile() } }
    fun logWeight(epochDay: Int, kg: Double) { repo.logWeight(epochDay, kg); quietly { repo.pushWeight(epochDay) } }
    fun deleteWeight(epochDay: Int) { repo.deleteWeight(epochDay); quietly { repo.pushDelete(epochDay) } }
    fun logMeasurement(epochDay: Int, site: MeasureSite, cm: Double) { repo.logMeasurement(epochDay, site, cm); quietly { repo.pushMeasurement(epochDay, site) } }
    fun deleteMeasurement(epochDay: Int, site: MeasureSite) { repo.deleteMeasurement(epochDay, site); quietly { repo.pushMeasurementDelete(epochDay, site) } }

    /** A nuvem é cópia: se falhar (sem internet, migração 003 ausente), fica para a próxima sincronização, sem erro na tela. */
    private fun quietly(block: suspend () -> Unit) {
        if (!repo.hasCloud) return
        viewModelScope.launch {
            try { block() } catch (e: Exception) { if (e is CancellationException) throw e }
        }
    }
}
