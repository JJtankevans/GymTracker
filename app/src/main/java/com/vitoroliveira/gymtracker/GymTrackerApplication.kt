package com.vitoroliveira.gymtracker

import android.app.Application
import com.vitoroliveira.gymtracker.di.appModule
import com.vitoroliveira.gymtracker.di.createGymTrackerSupabaseClient
import com.vitoroliveira.gymtracker.ui.uiModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class GymTrackerApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        val supabaseClient = createGymTrackerSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_KEY
        )

        startKoin {
            androidContext(this@GymTrackerApplication)
            modules(appModule(supabaseClient), uiModule)
        }
        // avisos do Social (pedido para seguir, fim de desafio) a cada ~6 h, sem servidor de push
        com.vitoroliveira.gymtracker.ui.social.SocialAlertsWorker.schedule(this)
    }
}