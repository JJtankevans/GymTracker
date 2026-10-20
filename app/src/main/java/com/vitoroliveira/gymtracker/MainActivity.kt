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
import com.vitoroliveira.gymtracker.ui.music.MusicController
import org.koin.android.ext.android.inject
import org.koin.compose.koinInject

class MainActivity : ComponentActivity() {
    private val music: MusicController by inject()

    override fun onResume() {
        super.onResume()
        music.refresh()   // também cobre a volta da tela de permissão
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val prefs: ThemePreferences = koinInject()
            val themeId by prefs.themeId.collectAsState()
            val reactive by prefs.musicReactive.collectAsState()
            val intensity by prefs.intensity.collectAsState()
            val musicState by music.state.collectAsState()
            val accent = if (reactive) musicState.nowPlaying?.accent else null
            GymTrackerTheme(spec = AppThemes.byId(themeId), accent = accent, intensity = intensity) {
                GymTrackerNavHost()
            }
        }
    }
}
