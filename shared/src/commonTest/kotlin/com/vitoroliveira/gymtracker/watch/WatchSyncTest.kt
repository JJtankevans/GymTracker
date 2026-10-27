package com.vitoroliveira.gymtracker.watch

import com.vitoroliveira.gymtracker.data.model.Exercise
import com.vitoroliveira.gymtracker.data.model.MuscleGroup
import com.vitoroliveira.gymtracker.data.model.RecentSet
import com.vitoroliveira.gymtracker.data.model.RecordSummary
import com.vitoroliveira.gymtracker.data.model.WorkoutDate
import com.vitoroliveira.gymtracker.domain.usecase.ImportWatchWorkout
import com.vitoroliveira.gymtracker.presentation.viewmodel.FakeWorkoutRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WatchSyncTest {

    private val supino = WatchExercise("e1", "Supino", sets = 2, reps = 10, lastKg = 40.0, lastReps = 10, bestKg = 45.0)
    private val crucifixo = WatchExercise("e2", "Crucifixo", sets = 1, reps = 12)
    private val routine = WatchRoutine("g1", "Peito", "#FF0000", listOf(supino, crucifixo))

    @Test
    fun routinesPerGroupWithLastLoadAndRecord() {
        val groups = listOf(MuscleGroup("g2", name = "Costas"), MuscleGroup("g1", name = "peito"))
        val exercises = listOf(
            Exercise("e1", name = "Supino", defaultSets = 4, defaultReps = 8, groupIds = listOf("g1")),
            Exercise("e2", name = "Remada", groupIds = listOf("g2")),
            Exercise("e3", name = "Abdominal")
        )
        val recent = listOf(
            RecentSet("e1", 10, 40.0, WorkoutDate("2026-10-01T10:00:00+00:00")),
            RecentSet("e1", 6, 50.0, WorkoutDate("2026-10-01T10:00:00+00:00")),
            RecentSet("e1", 8, 45.0, WorkoutDate("2026-10-20T10:00:00+00:00")),
            RecentSet("e1", 12, 45.0, WorkoutDate("2026-10-20T10:00:00+00:00")),
            RecentSet("e1", 5, 30.0, WorkoutDate("2026-10-20T10:00:00+00:00"))
        )
        val records = listOf(RecordSummary("e1", "Supino", 50.0, "2026-10-01", 60.0, null, 2))

        val r = buildWatchRoutines(groups, exercises, recent, records, nowMs = 123L)

        assertEquals(listOf("Costas", "peito", "Todos os exercícios"), r.routines.map { it.name })
        val sup = r.routines[1].exercises.single()
        assertEquals(45.0, sup.lastKg)      // treino mais recente, não o recorde antigo
        assertEquals(12, sup.lastReps)      // a série com mais repetições na carga máxima
        assertEquals(50.0, sup.bestKg)
        assertEquals(4, sup.sets)
        assertNull(r.routines.last().exercises.first { it.id == "e3" }.lastKg)
        assertNull(r.routines.last().groupId)
        assertEquals(123L, r.generatedAt)
    }

    @Test
    fun groupWithoutExercisesIsHidden() {
        val r = buildWatchRoutines(listOf(MuscleGroup("g1", name = "Vazio")), emptyList(), emptyList(), emptyList(), 0L)
        assertTrue(r.routines.isEmpty())
    }

    @Test
    fun jsonRoundTripIgnoresUnknownFields() {
        val r = WatchRoutines(true, 5L, listOf(routine))
        assertEquals(r, WatchJson.decodeRoutines(WatchJson.encodeRoutines(r)))
        assertNotNull(WatchJson.decodeRoutines("""{"loggedIn":false,"campoNovo":1}"""))
        assertNull(WatchJson.decodeRoutines("lixo"))
    }

    @Test
    fun sessionSuggestsLastLoadThenPreviousSetAndAdvances() {
        var s = WatchSession.start("w1", routine, nowMs = 1_000L)
        assertEquals(10 to 40.0, s.suggestion(s.current))

        s = s.addSet(8, 42.5, 2_000L).session
        assertEquals(8 to 42.5, s.suggestion(s.current))
        assertEquals("e1", s.current.id)

        val second = s.addSet(8, 42.5, 3_000L)
        assertTrue(second.finishedExercise)
        assertEquals("e2", second.session.current.id)   // passou sozinho para o próximo
        assertEquals(12 to 0.0, second.session.suggestion(second.session.current))

        val last = second.session.addSet(12, 10.0, 4_000L).session
        assertTrue(last.allDone)
        assertEquals(2, last.doneExercises)
        assertEquals(8 * 42.5 * 2 + 120, last.volumeKg)
    }

    @Test
    fun recordOnlyAboveExistingBest() {
        val s = WatchSession.start("w1", routine, 0L)
        assertFalse(s.addSet(10, 45.0, 1L).newRecord)
        val r = s.addSet(5, 47.5, 1L)
        assertTrue(r.newRecord)
        assertFalse(r.session.addSet(5, 47.5, 2L).newRecord)          // igualar o recorde deste treino não conta
        assertEquals(1, r.session.records)
        // exercício nunca feito: a primeira série não é "recorde"
        assertFalse(s.select(1).addSet(12, 10.0, 1L).newRecord)
    }

    @Test
    fun undoReturnsFocusToTheExerciseOfTheLastSet() {
        var s = WatchSession.start("w1", routine, 0L)
        s = s.addSet(10, 40.0, 1L).session.addSet(10, 40.0, 2L).session
        assertEquals(1, s.currentIndex)
        s = s.undoLastSet()
        assertEquals(0, s.currentIndex)
        assertEquals(1, s.setsDone("e1"))
    }

    @Test
    fun heartRateStatsIgnoreNoise() {
        val s = WatchSession.start("w1", routine, 0L).withHeartRate(100).withHeartRate(140).withHeartRate(0).withHeartRate(300)
        assertEquals(120, s.avgHeartRate)
        assertEquals(140, s.maxHeartRate)
    }

    @Test
    fun quickWorkoutIsNotSentToThePhone() {
        val q = WatchSession.quick("q", 10, 20.0, 0L)
        assertTrue(q.isQuick)
        val done = q.addSet(10, 20.0, 1L).session.addSet(10, 20.0, 2L).session
        assertEquals(2, done.totalSets)
        assertFalse(done.allDone)
        assertNull(done.toWorkout(5L))
    }

    @Test
    fun outboxAndSessionSurviveJson() {
        val s = WatchSession.start("w1", routine, 0L).addSet(10, 40.0, 1L).session
        assertEquals(s, WatchJson.decodeSession(WatchJson.encodeSession(s)))
        val w = assertNotNull(s.toWorkout(10L))
        val box = WatchOutbox().add(w).add(w).markPosted("w1")
        assertEquals(1, box.items.size)
        assertTrue(box.items.single().posted)
        assertEquals(box, WatchJson.decodeOutbox(WatchJson.encodeOutbox(box)))
        assertTrue(box.remove("w1").items.isEmpty())
    }

    @Test
    fun formatting() {
        assertEquals("40", formatKg(40.0))
        assertEquals("42,5", formatKg(42.5))
        assertEquals("850 kg", formatVolume(850.0))
        assertEquals("3,3 t", formatVolume(3280.0))
        assertEquals(42.5, progressionHint(supino))
        assertEquals(null, progressionHint(supino.copy(lastReps = 8)))
        assertEquals(55.0, progressionHint(supino.copy(lastKg = 50.0)))
    }

    @Test
    fun importCreatesSetsThenFinishesAndIsIdempotent() = runTest {
        val repo = FakeWorkoutRepository()
        val import = ImportWatchWorkout(repo)
        val w = WatchWorkout(
            "uuid-1", "g1", "Peito", startedAt = 1_761_000_000_000, finishedAt = 1_761_003_600_000,
            sets = listOf(
                WatchSet("e1", 1, 10, 40.0, 1_761_000_100_000),
                WatchSet("apagado", 1, 10, 40.0, 1_761_000_200_000),
                WatchSet("e1", 2, 8, 42.5, 1_761_000_300_000)
            )
        )

        assertEquals(ImportWatchWorkout.Result.SAVED, import(w, setOf("g1"), setOf("e1")))
        assertEquals(listOf(1, 2), repo.sets.map { it.setNumber })
        assertTrue(repo.sets.all { it.workoutId == "uuid-1" && it.exerciseId == "e1" })
        assertEquals(epochMsToIso(1_761_003_600_000), repo.finishedAt)
        assertEquals("g1", repo.history.single().muscleGroupId)

        // entregue de novo: não duplica
        assertEquals(ImportWatchWorkout.Result.ALREADY_SAVED, import(w, setOf("g1"), setOf("e1")))
        assertEquals(2, repo.sets.size)
    }

    @Test
    fun importDropsDeletedGroupAndSkipsEmpty() = runTest {
        val repo = FakeWorkoutRepository()
        val import = ImportWatchWorkout(repo)
        val w = WatchWorkout("u2", "g-apagado", "X", 0L, 1L, listOf(WatchSet("e1", 1, 5, 10.0, 1L)))
        import(w, emptySet(), setOf("e1"))
        assertNull(repo.history.single().muscleGroupId)

        val empty = w.copy(id = "u3", sets = listOf(WatchSet("sumiu", 1, 5, 10.0, 1L)))
        assertEquals(ImportWatchWorkout.Result.NOTHING_TO_SAVE, import(empty, emptySet(), setOf("e1")))
    }
}
