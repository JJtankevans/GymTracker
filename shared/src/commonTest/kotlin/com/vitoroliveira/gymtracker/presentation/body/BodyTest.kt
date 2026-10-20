package com.vitoroliveira.gymtracker.presentation.body

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BodyTest {
    @Test fun bmi() {
        assertEquals(BmiCategory.NORMAL, Bmi.category(Bmi.value(70.0, 175)!!))
        assertEquals(BmiCategory.OVER, Bmi.category(Bmi.value(90.0, 175)!!))
        assertNull(Bmi.value(70.0, 50))
        val r = Bmi.normalRangeKg(180)!!
        assertTrue(r.start in 59.0..60.5 && r.endInclusive in 80.0..81.0)
    }

    @Test fun liftRecognition() {
        assertEquals(Lift.BENCH, StrengthStandards.liftFor("Supino reto"))
        assertEquals(Lift.SQUAT, StrengthStandards.liftFor("Agachamento livre"))
        assertEquals(Lift.DEADLIFT, StrengthStandards.liftFor("Levantamento terra"))
        assertEquals(Lift.OVERHEAD, StrengthStandards.liftFor("Desenvolvimento militar"))
        assertNull(StrengthStandards.liftFor("Supino inclinado com halteres"))
        assertNull(StrengthStandards.liftFor("Agachamento búlgaro"))
        assertNull(StrengthStandards.liftFor("Rosca direta"))
    }

    @Test fun levels() {
        // homem de 80 kg com supino 1RM de 100 kg => 1.25x => entre Intermediário (1.13) e Avançado (1.36)
        assertEquals(StrengthLevel.INTERMEDIATE, StrengthStandards.levelFor(Lift.BENCH, 100.0, 80.0, Sex.MALE))
        assertNull(StrengthStandards.levelFor(Lift.BENCH, 20.0, 80.0, Sex.MALE))
        assertEquals(StrengthLevel.ADVANCED, StrengthStandards.nextLevel(StrengthLevel.INTERMEDIATE))
        assertNull(StrengthStandards.nextLevel(StrengthLevel.ELITE))
    }

    @Test fun epleyRoundTrip() {
        val w = 80.0
        assertEquals(w, loadForReps(estimateOneRepMax(w, 8), 8), 1e-9)
        assertEquals(100.0, estimateOneRepMax(100.0, 1), 1e-9)
    }

    @Test fun progressionUpWhenTargetHit() {
        val s = LoadSuggester.suggest("Rosca", 40.0, 12, 12, null, null, null)!!
        assertTrue(s.weightKg > 40.0)
    }

    @Test fun progressionAlwaysMovesAtLeastOneStep() {
        // 2.5% de 20 kg seria 0,5 kg: arredondado volta a 20, então sobe um degrau inteiro
        assertEquals(22.5, LoadSuggester.suggest("Rosca", 20.0, 12, 12, null, null, null)!!.weightKg, 1e-9)
    }

    @Test fun keepsInTheMiddle() {
        assertEquals(40.0, LoadSuggester.suggest("Rosca", 40.0, 10, 12, null, null, null)!!.weightKg, 1e-9)
    }

    @Test fun dropsWhenFarBelow() {
        val s = LoadSuggester.suggest("Rosca", 40.0, 6, 12, null, null, null)!!
        assertTrue(s.weightKg < 40.0)
    }

    @Test fun firstTimeUsesBodyweightForBigLifts() {
        val s = LoadSuggester.suggest("Supino reto", null, null, 10, 80.0, Sex.MALE, 30)!!
        assertTrue(s.weightKg in 20.0..50.0, "got ${s.weightKg}")
        // mais velho => mais conservador
        val old = LoadSuggester.suggest("Supino reto", null, null, 10, 80.0, Sex.MALE, 60)!!
        assertTrue(old.weightKg <= s.weightKg)
    }

    @Test fun firstTimeWithoutDataGivesNothing() {
        assertNull(LoadSuggester.suggest("Rosca direta", null, null, 10, 80.0, Sex.MALE, 30))
        assertNull(LoadSuggester.suggest("Supino reto", null, null, 10, null, null, null))
    }

    @Test fun viewModelKeepsOneEntryPerDay() {
        val vm = BodyViewModel(BodyRepository(InMemoryBodyStore()))
        vm.logWeight(100, 80.0); vm.logWeight(100, 79.5); vm.logWeight(101, 79.0); vm.logWeight(102, 5.0)
        assertEquals(2, vm.data.value.weights.size)
        assertEquals(79.5, vm.data.value.weights.first().kg, 1e-9)
        assertNotNull(vm.data.value.latestKg)
        assertEquals(79.0, vm.data.value.latestKg!!, 1e-9)
    }
}

class LiftSeriesTest {
    private fun p(date: String, kg: Double, reps: Int, sets: Int) =
        com.vitoroliveira.gymtracker.data.model.ExerciseProgression("e", "Supino reto", "u", date, kg, reps, sets)

    @Test fun estimatesFromAverageReps() {
        val s = buildLiftSeries("Supino reto", Lift.BENCH, listOf(p("2026-01-01", 60.0, 30, 3), p("2026-01-08", 65.0, 27, 3)))!!
        assertEquals(60.0 * (1 + 10 / 30.0), s.oneRepMaxes[0], 1e-9)
        assertEquals(65.0 * (1 + 9 / 30.0), s.bestOneRm, 1e-9)
        assertNull(buildLiftSeries("x", Lift.BENCH, emptyList()))
    }
}
