package com.vitoroliveira.gymtracker.wear

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w227dp-h227dp-round-xxhdpi")
class WatchGalleryTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun pages() {
        val ctx = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        val store = WatchStore(ctx).apply { sets = 4; volume = 3280f; startedAt = System.currentTimeMillis() - 23 * 60_000 }
        rule.setContent { WatchApp(store) }
        rule.waitForIdle()
        rule.onRoot().captureRoboImage("build/outputs/watch-gallery/1_descanso.png")
        rule.onRoot().performTouchInput { swipeLeft() }
        rule.waitForIdle(); rule.mainClock.advanceTimeBy(1000)
        rule.onRoot().captureRoboImage("build/outputs/watch-gallery/2_serie.png")
        rule.onRoot().performTouchInput { swipeLeft() }
        rule.waitForIdle(); rule.mainClock.advanceTimeBy(1000)
        rule.onRoot().captureRoboImage("build/outputs/watch-gallery/3_treino.png")
    }
}
