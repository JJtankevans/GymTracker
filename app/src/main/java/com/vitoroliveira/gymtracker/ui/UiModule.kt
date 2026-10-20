package com.vitoroliveira.gymtracker.ui

import com.vitoroliveira.gymtracker.presentation.circuit.CircuitViewModel
import com.vitoroliveira.gymtracker.presentation.circuit.TimingStore
import com.vitoroliveira.gymtracker.ui.circuit.PrefsTimingStore
import org.koin.core.module.dsl.viewModel
import com.vitoroliveira.gymtracker.ui.music.MusicController
import com.vitoroliveira.gymtracker.ui.music.SocialPrefs
import com.vitoroliveira.gymtracker.ui.music.WorkoutTrackRecorder
import com.vitoroliveira.gymtracker.ui.theme.ThemePreferences
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/** Dependências que só existem no app Android (precisam de Context). */
val uiModule = module {
    single { ThemePreferences(androidContext()) }
    single { MusicController(androidContext()) }
    single { SocialPrefs(androidContext()) }
    single { WorkoutTrackRecorder(get(), get(), get()) }
    single<TimingStore> { PrefsTimingStore(androidContext()) }
    viewModel { CircuitViewModel(get()) }
}
