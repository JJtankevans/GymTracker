package com.vitoroliveira.gymtracker.presentation.body

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

private class FakeBodyCloud(
    val weights: MutableMap<Int, Double> = mutableMapOf(),
    var profile: BodyProfile? = BodyProfile()
) : BodyCloud {
    var fail = false
    val deletes = mutableListOf<Int>()
    val upserts = mutableListOf<Int>()
    var savedProfile: BodyProfile? = null
    private fun check() { if (fail) throw RuntimeException("Unable to resolve host") }
    override suspend fun fetchWeights(): List<BodyWeightEntry> { check(); return weights.map { (d, kg) -> BodyWeightEntry(d, kg) } }
    override suspend fun upsertWeight(entry: BodyWeightEntry) { check(); upserts += entry.epochDay; weights[entry.epochDay] = roundWeight(entry.kg) }
    override suspend fun deleteWeight(epochDay: Int) { check(); deletes += epochDay; weights.remove(epochDay) }
    override suspend fun fetchProfile(): BodyProfile? { check(); return profile }
    override suspend fun saveProfile(profile: BodyProfile) { check(); savedProfile = profile; this.profile = profile }
}

/** Nuvem que segura a busca dos pesos até [release]: simula a rede lenta durante a troca de conta. */
private class SlowBodyCloud(private val inner: FakeBodyCloud) : BodyCloud by inner {
    val release = CompletableDeferred<Unit>()
    override suspend fun fetchWeights(): List<BodyWeightEntry> { release.await(); return inner.fetchWeights() }
}

/** Nuvem com a tabela de medidas (migração 014); [missingTable] simula o banco sem a migração. */
private class MeasureCloud(private val inner: FakeBodyCloud = FakeBodyCloud()) : BodyCloud by inner {
    val measures = mutableMapOf<String, BodyMeasurement>()
    var missingTable = false
    val deleted = mutableListOf<String>()
    private fun check() { if (missingTable) throw RuntimeException("relation \"body_measurements\" does not exist") }
    override suspend fun fetchMeasurements(): List<BodyMeasurement> { check(); return measures.values.toList() }
    override suspend fun upsertMeasurement(m: BodyMeasurement) { check(); measures[measureKey(m.epochDay, m.site)] = m }
    override suspend fun deleteMeasurement(epochDay: Int, site: MeasureSite) { check(); deleted += measureKey(epochDay, site); measures.remove(measureKey(epochDay, site)) }
}

class BodySyncTest {
    @Test fun medidasSobemDescemENaoRessuscitam() = runTest {
        val cloud = MeasureCloud()
        cloud.measures[measureKey(5, MeasureSite.ARM)] = BodyMeasurement(5, MeasureSite.ARM, 38.0)      // de outro celular
        cloud.measures[measureKey(6, MeasureSite.CALF)] = BodyMeasurement(6, MeasureSite.CALF, 40.0)
        val repo = BodyRepository(InMemoryBodyStore(BodyData(measurements = listOf(BodyMeasurement(7, MeasureSite.WAIST, 84.0)))), cloud)
        repo.deleteMeasurement(6, MeasureSite.CALF)          // apagada aqui antes de sincronizar

        repo.sync()

        assertEquals(listOf(5 to MeasureSite.ARM, 7 to MeasureSite.WAIST), repo.data.value.measurements.map { it.epochDay to it.site })
        assertTrue(measureKey(7, MeasureSite.WAIST) in cloud.measures)      // a local subiu
        assertEquals(listOf(measureKey(6, MeasureSite.CALF)), cloud.deleted) // a apagada foi apagada lá
        assertTrue(repo.data.value.pendingMeasureDeletes.isEmpty())
    }

    @Test fun semATabelaDeMedidasOPesoSincronizaMesmoAssim() = runTest {
        val cloud = MeasureCloud().apply { missingTable = true }
        val repo = BodyRepository(
            InMemoryBodyStore(BodyData(weights = listOf(BodyWeightEntry(9, 70.0)), measurements = listOf(BodyMeasurement(9, MeasureSite.HIPS, 99.0)))),
            cloud
        )
        repo.deleteMeasurement(1, MeasureSite.ARM)
        repo.sync()
        assertEquals(listOf(9), repo.data.value.weights.map { it.epochDay })
        assertEquals(1, repo.data.value.measurements.size)                  // a local continua
        assertEquals(setOf(measureKey(1, MeasureSite.ARM)), repo.data.value.pendingMeasureDeletes) // fica para depois
    }

    @Test fun sairDaContaApagaOsDadosDoAparelhoEAProximaContaSoVeOsDela() = runTest {
        val store = InMemoryBodyStore(
            BodyData(
                profile = BodyProfile(heightCm = 180),
                weights = listOf(BodyWeightEntry(20, 80.0)),
                measurements = listOf(BodyMeasurement(20, MeasureSite.WAIST, 85.0))
            )
        )
        val cloud = FakeBodyCloud(weights = mutableMapOf(20 to 80.0))
        val repo = BodyRepository(store, cloud)
        repo.sync()

        repo.clear()
        assertEquals(BodyData(), repo.data.value)
        assertEquals(BodyData(), store.load())

        // outra conta entra: a nuvem dela tem só o dia 30 e nada do aparelho é enviado para ela
        cloud.weights.clear(); cloud.weights[30] = 70.0; cloud.upserts.clear(); cloud.savedProfile = null; cloud.profile = BodyProfile(heightCm = 165)
        repo.sync()
        assertEquals(listOf(30), repo.data.value.weights.map { it.epochDay })
        assertEquals(165, repo.data.value.profile.heightCm)
        assertTrue(cloud.upserts.isEmpty())
        assertEquals(null, cloud.savedProfile)
    }

    @Test fun sincronizacaoQueComecouAntesDeSairNaoGravaNadaDepois() = runTest {
        val inner = FakeBodyCloud()
        val cloud = SlowBodyCloud(inner)
        val repo = BodyRepository(InMemoryBodyStore(BodyData(weights = listOf(BodyWeightEntry(20, 80.0)))), cloud)
        val job = launch { repo.sync() }
        runCurrent()            // a sincronização está esperando a nuvem
        repo.clear()            // o usuário saiu da conta
        cloud.release.complete(Unit)
        job.join()
        assertTrue(inner.upserts.isEmpty(), "o peso da conta anterior foi enviado: ${inner.upserts}")
        assertEquals(BodyData(), repo.data.value)
    }

    @Test fun mandaOQueSoExisteNoAparelhoETrazOQueSoExisteNaNuvem() = runTest {
        val cloud = FakeBodyCloud(weights = mutableMapOf(10 to 81.0))
        val repo = BodyRepository(InMemoryBodyStore(BodyData(weights = listOf(BodyWeightEntry(20, 80.0)))), cloud)
        repo.sync()
        assertEquals(listOf(10, 20), repo.data.value.weights.map { it.epochDay })
        assertEquals(listOf(20), cloud.upserts)
        assertEquals(80.0, cloud.weights[20])
    }

    @Test fun mesmoDiaComValorDiferenteOAparelhoVence() = runTest {
        val cloud = FakeBodyCloud(weights = mutableMapOf(20 to 90.0))
        val repo = BodyRepository(InMemoryBodyStore(BodyData(weights = listOf(BodyWeightEntry(20, 80.0)))), cloud)
        repo.sync()
        assertEquals(80.0, repo.data.value.weights.single().kg)
        assertEquals(80.0, cloud.weights[20])
    }

    @Test fun igualNaNuvemNaoReenvia() = runTest {
        val cloud = FakeBodyCloud(weights = mutableMapOf(20 to 80.0))
        val repo = BodyRepository(InMemoryBodyStore(BodyData(weights = listOf(BodyWeightEntry(20, 80.0)))), cloud)
        repo.sync()
        assertTrue(cloud.upserts.isEmpty())
    }

    @Test fun apagadoNoAparelhoNaoVoltaEEApagadoNaNuvem() = runTest {
        val cloud = FakeBodyCloud(weights = mutableMapOf(10 to 81.0, 20 to 80.0))
        val repo = BodyRepository(InMemoryBodyStore(BodyData(weights = listOf(BodyWeightEntry(10, 81.0), BodyWeightEntry(20, 80.0)))), cloud)
        repo.deleteWeight(10)
        repo.sync()
        assertEquals(listOf(20), repo.data.value.weights.map { it.epochDay })
        assertEquals(listOf(10), cloud.deletes)
        assertTrue(repo.data.value.pendingDeletes.isEmpty())
    }

    @Test fun semInternetNadaSePerdeEOApagadoContinuaPendente() = runTest {
        val cloud = FakeBodyCloud().apply { fail = true }
        val repo = BodyRepository(InMemoryBodyStore(BodyData(weights = listOf(BodyWeightEntry(10, 81.0)))), cloud)
        repo.deleteWeight(10)
        repo.logWeight(11, 80.5)
        assertFailsWith<RuntimeException> { repo.sync() }
        assertEquals(listOf(11), repo.data.value.weights.map { it.epochDay })
        assertEquals(setOf(10), repo.data.value.pendingDeletes)
    }

    @Test fun registrarDeNovoUmDiaApagadoTiraDosPendentes() {
        val repo = BodyRepository(InMemoryBodyStore())
        repo.logWeight(10, 80.0)
        repo.deleteWeight(10)
        repo.logWeight(10, 79.0)
        assertTrue(repo.data.value.pendingDeletes.isEmpty())
    }

    @Test fun celularNovoTrazOPerfilDaNuvem() = runTest {
        val remote = BodyProfile(heightCm = 178, birthYear = 1995, sex = Sex.MALE)
        val cloud = FakeBodyCloud(profile = remote)
        val repo = BodyRepository(InMemoryBodyStore(), cloud)
        repo.sync()
        assertEquals(remote, repo.data.value.profile)
    }

    @Test fun perfilPreenchidoNoAparelhoEEnviado() = runTest {
        val local = BodyProfile(heightCm = 170)
        val cloud = FakeBodyCloud(profile = BodyProfile(heightCm = 150))
        val repo = BodyRepository(InMemoryBodyStore(BodyData(profile = local)), cloud)
        repo.sync()
        assertEquals(local, cloud.savedProfile)
        assertEquals(local, repo.data.value.profile)
    }

    @Test fun sincronizaUmaVezSoPorAbertura() = runTest {
        val cloud = FakeBodyCloud(weights = mutableMapOf(10 to 81.0))
        val repo = BodyRepository(InMemoryBodyStore(), cloud)
        repo.sync()
        cloud.weights[11] = 80.0
        repo.sync()
        assertEquals(1, repo.data.value.weights.size)
        repo.sync(force = true)
        assertEquals(2, repo.data.value.weights.size)
    }

    @Test fun pesoArredondadoComoNoBanco() {
        assertEquals(80.3, roundWeight(80.26))
        assertEquals(80.0, roundWeight(80.04))
    }
}
