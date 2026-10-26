package com.vitoroliveira.gymtracker.presentation.body

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MeasurementsTest {
    @Test fun umaMedidaPorDiaELocal() {
        val repo = BodyRepository(InMemoryBodyStore())
        repo.logMeasurement(100, MeasureSite.WAIST, 85.0)
        repo.logMeasurement(100, MeasureSite.WAIST, 84.5)
        repo.logMeasurement(100, MeasureSite.ARM, 36.0)
        repo.logMeasurement(100, MeasureSite.ARM, 5.0) // fora da faixa: ignorado
        assertEquals(2, repo.data.value.measurements.size)
        assertEquals(84.5, repo.data.value.measurements.first { it.site == MeasureSite.WAIST }.cm)
    }

    @Test fun resumoMostraUltimoValorEVariacao() {
        val list = listOf(
            BodyMeasurement(10, MeasureSite.WAIST, 88.0), BodyMeasurement(40, MeasureSite.WAIST, 84.6),
            BodyMeasurement(20, MeasureSite.ARM, 35.0)
        )
        val s = summarizeMeasurements(list)
        assertEquals(listOf(MeasureSite.WAIST, MeasureSite.ARM), s.map { it.site })
        assertEquals(84.6, s[0].latestCm)
        assertEquals(-3.4, s[0].changeCm)
        assertEquals(0.0, s[1].changeCm)
    }

    @Test fun apagarTiraSoAqueleDiaELocal() {
        val repo = BodyRepository(InMemoryBodyStore())
        repo.logMeasurement(100, MeasureSite.WAIST, 85.0)
        repo.logMeasurement(101, MeasureSite.WAIST, 84.0)
        repo.deleteMeasurement(100, MeasureSite.WAIST)
        assertEquals(listOf(101), repo.data.value.measurements.map { it.epochDay })
        assertTrue(summarizeMeasurements(emptyList()).isEmpty())
    }
}
