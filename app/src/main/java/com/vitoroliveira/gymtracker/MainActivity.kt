package com.vitoroliveira.gymtracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.vitoroliveira.gymtracker.ui.GymTrackerNavHost
import com.vitoroliveira.gymtracker.ui.theme.AppThemes
import com.vitoroliveira.gymtracker.ui.theme.GymTrackerTheme
import com.vitoroliveira.gymtracker.ui.theme.ThemePreferences
import org.koin.compose.koinInject

class MainActivity : ComponentActivity() {
    /** Ação vinda de um atalho do ícone (iniciar treino, histórico, evolução); a navegação consome e limpa. */
    private var launchAction by androidx.compose.runtime.mutableStateOf<String?>(null)

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        launchAction = intent.action
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        launchAction = intent?.action
        enableEdgeToEdge()
        setContent {
            val prefs: ThemePreferences = koinInject()
            val themeId by prefs.themeId.collectAsState()
            val intensity by prefs.intensity.collectAsState()
            val bodyData by koinInject<com.vitoroliveira.gymtracker.presentation.body.BodyRepository>().data.collectAsState()
            GymTrackerTheme(spec = AppThemes.byId(themeId), intensity = intensity) {
                androidx.compose.runtime.CompositionLocalProvider(
                    com.vitoroliveira.gymtracker.ui.components.LocalBodyData provides bodyData
                ) { GymTrackerNavHost(launchAction = launchAction, onActionConsumed = { launchAction = null }) }
            }
        }
    }
}
