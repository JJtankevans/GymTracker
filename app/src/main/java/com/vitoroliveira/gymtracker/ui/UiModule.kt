package com.vitoroliveira.gymtracker.ui

import com.vitoroliveira.gymtracker.presentation.circuit.CircuitViewModel
import com.vitoroliveira.gymtracker.presentation.circuit.TimingStore
import com.vitoroliveira.gymtracker.ui.circuit.PrefsTimingStore
import org.koin.core.module.dsl.viewModel
import com.vitoroliveira.gymtracker.presentation.body.BodyStore
import com.vitoroliveira.gymtracker.presentation.body.BodyRepository
import com.vitoroliveira.gymtracker.presentation.body.BodyViewModel
import com.vitoroliveira.gymtracker.presentation.body.EvolutionViewModel
import com.vitoroliveira.gymtracker.ui.body.PrefsBodyStore
import com.vitoroliveira.gymtracker.ui.music.MusicController
import com.vitoroliveira.gymtracker.ui.music.SocialPrefs
import com.vitoroliveira.gymtracker.ui.music.WorkoutTrackRecorder
import com.vitoroliveira.gymtracker.ui.theme.ThemePreferences
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/** Dependências que só existem no app Android (precisam de Context). */
val uiModule = module {
    single { ThemePreferences(androidContext()) }
    single { com.vitoroliveira.gymtracker.ui.exercise.CatalogPrefs(androidContext()) }
    single { MusicController(androidContext()) }
    single { SocialPrefs(androidContext()) }
    single { WorkoutTrackRecorder(get(), get(), get()) }
    single<TimingStore> { PrefsTimingStore(androidContext()) }
    viewModel { CircuitViewModel(get()) }
    single<BodyStore> { PrefsBodyStore(androidContext()) }
    single { BodyRepository(get(), get()) }
    viewModel { BodyViewModel(get()) }
    viewModel { EvolutionViewModel(get(), get()) }
    single<com.vitoroliveira.gymtracker.presentation.schedule.ScheduleStore> { com.vitoroliveira.gymtracker.ui.muscleGroup.PrefsScheduleStore(androidContext()) }
    single { com.vitoroliveira.gymtracker.presentation.schedule.ScheduleRepository(get()) }
    viewModel { com.vitoroliveira.gymtracker.presentation.schedule.ScheduleViewModel(get()) }
    single<com.vitoroliveira.gymtracker.domain.repository.LocalUserData> {
        LocalUserDataImpl(androidContext(), get(), get(), get(), get(), get())
    }
}
