package com.vitoroliveira.gymtracker.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import com.vitoroliveira.gymtracker.presentation.viewmodel.HomeUiState
import com.vitoroliveira.gymtracker.ui.home.HomeScreenContent
import com.vitoroliveira.gymtracker.ui.music.MiniPlayerContent
import com.vitoroliveira.gymtracker.ui.music.NowPlaying
import com.vitoroliveira.gymtracker.ui.music.dominantColor
import com.vitoroliveira.gymtracker.ui.theme.AppThemes
import com.vitoroliveira.gymtracker.ui.theme.GymTrackerTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Home com música "tocando": a capa muda a paleta de cada tema reativo. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi", application = android.app.Application::class)
class MusicGalleryTest {
    @get:Rule val composeRule = createComposeRule()

    private fun cover(a: Int, b: Int): Bitmap {
        val bmp = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888)
        Canvas(bmp).drawRect(0f, 0f, 200f, 200f, Paint().apply { shader = LinearGradient(0f, 0f, 200f, 200f, a, b, Shader.TileMode.CLAMP) })
        return bmp
    }

    @Test
    fun homeWithMusic() {
        val covers = listOf(
            "laranja" to cover(0xFFFF6A00.toInt(), 0xFF7A1F00.toInt()),
            "verde" to cover(0xFF1ED760.toInt(), 0xFF0B3D20.toInt()),
            "roxo" to cover(0xFF9B30FF.toInt(), 0xFF2A0A5E.toInt())
        )
        var theme by mutableStateOf(AppThemes.byId("aurora"))
        var cov by mutableStateOf(covers[0])
        composeRule.setContent {
            val bmp = cov.second
            val accent: Color? = dominantColor(bmp)
            GymTrackerTheme(spec = theme, accent = accent) {
                HomeScreenContent(
                    uiState = HomeUiState(weeklyCount = 3, weeklyGoal = 4, streakDays = 5, lastWorkoutDate = "18/10", lastWorkoutMuscleGroupName = "Peito e Tríceps", totalWorkouts = 31),
                    onNavigateToMuscleGroups = {}, onNavigateToHistory = {}, onNavigateToProfile = {},
                    onStartWorkoutClick = {}, onClearError = {},
                    topSlot = { MiniPlayerContent(NowPlaying(title = "Blinding Lights", artist = "The Weeknd", art = bmp, accent = accent, isPlaying = true, app = "Spotify")) }
                )
            }
        }
        listOf("aurora" to 0, "sunset" to 1, "neon" to 2, "magma" to 0, "carbon" to 1, "papel" to 2).forEach { (id, c) ->
            theme = AppThemes.byId(id); cov = covers[c]
            composeRule.mainClock.advanceTimeBy(9_000)
            composeRule.waitForIdle()
            composeRule.onRoot().captureRoboImage("build/outputs/music-gallery/music_$id.png")
        }
    }
}
