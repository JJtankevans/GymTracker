package com.vitoroliveira.gymtracker.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import com.vitoroliveira.gymtracker.presentation.viewmodel.HomeUiState
import com.vitoroliveira.gymtracker.ui.home.HomeScreenContent
import com.vitoroliveira.gymtracker.ui.theme.AppThemes
import com.vitoroliveira.gymtracker.ui.theme.GymTrackerTheme
import com.vitoroliveira.gymtracker.ui.theme.Intensity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi", application = android.app.Application::class)
class IntensityGalleryTest {
    @get:Rule val composeRule = createComposeRule()

    @Test
    fun homeByIntensity() {
        var spec by mutableStateOf(AppThemes.all.first())
        var inten by mutableStateOf(Intensity.NORMAL)
        composeRule.setContent {
            GymTrackerTheme(spec = spec, intensity = inten) {
                HomeScreenContent(
                    uiState = HomeUiState(weeklyCount = 3, weeklyGoal = 4, streakDays = 5, lastWorkoutDate = "18/10",
                        lastWorkoutMuscleGroupName = "Peito e Tríceps", totalWorkouts = 31),
                    onNavigateToMuscleGroups = {}, onNavigateToHistory = {},
                    onNavigateToProfile = {}, onStartWorkoutClick = {}, onClearError = {}
                )
            }
        }
        listOf("synthwave", "neon", "aurora", "magma").mapNotNull { id -> AppThemes.all.firstOrNull { it.id == id } }.forEach { s ->
            Intensity.entries.forEach { i ->
                spec = s; inten = i
                composeRule.mainClock.advanceTimeBy(7_000)
                composeRule.waitForIdle()
                composeRule.onRoot().captureRoboImage("build/outputs/intensity-gallery/${s.id}_${i.name}.png")
            }
        }
    }
}
