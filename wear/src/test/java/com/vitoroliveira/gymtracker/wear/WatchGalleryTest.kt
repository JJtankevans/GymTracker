package com.vitoroliveira.gymtracker.wear

import android.content.Context
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.test.core.app.ApplicationProvider
import com.github.takahirom.roborazzi.captureRoboImage
import com.vitoroliveira.gymtracker.watch.WatchExercise
import com.vitoroliveira.gymtracker.watch.WatchRoutine
import com.vitoroliveira.gymtracker.watch.WatchRoutines
import com.vitoroliveira.gymtracker.watch.WatchSession
import kotlinx.coroutines.MainScope
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Capturas das telas do relógio (rodar no PC: ./gradlew.bat :wear:testDebugUnitTest -Proborazzi.test.record=true). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w227dp-h227dp-round-xxhdpi")
class WatchGalleryTest {
    @get:Rule val rule = createComposeRule()

    private val peito = WatchRoutine(
        "g1", "Peito e tríceps", "#FF7A1A",
        listOf(
            WatchExercise("e1", "Supino reto", sets = 4, reps = 10, lastKg = 40.0, lastReps = 10, bestKg = 45.0),
            WatchExercise("e2", "Crucifixo", sets = 3, reps = 12, lastKg = 14.0, lastReps = 12),
            WatchExercise("e3", "Tríceps corda", sets = 3, reps = 12)
        )
    )

    private fun controller(session: WatchSession?, rest: RestState? = null): WatchController {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        val store = WatchStore(ctx).apply {
            routines = WatchRoutines(true, 1L, listOf(peito, peito.copy(groupId = "g2", name = "Costas", colorHex = "#22E3FF")))
            routinesReceivedAt = System.currentTimeMillis()
            this.session = session
            this.rest = rest
            askedPermissions = true
        }
        return WatchController(ctx, store, WatchSync(ctx), HeartRateMonitor(ctx), MainScope())
    }

    @Test
    fun home() {
        val c = controller(null)
        rule.setContent { WatchApp(c) }
        rule.waitForIdle()
        rule.onRoot().captureRoboImage("build/outputs/watch-gallery/1_inicio.png")
    }

    @Test
    fun workout() {
        val s = WatchSession.start("w", peito, System.currentTimeMillis() - 18 * 60_000)
            .addSet(10, 40.0, 1L).session
        val c = controller(s)
        rule.setContent { WatchApp(c) }
        rule.waitForIdle()
        rule.onNodeWithText("Continuar treino").performClick()
        rule.waitForIdle()
        rule.onRoot().captureRoboImage("build/outputs/watch-gallery/2_serie.png")
        // o descanso tem um laço de atualização: relógio do teste manual para não esperar "ficar parado" para sempre
        rule.mainClock.autoAdvance = false
        rule.runOnIdle { c.registerSet(); c.skipRest() }
        rule.mainClock.advanceTimeBy(600)
        rule.onRoot().performTouchInput { swipeLeft() }
        rule.mainClock.advanceTimeBy(1500)
        rule.onRoot().captureRoboImage("build/outputs/watch-gallery/4_numeros.png")
        rule.onRoot().performTouchInput { swipeRight() }
        rule.mainClock.advanceTimeBy(1500)
        rule.onRoot().performTouchInput { swipeRight() }
        rule.mainClock.advanceTimeBy(1500)
        rule.onRoot().captureRoboImage("build/outputs/watch-gallery/5_exercicios.png")
    }

    @Test
    fun rest() {
        val s = WatchSession.start("w", peito, System.currentTimeMillis() - 5 * 60_000).addSet(10, 40.0, 1L).session
        // descanso já em andamento ao abrir: a tela do treino mostra o anel com a contagem
        val c = controller(s, RestState(System.currentTimeMillis() + 75_000, 90))
        rule.mainClock.autoAdvance = false
        rule.setContent { WatchApp(c) }
        rule.mainClock.advanceTimeBy(500)
        rule.onNodeWithText("Continuar treino").performClick()
        rule.mainClock.advanceTimeBy(1500)
        rule.onRoot().captureRoboImage("build/outputs/watch-gallery/3_descanso.png")
    }
}
