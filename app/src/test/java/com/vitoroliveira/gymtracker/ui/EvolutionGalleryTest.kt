package com.vitoroliveira.gymtracker.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import com.vitoroliveira.gymtracker.presentation.body.*
import com.vitoroliveira.gymtracker.ui.body.BodyDataDialog
import com.vitoroliveira.gymtracker.ui.body.EvolutionContent
import com.vitoroliveira.gymtracker.ui.theme.AppThemes
import com.vitoroliveira.gymtracker.ui.theme.GymTrackerTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h1600dp-xxhdpi", application = android.app.Application::class)
class EvolutionGalleryTest {
    @get:Rule val composeRule = createComposeRule()

    private val body = BodyData(
        BodyProfile(heightCm = 178, birthYear = 1996, sex = Sex.MALE),
        listOf(84.2, 83.6, 83.9, 82.8, 82.1, 81.7, 81.0, 80.4).mapIndexed { i, kg -> BodyWeightEntry(20400 + i * 7, kg) }
    )
    private val evo = EvolutionUiState(lifts = listOf(
        LiftSeries("Supino reto", Lift.BENCH,
            listOf("2026-08-04", "2026-08-11", "2026-08-18", "2026-08-25", "2026-09-01", "2026-09-08", "2026-09-15"),
            listOf(60.0, 62.5, 62.5, 65.0, 67.5, 70.0, 72.5),
            listOf(60.0, 62.5, 62.5, 65.0, 67.5, 70.0, 72.5).map { estimateOneRepMax(it, 8) }),
        LiftSeries("Agachamento livre", Lift.SQUAT,
            listOf("2026-08-06", "2026-08-13", "2026-08-20", "2026-08-27", "2026-09-03"),
            listOf(80.0, 85.0, 90.0, 90.0, 95.0),
            listOf(80.0, 85.0, 90.0, 90.0, 95.0).map { estimateOneRepMax(it, 6) })
    ))

    @Test fun evolution() {
        for (id in listOf("neon", "oceano", "brutal")) {
            val spec = AppThemes.all.firstOrNull { it.id == id } ?: continue
            composeRule.setContent {
                GymTrackerTheme(spec = spec) { EvolutionContent(body, evo, {}, { _, _ -> }, {}, {}) }
            }
            composeRule.mainClock.advanceTimeBy(2_000)
            composeRule.waitForIdle()
            composeRule.onRoot().captureRoboImage("build/outputs/evolution-gallery/evolucao_$id.png")
            return
        }
    }
}
