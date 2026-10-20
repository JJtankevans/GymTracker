package com.vitoroliveira.gymtracker.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import com.vitoroliveira.gymtracker.data.model.Exercise
import com.vitoroliveira.gymtracker.presentation.circuit.*
import com.vitoroliveira.gymtracker.ui.circuit.CircuitRunContent
import com.vitoroliveira.gymtracker.ui.circuit.CircuitSetup
import com.vitoroliveira.gymtracker.ui.theme.AppThemes
import com.vitoroliveira.gymtracker.ui.theme.GymTrackerTheme
import com.vitoroliveira.gymtracker.ui.theme.ThemedScaffold
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi", application = android.app.Application::class)
class CircuitGalleryTest {
    @get:Rule val rule = createComposeRule()

    private val steps = CircuitPlanner.build(
        listOf(
            CircuitPlanner.Input("a", "Supino Reto", 3, 10, 80.0),
            CircuitPlanner.Input("b", "Crucifixo", 3, 12, 20.0)
        ), restSeconds = 90
    )
    private val base = CircuitState(phase = CircuitPhase.SET, steps = steps, stepIndex = 1, predictedSetSec = 36, setElapsedMs = 23_400,
        completed = List(1) { CommittedSet(steps[0], 34, false) })

    @Test
    fun screens() {
        var theme by mutableStateOf(AppThemes.byId("carbon"))
        var mode by mutableStateOf(0)
        val learner = SetTimingLearner().apply { repeat(5) { record("a", 40, 10) } }
        val ex = listOf(
            Exercise(id = "a", name = "Supino Reto", defaultSets = 3, defaultReps = 10),
            Exercise(id = "b", name = "Crucifixo", defaultSets = 3, defaultReps = 12)
        )
        rule.setContent {
            GymTrackerTheme(spec = theme) {
                ThemedScaffold { p ->
                    androidx.compose.foundation.layout.Box(androidx.compose.ui.Modifier.padding(p)) {
                        when (mode) {
                            0 -> CircuitSetup("Peito", ex, mapOf("a" to 80.0, "b" to 20.0), learner) {}
                            1 -> CircuitRunContent(base, {}, {}, {}, { _, _ -> })
                            else -> CircuitRunContent(base.copy(phase = CircuitPhase.REST, restTotalSec = 90, restLeftMs = 61_000, pendingDurationSec = 36, autoEnded = true), {}, {}, {}, { _, _ -> })
                        }
                    }
                }
            }
        }
        listOf("carbon", "neon", "papel").forEach { id ->
            theme = AppThemes.byId(id)
            for (m in 0..2) {
                mode = m
                rule.mainClock.advanceTimeBy(3_000); rule.waitForIdle()
                rule.onRoot().captureRoboImage("build/outputs/circuit-gallery/${m}_$id.png")
            }
        }
    }
}
