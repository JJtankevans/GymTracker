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
import androidx.lifecycle.lifecycleScope
import org.koin.compose.koinInject

class MainActivity : ComponentActivity() {
    private val watch: com.vitoroliveira.gymtracker.wear.WatchBridge by org.koin.android.ext.android.inject()

    override fun onResume() {
        super.onResume()
        // o relógio fica em dia (semana, últimas cargas) sem precisar tocar em "Sincronizar"; sem relógio, falha calado
        watch.publishInBackgroundIfStale(lifecycleScope)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val prefs: ThemePreferences = koinInject()
            val themeId by prefs.themeId.collectAsState()
            val intensity by prefs.intensity.collectAsState()
            val bodyData by koinInject<com.vitoroliveira.gymtracker.presentation.body.BodyRepository>().data.collectAsState()
            // a cor da capa da música saiu junto com a leitura de notificações (bloqueio do Play Protect, 28/10/2026)
            GymTrackerTheme(spec = AppThemes.byId(themeId), accent = null, intensity = intensity) {
                androidx.compose.runtime.CompositionLocalProvider(
                    com.vitoroliveira.gymtracker.ui.components.LocalBodyData provides bodyData
                ) { GymTrackerNavHost() }
            }
        }
    }
}
