package com.vitoroliveira.gymtracker.wear

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

class WearActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Treinando: a tela do relógio não apaga sozinha enquanto o app está aberto.
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val store = WatchStore(this)
        setContent { WatchApp(store) }
    }
}
