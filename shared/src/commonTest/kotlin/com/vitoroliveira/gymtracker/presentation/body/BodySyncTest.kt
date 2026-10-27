package com.vitoroliveira.gymtracker.presentation.body

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

class BodySyncTest {
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
