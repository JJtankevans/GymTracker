package com.vitoroliveira.gymtracker.wear

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope
import androidx.wear.ambient.AmbientLifecycleObserver

class WearActivity : ComponentActivity() {

    private lateinit var controller: WatchController

    // Modo ambiente: com a tela "sempre ligada", o app continua visível (em preto e branco) ao baixar o pulso,
    // em vez de voltar para o mostrador. O alarme do descanso vibra mesmo assim.
    private val ambientCallback = object : AmbientLifecycleObserver.AmbientLifecycleCallback {
        override fun onEnterAmbient(ambientDetails: AmbientLifecycleObserver.AmbientDetails) = controller.onAmbientChanged(true)
        override fun onExitAmbient() = controller.onAmbientChanged(false)
        override fun onUpdateAmbient() {}
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        controller = WatchController(
            app = applicationContext,
            store = WatchStore(applicationContext),
            sync = WatchSync(applicationContext),
            heart = HeartRateMonitor(applicationContext),
            scope = lifecycleScope
        )
        lifecycle.addObserver(AmbientLifecycleObserver(this, ambientCallback))
        setContent { WatchApp(controller) }
    }

    override fun onResume() {
        super.onResume()
        controller.onResume()
    }

    override fun onPause() {
        controller.onPause()
        super.onPause()
    }
}
