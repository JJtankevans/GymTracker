package com.vitoroliveira.gymtracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.vitoroliveira.gymtracker.ui.GymTrackerNavHost
import com.vitoroliveira.gymtracker.ui.theme.AppThemes
import com.vitoroliveira.gymtracker.ui.theme.GymTrackerTheme
import com.vitoroliveira.gymtracker.ui.theme.ThemePreferences
import org.koin.compose.koinInject

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val prefs: ThemePreferences = koinInject()
            val themeId by prefs.themeId.collectAsState()
            val intensity by prefs.intensity.collectAsState()
            val bodyData by koinInject<com.vitoroliveira.gymtracker.presentation.body.BodyRepository>().data.collectAsState()
            GymTrackerTheme(spec = AppThemes.byId(themeId), intensity = intensity) {
                androidx.compose.runtime.CompositionLocalProvider(
                    com.vitoroliveira.gymtracker.ui.components.LocalBodyData provides bodyData
                ) { GymTrackerNavHost() }
            }
        }
    }
}
