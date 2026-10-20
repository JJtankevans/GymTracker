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
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Renderiza a Home em cada tema e salva PNGs em app/build/outputs/theme-gallery/.
 * Serve de conferência visual dos temas sem precisar de emulador.
 * Rodar: ./gradlew :app:testDebugUnitTest --tests "*ThemeGalleryTest*"
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi", application = android.app.Application::class)
class ThemeGalleryTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun homeInEveryTheme() {
        var current by mutableStateOf(AppThemes.all.first())
        composeRule.setContent {
            GymTrackerTheme(spec = current) {
                HomeScreenContent(
                    uiState = HomeUiState(
                        weeklyCount = 3, weeklyGoal = 4, streakDays = 5,
                        lastWorkoutDate = "18/10", lastWorkoutMuscleGroupName = "Peito e Tríceps",
                        totalWorkouts = 31,
                        weeklyHistory = listOf(2,3,4,1,3,4,2,3).mapIndexed { i, c -> com.vitoroliveira.gymtracker.presentation.WeekBucket(kotlinx.datetime.LocalDate(2026, 8, 24).let { kotlinx.datetime.LocalDate.fromEpochDays(it.toEpochDays() + i * 7) }, c) }
                    ),
                    onNavigateToMuscleGroups = {}, onNavigateToHistory = {},
                    onNavigateToProfile = {}, onStartWorkoutClick = {}, onClearError = {}
                )
            }
        }
        AppThemes.all.forEach { spec ->
            current = spec
            composeRule.mainClock.advanceTimeBy(9_000)
            composeRule.waitForIdle()
            composeRule.onRoot().captureRoboImage("build/outputs/theme-gallery/home_${spec.id}.png")
        }
    }
}
